/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.deposit.task;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ethbahmnicore.deposit.properties.DepositProperties;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * Creates one Bed Service order per occupied IPD patient per calendar day. Relies on SQL global
 * property bahmni.sqlGet.getBedServiceConcept (ward → concept) via AdministrationService.executeSQL
 * when available; falls back to concept name match.
 */
public class DailyBedFeeTask extends AbstractTask {
	
	private static final Log log = LogFactory.getLog(DailyBedFeeTask.class);
	
	@Override
	public void execute() {
		try {
			DepositProperties properties = Context.getRegisteredComponents(DepositProperties.class).isEmpty() ? null
			        : Context.getRegisteredComponents(DepositProperties.class).get(0);
			String bedOrderTypeName = properties == null ? "Bed Service" : properties.getBedOrderTypeName();
			
			OrderService orderService = Context.getOrderService();
			VisitService visitService = Context.getVisitService();
			EncounterService encounterService = Context.getEncounterService();
			ConceptService conceptService = Context.getConceptService();
			ProviderService providerService = Context.getProviderService();
			
			OrderType bedOrderType = orderService.getOrderTypeByName(bedOrderTypeName);
			if (bedOrderType == null) {
				log.error("DailyBedFeeTask: order type '" + bedOrderTypeName + "' not found");
				return;
			}
			
			CareSetting inpatient = orderService.getCareSettingByName("Inpatient");
			if (inpatient == null) {
				inpatient = orderService.getCareSettingByName("INPATIENT");
			}
			
			EncounterType consultationType = encounterService.getEncounterType("Consultation");
			if (consultationType == null) {
				consultationType = encounterService.getEncounterType("ADMISSION");
			}
			
			Date startOfDay = startOfDay(new Date());
			List<Visit> activeVisits = visitService.getVisits(null, null, null, null, null, null, null, null, null, false,
			    false);
			int created = 0;
			for (Visit visit : activeVisits) {
				if (visit.getStopDatetime() != null) {
					continue;
				}
				if (!isIpdVisit(visit)) {
					continue;
				}
				Patient patient = visit.getPatient();
				if (patient == null) {
					continue;
				}
				if (alreadyChargedToday(orderService, patient, bedOrderType, startOfDay)) {
					continue;
				}
				
				String wardName = resolveWardName(patient);
				Concept bedConcept = resolveBedConcept(conceptService, wardName);
				if (bedConcept == null) {
					log.warn("DailyBedFeeTask: no bed concept for patient " + patient.getUuid() + " ward=" + wardName);
					continue;
				}
				
				Encounter encounter = new Encounter();
				encounter.setPatient(patient);
				encounter.setVisit(visit);
				encounter.setEncounterDatetime(new Date());
				if (consultationType != null) {
					encounter.setEncounterType(consultationType);
				}
				if (visit.getLocation() != null) {
					encounter.setLocation(visit.getLocation());
				}
				
				Provider provider = firstProvider(providerService);
				Order order = new org.openmrs.TestOrder();
				order.setPatient(patient);
				order.setConcept(bedConcept);
				order.setOrderType(bedOrderType);
				order.setCareSetting(inpatient != null ? inpatient : orderService.getCareSetting(1));
				order.setOrderer(provider);
				order.setEncounter(encounter);
				order.setDateActivated(new Date());
				order.setCommentToFulfiller("Daily bed fee " + formatDay(startOfDay)
				        + (wardName == null ? "" : " ward=" + wardName));
				
				encounter.addOrder(order);
				encounterService.saveEncounter(encounter);
				created++;
			}
			log.info("DailyBedFeeTask created " + created + " bed fee orders");
		}
		catch (Exception e) {
			log.error("DailyBedFeeTask failed", e);
		}
	}
	
	private static boolean isIpdVisit(Visit visit) {
		if (visit.getVisitType() == null || visit.getVisitType().getName() == null) {
			return false;
		}
		String name = visit.getVisitType().getName().toLowerCase(Locale.ENGLISH);
		return name.contains("ipd") || name.contains("inpatient");
	}
	
	private static boolean alreadyChargedToday(OrderService orderService, Patient patient, OrderType bedOrderType,
	        Date startOfDay) {
		List<Order> orders = orderService.getAllOrdersByPatient(patient);
		for (Order order : orders) {
			if (order.getOrderType() == null || !order.getOrderType().equals(bedOrderType)) {
				continue;
			}
			if (order.getDateActivated() != null && !order.getDateActivated().before(startOfDay) && !order.isVoided()) {
				return true;
			}
		}
		return false;
	}
	
	private static String resolveWardName(Patient patient) {
		try {
			List<List<Object>> rows = Context.getAdministrationService().executeSQL(
			    "SELECT pl.name FROM bed_patient_assignment_map bpam " + "JOIN bed b ON b.bed_id = bpam.bed_id "
			            + "JOIN location bl ON bl.location_id = b.location_id "
			            + "JOIN location pl ON pl.location_id = bl.parent_location " + "WHERE bpam.patient_id = "
			            + patient.getPatientId() + " AND bpam.date_stopped IS NULL LIMIT 1", true);
			if (rows != null && !rows.isEmpty() && rows.get(0) != null && !rows.get(0).isEmpty()) {
				Object val = rows.get(0).get(0);
				return val == null ? null : String.valueOf(val);
			}
		}
		catch (Exception e) {
			log.debug("Could not resolve ward via SQL", e);
		}
		return null;
	}
	
	private static Concept resolveBedConcept(ConceptService conceptService, String wardName) {
		if (wardName != null) {
			try {
				List<List<Object>> rows = Context.getAdministrationService().executeSQL(
				    "SELECT c.uuid FROM concept_name cn " + "JOIN concept c ON c.concept_id = cn.concept_id "
				            + "WHERE cn.name LIKE '%" + wardName.replace("'", "") + "%Bed%' "
				            + "AND cn.locale = 'en' AND cn.voided = 0 LIMIT 1", true);
				if (rows != null && !rows.isEmpty() && rows.get(0) != null && !rows.get(0).isEmpty()) {
					Concept byUuid = conceptService.getConceptByUuid(String.valueOf(rows.get(0).get(0)));
					if (byUuid != null) {
						return byUuid;
					}
				}
			}
			catch (Exception e) {
				log.debug("Ward bed concept SQL failed", e);
			}
			Concept byName = conceptService.getConceptByName(wardName + " Bed Charge");
			if (byName != null) {
				return byName;
			}
		}
		Concept generic = conceptService.getConceptByName("Bed Charge");
		if (generic == null) {
			generic = conceptService.getConceptByName("Bed Service");
		}
		return generic;
	}
	
	private static Provider firstProvider(ProviderService providerService) {
		List<Provider> providers = providerService.getAllProviders(false);
		return providers == null || providers.isEmpty() ? null : providers.get(0);
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
	
	private static String formatDay(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return String.format(Locale.ENGLISH, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
		    cal.get(Calendar.DAY_OF_MONTH));
	}
}
