/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.registrationfee.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ethbahmnicore.registrationfee.contract.EligibilityStatus;
import org.openmrs.module.ethbahmnicore.registrationfee.contract.RegistrationFeeEligibility;
import org.openmrs.module.ethbahmnicore.registrationfee.odoo.OdooException;
import org.openmrs.module.ethbahmnicore.registrationfee.odoo.OdooInvoice;
import org.openmrs.module.ethbahmnicore.registrationfee.odoo.OdooInvoiceRepository;
import org.openmrs.module.ethbahmnicore.registrationfee.properties.RegistrationFeeProperties;
import org.openmrs.module.ethbahmnicore.registrationfee.service.RegistrationFeeEligibilityService;
import org.openmrs.module.ethbahmnicore.registrationfee.util.RegistrationFeeAllowList;

/**
 * Applies Cash / Free / Credit invoice rules against Odoo for the configured payment window.
 */
public class RegistrationFeeEligibilityServiceImpl extends BaseOpenmrsService implements RegistrationFeeEligibilityService {
	
	private static final Log log = LogFactory.getLog(RegistrationFeeEligibilityServiceImpl.class);
	
	private RegistrationFeeProperties properties;
	
	private OdooInvoiceRepository invoiceRepository;
	
	private PatientService patientService;
	
	private RegistrationFeeAllowList allowList;
	
	public void setProperties(RegistrationFeeProperties properties) {
		this.properties = properties;
	}
	
