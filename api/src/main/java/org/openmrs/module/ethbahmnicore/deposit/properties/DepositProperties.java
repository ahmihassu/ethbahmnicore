/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.deposit.properties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ethbahmnicore.registrationfee.properties.RegistrationFeeProperties;

/**
 * IPD deposit enforcement settings. Reuses Odoo connection GPs / env from registration fee.
 */
public class DepositProperties {
	
	public static final String GP_ENFORCE = "ethbahmnicore.enforceIpdDeposit";
	
	public static final String GP_MIN_DEPOSIT = "ethbahmnicore.ipd.minDepositAmount";
	
	public static final String GP_BED_ORDER_TYPE = "ethbahmnicore.ipd.bedOrderTypeName";
	
	public static final String GP_ALLOWED_ORDER_TYPES = "ethbahmnicore.ipd.depositAllowedOrderTypes";
	
	public static final String GP_ADMISSION_ENCOUNTER_TYPES = "ethbahmnicore.ipd.admissionEncounterTypes";
	
	private AdministrationService administrationService;
	
	private RegistrationFeeProperties registrationFeeProperties;
	
	public void setAdministrationService(AdministrationService administrationService) {
		this.administrationService = administrationService;
	}
	
	public void setRegistrationFeeProperties(RegistrationFeeProperties registrationFeeProperties) {
		this.registrationFeeProperties = registrationFeeProperties;
	}
	
	public boolean isEnforceIpdDeposit() {
		return parseBoolean(getGp(GP_ENFORCE), false);
	}
	
	public double getMinDepositAmount() {
		String raw = getGp(GP_MIN_DEPOSIT);
		if (StringUtils.isBlank(raw)) {
			return 0.0;
		}
		try {
			return Double.parseDouble(raw.trim());
		}
		catch (NumberFormatException e) {
			return 0.0;
		}
	}
	
	public String getBedOrderTypeName() {
		return defaultIfBlank(getGp(GP_BED_ORDER_TYPE), "Bed Service");
	}
	
	public Set<String> getAllowedOrderTypeNamesLower() {
		String raw = getGp(GP_ALLOWED_ORDER_TYPES);
		Set<String> names = new HashSet<String>();
		if (StringUtils.isBlank(raw)) {
			names.add("bed service");
			names.add("registration fee");
			return names;
		}
		for (String part : raw.split(",")) {
			if (StringUtils.isNotBlank(part)) {
				names.add(part.trim().toLowerCase(Locale.ENGLISH));
			}
		}
		return names;
	}
	
	public Set<String> getAdmissionEncounterTypeNamesLower() {
		String raw = getGp(GP_ADMISSION_ENCOUNTER_TYPES);
		Set<String> names = new HashSet<String>();
		if (StringUtils.isBlank(raw)) {
			names.add("admission");
			return names;
		}
		for (String part : raw.split(",")) {
			if (StringUtils.isNotBlank(part)) {
				names.add(part.trim().toLowerCase(Locale.ENGLISH));
			}
		}
		return names;
	}
	
	public boolean isOdooConfigured() {
		return registrationFeeProperties != null && registrationFeeProperties.isOdooConfigured();
	}
	
	public RegistrationFeeProperties getRegistrationFeeProperties() {
		return registrationFeeProperties;
	}
	
	private String getGp(String property) {
		AdministrationService admin = administrationService;
		if (admin == null && Context.isSessionOpen()) {
			admin = Context.getAdministrationService();
		}
		if (admin == null) {
			return null;
		}
		return admin.getGlobalProperty(property);
	}
	
	private static boolean parseBoolean(String raw, boolean defaultValue) {
		if (StringUtils.isBlank(raw)) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim()) || "yes".equalsIgnoreCase(raw.trim());
	}
	
	private static String defaultIfBlank(String value, String defaultValue) {
		return StringUtils.isBlank(value) ? defaultValue : value.trim();
	}
}
