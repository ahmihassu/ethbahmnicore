/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.deposit.service.impl;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ethbahmnicore.deposit.contract.DepositEligibility;
import org.openmrs.module.ethbahmnicore.deposit.contract.DepositEligibilityStatus;
import org.openmrs.module.ethbahmnicore.deposit.odoo.OdooDepositPartner;
import org.openmrs.module.ethbahmnicore.deposit.odoo.OdooDepositRepository;
import org.openmrs.module.ethbahmnicore.deposit.properties.DepositProperties;
import org.openmrs.module.ethbahmnicore.deposit.service.DepositEligibilityService;
import org.openmrs.module.ethbahmnicore.registrationfee.odoo.OdooException;

/**
 * Cash-only IPD deposit checks against Odoo balance. Free/Credit skip deposit requirement.
 */
public class DepositEligibilityServiceImpl extends BaseOpenmrsService implements DepositEligibilityService {
	
	private static final Log log = LogFactory.getLog(DepositEligibilityServiceImpl.class);
	
	private DepositProperties properties;
	
	private OdooDepositRepository depositRepository;
	
	private PatientService patientService;
	
	public void setProperties(DepositProperties properties) {
		this.properties = properties;
	}
	
	public void setDepositRepository(OdooDepositRepository depositRepository) {
		this.depositRepository = depositRepository;
	}
	
	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}
	
	@Override
	public boolean isEnforceIpdDeposit() {
		return properties.isEnforceIpdDeposit();
	}
	
	@Override
	public DepositEligibility evaluateForAdmit(String patientUuid, String identifier) throws APIException {
		return evaluate(patientUuid, identifier, null, null, EvalPurpose.ADMIT);
	}
	
	@Override
	public DepositEligibility evaluateForOrder(String patientUuid, String identifier, String conceptUuid, Double quantity)
	        throws APIException {
		return evaluate(patientUuid, identifier, conceptUuid, quantity, EvalPurpose.ORDER);
	}
	
	@Override
	public DepositEligibility evaluateForStatus(String patientUuid, String identifier) throws APIException {
		return evaluate(patientUuid, identifier, null, null, EvalPurpose.STATUS);
	}
	
	@Override
	public void assertAdmitAllowed(String patientUuid) throws APIException {
		if (!isEnforceIpdDeposit()) {
			return;
		}
		DepositEligibility eligibility = evaluateForAdmit(patientUuid, null);
		if (!eligibility.isAllowed()) {
			throw new APIException("IPD deposit required: " + eligibility.getReason());
		}
	}
	
	@Override
	public void assertOrderAllowed(Order order) throws APIException {
		if (!isEnforceIpdDeposit() || order == null) {
			return;
		}
		if (isDepositExemptOrder(order)) {
			return;
		}
		Patient patient = order.getPatient();
		if (patient == null && order.getEncounter() != null) {
			patient = order.getEncounter().getPatient();
		}
		if (patient == null) {
			return;
		}
		if (!isCurrentlyIpd(patient)) {
			return;
		}
		String conceptUuid = order.getConcept() == null ? null : order.getConcept().getUuid();
		Double qty = 1.0;
		if (order instanceof org.openmrs.DrugOrder) {
			org.openmrs.DrugOrder drugOrder = (org.openmrs.DrugOrder) order;
			if (drugOrder.getQuantity() != null) {
				qty = drugOrder.getQuantity();
			}
		}
		DepositEligibility eligibility = evaluateForOrder(patient.getUuid(), null, conceptUuid, qty);
		if (!eligibility.isDepositRequired()) {
			return;
		}
		double siblingReserved = sumSiblingOrderPrices(order);
		Double required = eligibility.getRequiredAmount() == null ? 0.0 : eligibility.getRequiredAmount();
		Double available = eligibility.getAvailableBalance();
		if (available == null) {
			available = eligibility.getBalance() == null ? 0.0 : eligibility.getBalance();
		}
		double coverNeeded = required + siblingReserved;
		if (available + 0.00001 < coverNeeded) {
			double shortfall = coverNeeded - available;
			throw new APIException(
			        String.format(
			            Locale.ENGLISH,
			            "Insufficient IPD deposit: available %.2f, required %.2f (this order %.2f + other orders in encounter %.2f), shortfall %.2f",
			            available, coverNeeded, required, siblingReserved, shortfall));
		}
		if (!eligibility.isAllowed()) {
			throw new APIException("Insufficient IPD deposit: " + eligibility.getReason());
		}
	}
	
	/**
	 * Prices of other already-persisted orders on the same encounter that are not yet in Odoo.
	 * Already-synced (paid/draft SO) siblings must not be reserved again — deposit/committed
	 * already reflects them.
	 */
	private double sumSiblingOrderPrices(Order order) {
		if (order.getEncounter() == null || order.getEncounter().getOrders() == null) {
			return 0.0;
		}
		double total = 0.0;
		for (Order sibling : order.getEncounter().getOrders()) {
			if (sibling == null || sibling == order) {
				continue;
			}
			if (sibling.getId() == null) {
				continue;
			}
			if (isDepositExemptOrder(sibling)) {
				continue;
			}
			if (sibling.getUuid() != null && depositRepository.isOrderSyncedToOdoo(sibling.getUuid())) {
				continue;
			}
			if (sibling.getConcept() == null) {
				continue;
			}
			Double unitPrice = depositRepository.findProductListPrice(sibling.getConcept().getUuid());
			if (unitPrice == null) {
				continue;
			}
			double qty = 1.0;
			if (sibling instanceof org.openmrs.DrugOrder) {
				org.openmrs.DrugOrder drugOrder = (org.openmrs.DrugOrder) sibling;
				if (drugOrder.getQuantity() != null) {
					qty = drugOrder.getQuantity();
				}
			}
			total += unitPrice * qty;
		}
		return total;
	}
	
	private boolean isCurrentlyIpd(Patient patient) {
		try {
			List<List<Object>> rows = org.openmrs.api.context.Context.getAdministrationService().executeSQL(
			    "SELECT 1 FROM bed_patient_assignment_map WHERE patient_id = " + patient.getPatientId()
			            + " AND date_stopped IS NULL LIMIT 1", true);
			if (rows != null && !rows.isEmpty()) {
				return true;
			}
		}
		catch (Exception e) {
			log.debug("bed assignment check failed", e);
		}
		try {
			List<org.openmrs.Visit> visits = org.openmrs.api.context.Context.getVisitService().getActiveVisitsByPatient(
			    patient);
			if (visits != null) {
				for (org.openmrs.Visit visit : visits) {
					if (visit.getVisitType() != null && visit.getVisitType().getName() != null) {
						String name = visit.getVisitType().getName().toLowerCase(Locale.ENGLISH);
						if (name.contains("ipd") || name.contains("inpatient")) {
							return true;
						}
					}
				}
			}
		}
		catch (Exception e) {
			log.debug("active visit check failed", e);
		}
		return false;
	}
	
	@Override
	public boolean isDepositExemptOrder(Order order) {
		if (order == null || order.getOrderType() == null) {
			return false;
		}
		OrderType type = order.getOrderType();
		String name = type.getName() == null ? "" : type.getName().toLowerCase(Locale.ENGLISH);
		return properties.getAllowedOrderTypeNamesLower().contains(name);
	}
	
	@Override
	public boolean isAdmissionEncounterType(String encounterTypeName) {
		if (StringUtils.isBlank(encounterTypeName)) {
			return false;
		}
		return properties.getAdmissionEncounterTypeNamesLower().contains(
		    encounterTypeName.trim().toLowerCase(Locale.ENGLISH));
	}
	
	private enum EvalPurpose {
		ADMIT, ORDER, STATUS
	}
	
	private DepositEligibility evaluate(String patientUuid, String identifier, String conceptUuid, Double quantity,
	        EvalPurpose purpose) {
		DepositEligibility result = new DepositEligibility();
		boolean enforce = properties.isEnforceIpdDeposit();
		result.setEnforceIpdDeposit(enforce);
		
		double gpMin = properties.getMinDepositAmount();
		result.setMinDeposit(gpMin);
		
		Patient patient = resolvePatient(patientUuid, identifier);
		if (patient == null) {
			result.setAllowed(!enforce);
			result.setStatus(DepositEligibilityStatus.PATIENT_NOT_FOUND);
			result.setReason("Patient not found");
			result.setPatientUuid(patientUuid);
			result.setIdentifier(identifier);
			return result;
		}
		result.setPatientUuid(patient.getUuid());
		result.setIdentifier(preferredIdentifier(patient));
		boolean currentlyIpd = isCurrentlyIpd(patient);
		result.setCurrentlyIpd(currentlyIpd);
		
		if (!properties.isOdooConfigured()) {
			result.setDepositRequired(true);
			result.setStatus(DepositEligibilityStatus.ODOO_NOT_CONFIGURED);
			result.setReason("Odoo credentials missing");
			// AOP only hard-denies when enforce is on; UI still sees the failure fields.
			result.setAllowed(!enforce);
			return result;
		}
		
		try {
			OdooDepositPartner partner = depositRepository.findPartnerDeposit(patient.getUuid(),
			    preferredIdentifier(patient));
			if (partner == null) {
				result.setDepositRequired(true);
				result.setStatus(DepositEligibilityStatus.PARTNER_NOT_FOUND);
				result.setReason("Patient partner not found in Odoo");
				result.setAllowed(!enforce);
				return result;
			}
			
			String method = normalizeMethod(partner.getPaymentMethod());
			result.setPaymentMethod(partner.getPaymentMethod());
			double balance = partner.getIpdDepositBalance() == null ? 0.0 : partner.getIpdDepositBalance();
			result.setBalance(balance);
			double committed = partner.getCommittedAmount() == null ? 0.0 : partner.getCommittedAmount();
			double available = partner.getAvailableBalance() == null ? Math.max(balance - committed, 0.0) : partner
			        .getAvailableBalance();
			result.setCommittedAmount(committed);
			result.setAvailableBalance(available);
			
			double odooMin = partner.getCompanyMinDeposit() == null ? 0.0 : partner.getCompanyMinDeposit();
			double minDeposit = odooMin > 0 ? odooMin : gpMin;
			result.setMinDeposit(minDeposit);
			
			if (!"cash".equals(method)) {
				result.setAllowed(true);
				result.setDepositRequired(false);
				result.setStatus(DepositEligibilityStatus.NOT_REQUIRED);
				result.setReason("Deposit required only for Cash patients");
				return result;
			}
			
			// Orders / header status: IPD only. Admit checks stay for ADT even before bed assign.
			if (purpose != EvalPurpose.ADMIT && !currentlyIpd) {
				result.setAllowed(true);
				result.setDepositRequired(false);
				result.setStatus(DepositEligibilityStatus.NOT_REQUIRED);
				result.setReason("Deposit required only for IPD patients");
				result.setRequiredAmount(0.0);
				result.setShortfall(0.0);
				return result;
			}
			
			if (purpose == EvalPurpose.STATUS) {
				// Soft header: show balance for Cash IPD; do not block.
				result.setDepositRequired(true);
				result.setRequiredAmount(0.0);
				result.setShortfall(0.0);
				result.setAllowed(true);
				result.setStatus(DepositEligibilityStatus.CASH_DEPOSIT_OK);
				result.setReason("IPD Cash patient — deposit balance shown");
				return result;
			}
			
			result.setDepositRequired(true);
			double required;
			if (purpose == EvalPurpose.ADMIT) {
				required = minDeposit;
			} else {
				Double unitPrice = depositRepository.findProductListPrice(conceptUuid);
				double qty = quantity == null || quantity <= 0 ? 1.0 : quantity;
				if (unitPrice == null) {
					// Unknown price: require more than currently available so the order is blocked.
					required = available + 0.01;
					result.setReason("Could not resolve Odoo price for concept " + conceptUuid);
				} else {
					required = unitPrice * qty;
				}
			}
			result.setRequiredAmount(required);
			// Admit checks ledger balance; further orders check uncommitted available balance.
			double coverFrom = purpose == EvalPurpose.ADMIT ? balance : available;
			double shortfall = Math.max(required - coverFrom, 0.0);
			result.setShortfall(shortfall);
			
			boolean sufficient = coverFrom + 0.00001 >= required;
			if (sufficient) {
				result.setStatus(DepositEligibilityStatus.CASH_DEPOSIT_OK);
				result.setReason("Deposit covers required amount");
			} else {
				result.setStatus(DepositEligibilityStatus.CASH_DEPOSIT_INSUFFICIENT);
				if (StringUtils.isBlank(result.getReason())) {
					result.setReason(String.format(Locale.ENGLISH,
					    "Available deposit %.2f (balance %.2f, committed %.2f) is less than required %.2f (shortfall %.2f)",
					    coverFrom, balance, committed, required, shortfall));
				}
			}
			// Soft-gate UI uses depositRequired/shortfall; AOP hard-deny only when enforce is on.
			result.setAllowed(enforce ? sufficient : true);
			if (!enforce) {
				result.setStatus(DepositEligibilityStatus.ENFORCEMENT_DISABLED);
				result.setReason((sufficient ? "Deposit OK but " : result.getReason() + "; ")
				        + "server AOP enforceIpdDeposit is false");
			}
			return result;
		}
		catch (OdooException e) {
			log.error("Odoo deposit lookup failed", e);
			result.setDepositRequired(true);
			result.setStatus(DepositEligibilityStatus.ODOO_ERROR);
			result.setReason("Odoo error: " + e.getMessage());
			result.setAllowed(!enforce);
			return result;
		}
	}
	
	private Patient resolvePatient(String patientUuid, String identifier) {
		if (StringUtils.isNotBlank(patientUuid)) {
			Patient byUuid = patientService.getPatientByUuid(patientUuid.trim());
			if (byUuid != null) {
				return byUuid;
			}
		}
		if (StringUtils.isNotBlank(identifier)) {
			List<Patient> matches = patientService.getPatients(null, identifier.trim(), null, true);
			if (matches != null && !matches.isEmpty()) {
				return matches.get(0);
			}
		}
		return null;
	}
	
	private static String preferredIdentifier(Patient patient) {
		PatientIdentifier preferred = patient.getPatientIdentifier();
		return preferred == null ? null : preferred.getIdentifier();
	}
	
	private static String normalizeMethod(String method) {
		return method == null ? "" : method.trim().toLowerCase(Locale.ENGLISH);
	}
}