	public void setInvoiceRepository(OdooInvoiceRepository invoiceRepository) {
		this.invoiceRepository = invoiceRepository;
	}
	
	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}
	
	public void setAllowList(RegistrationFeeAllowList allowList) {
		this.allowList = allowList;
	}
	
	@Override
	public RegistrationFeeEligibility evaluate(String patientUuid, String identifier) throws APIException {
		int windowDays = properties.getPaymentWindowDays();
		Date windowEnd = startOfDay(new Date());
		Date windowStart = addDays(windowEnd, -(windowDays - 1));
		
		boolean enforce = properties.isEnforceRegistrationFee();
		RegistrationFeeEligibility base = new RegistrationFeeEligibility();
		base.setEnforceRegistrationFee(enforce);
		base.setWindowDays(windowDays);
		base.setWindowStart(windowStart);
		base.setWindowEnd(windowEnd);
		
		Patient patient = resolvePatient(patientUuid, identifier);
		if (patient == null) {
			base.setAllowed(!enforce);
			base.setStatus(EligibilityStatus.PATIENT_NOT_FOUND);
			base.setReason("Patient not found for uuid/identifier");
			base.setPatientUuid(patientUuid);
			base.setIdentifier(identifier);
			return base;
		}
		
		String resolvedUuid = patient.getUuid();
		String resolvedId = preferredIdentifier(patient);
		base.setPatientUuid(resolvedUuid);
		base.setIdentifier(resolvedId);
		
		if (!enforce) {
			RegistrationFeeEligibility disabled = base;
			if (properties.isOdooConfigured()) {
				try {
					disabled = evaluateAgainstOdoo(base, resolvedUuid, resolvedId, windowStart, windowEnd);
				}
				catch (OdooException e) {
					log.debug("Odoo lookup skipped while enforcement disabled", e);
				}
			}
			disabled.setAllowed(true);
			disabled.setStatus(EligibilityStatus.ENFORCEMENT_DISABLED);
			disabled.setReason("enforceRegistrationFee is false; services are not denied");
			disabled.setEnforceRegistrationFee(false);
			return disabled;
		}
		
		if (!properties.isOdooConfigured()) {
			base.setAllowed(false);
			base.setStatus(EligibilityStatus.ODOO_NOT_CONFIGURED);
			base.setReason("Odoo credentials missing (ETHBAHMNICORE_ODOO_USER / ETHBAHMNICORE_ODOO_PASSWORD)");
			return base;
		}
		
		try {
			return evaluateAgainstOdoo(base, resolvedUuid, resolvedId, windowStart, windowEnd);
		}
		catch (OdooException e) {
			log.error("Odoo eligibility lookup failed", e);
			base.setAllowed(false);
			base.setStatus(EligibilityStatus.ODOO_UNAVAILABLE);
			base.setReason("Unable to reach Odoo for registration-fee invoice check: " + e.getMessage());
			return base;
		}
	}
	
	@Override
	public void assertAllowed(String patientUuid, String identifier) throws APIException {
		RegistrationFeeEligibility eligibility = evaluate(patientUuid, identifier);
		if (!eligibility.isAllowed()) {
			throw new APIException("Registration fee not satisfied [" + eligibility.getStatus() + "]: "
			        + eligibility.getReason());
		}
	}
	
	@Override
	public boolean isEnforceRegistrationFee() {
		return properties.isEnforceRegistrationFee();
	}
	
	@Override
	public boolean isRegistrationFeeOrder(org.openmrs.Order order) {
		return allowList != null && allowList.isRegistrationFeeOrder(order);
	}
	
	@Override
	public boolean isRegistrationFeeEncounter(org.openmrs.Encounter encounter) {
		return allowList != null && allowList.isRegistrationFeeEncounter(encounter);
	}
	
	private RegistrationFeeEligibility evaluateAgainstOdoo(RegistrationFeeEligibility base, String patientUuid,
	        String identifier, Date windowStart, Date windowEnd) {
		OdooInvoice invoice = invoiceRepository.findLatestMruInvoice(patientUuid, identifier, windowStart, windowEnd);
		if (invoice == null) {
			OdooInvoice anyTime = invoiceRepository.findLatestMruInvoiceAnyTime(patientUuid, identifier);
			if (anyTime != null) {
				base.setInvoiceId(anyTime.getId());
				base.setInvoiceState(anyTime.getState());
				base.setPaymentMethod(anyTime.getPaymentMethod());
				base.setAllowed(false);
				base.setStatus(EligibilityStatus.OUTSIDE_WINDOW);
				base.setReason("MRU invoice exists but is outside the current payment window");
				return base;
			}
			base.setAllowed(false);
			base.setStatus(EligibilityStatus.NO_INVOICE);
			base.setReason("No MRU / Registration Fee invoice found in the current payment window");
			return base;
		}
		
		base.setInvoiceId(invoice.getId());
		base.setInvoiceState(invoice.getState());
		base.setPaymentMethod(invoice.getPaymentMethod());
		
		String method = normalizeMethod(invoice.getPaymentMethod());
		String state = invoice.getState() == null ? "" : invoice.getState().trim().toLowerCase(Locale.ENGLISH);
		
		if ("cash".equals(method)) {
			if ("paid".equals(state)) {
				return ok(base, EligibilityStatus.CASH_PAID_OK, "Cash invoice is paid");
			}
			return deny(base, EligibilityStatus.CASH_UNPAID, "Cash invoice exists but is not paid (state=" + state + ")");
		}
		if ("free".equals(method)) {
			if ("paid".equals(state)) {
				return ok(base, EligibilityStatus.FREE_PAID_OK, "Free invoice is paid (100% discount)");
			}
			return deny(base, EligibilityStatus.FREE_UNPAID, "Free invoice exists but is not paid (state=" + state + ")");
		}
		if ("credit".equals(method)) {
			if ("open".equals(state)) {
				return ok(base, EligibilityStatus.CREDIT_OPEN_OK, "Credit invoice is open (payment deferred)");
			}
			return deny(base, EligibilityStatus.CREDIT_MISSING_OR_CLOSED, "Credit invoice must be open/unpaid (state="
			        + state + ")");
		}
		
		return deny(base, EligibilityStatus.UNKNOWN_PAYMENT_METHOD,
		    "Unrecognized payment_method on invoice: " + invoice.getPaymentMethod());
	}
	
	private static RegistrationFeeEligibility ok(RegistrationFeeEligibility base, EligibilityStatus status, String reason) {
		base.setAllowed(true);
		base.setStatus(status);
		base.setReason(reason);
		return base;
	}
	
	private static RegistrationFeeEligibility deny(RegistrationFeeEligibility base, EligibilityStatus status, String reason) {
		base.setAllowed(false);
		base.setStatus(status);
		base.setReason(reason);
		return base;
	}
	
	private static String normalizeMethod(String paymentMethod) {
		if (paymentMethod == null) {
			return "";
		}
		return paymentMethod.trim().toLowerCase(Locale.ENGLISH);
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
		if (preferred != null && StringUtils.isNotBlank(preferred.getIdentifier())) {
			return preferred.getIdentifier();
		}
		if (patient.getActiveIdentifiers() != null) {
			for (PatientIdentifier id : patient.getActiveIdentifiers()) {
				if (id != null && StringUtils.isNotBlank(id.getIdentifier())) {
					return id.getIdentifier();
				}
			}
		}
		return null;
	}
	
	private static Date startOfDay(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
	
	private static Date addDays(Date date, int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DAY_OF_MONTH, days);
		return cal.getTime();
	}
}
