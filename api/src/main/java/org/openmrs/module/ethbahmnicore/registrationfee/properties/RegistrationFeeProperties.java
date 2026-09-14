/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.registrationfee.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;

/**
 * Registration-fee config: OpenMRS global properties for non-secrets; environment variables for
 * Odoo credentials.
 */
public class RegistrationFeeProperties {
	
	public static final String GP_ENFORCE = "ethbahmnicore.enforceRegistrationFee";
	
	public static final String GP_WINDOW_DAYS = "ethbahmnicore.registrationFee.paymentWindowDays";
	
	public static final String GP_ODOO_HOST = "ethbahmnicore.odoo.host";
	
	public static final String GP_ODOO_PORT = "ethbahmnicore.odoo.port";
	
	public static final String GP_ODOO_DATABASE = "ethbahmnicore.odoo.database";
	
	public static final String GP_SHOP_NAME = "ethbahmnicore.registrationFee.shopName";
	
	public static final String GP_PRODUCT_NAMES = "ethbahmnicore.registrationFee.productNames";
	
	public static final String GP_ORDER_TYPE_NAME = "ethbahmnicore.registrationFee.orderTypeName";
	
	public static final String GP_ALLOWED_ENCOUNTER_TYPES = "ethbahmnicore.registrationFee.allowedEncounterTypes";
	
	/** Env var for Odoo XML-RPC username (read-only technical user recommended). */
	public static final String ENV_ODOO_USER = "ETHBAHMNICORE_ODOO_USER";
	
	/** Env var for Odoo XML-RPC password. */
	public static final String ENV_ODOO_PASSWORD = "ETHBAHMNICORE_ODOO_PASSWORD";
	
	private static final int DEFAULT_WINDOW_DAYS = 30;
	
	private static final int DEFAULT_ODOO_PORT = 8069;
	
	private AdministrationService administrationService;
	
	private EnvReader envReader = new EnvReader() {
		
		@Override
		public String get(String name) {
			return System.getenv(name);
		}
	};
	
	public void setAdministrationService(AdministrationService administrationService) {
		this.administrationService = administrationService;
	}
	
	public void setEnvReader(EnvReader envReader) {
		this.envReader = envReader;
	}
	
	public boolean isEnforceRegistrationFee() {
		return parseBoolean(getGp(GP_ENFORCE), false);
	}
	
	public int getPaymentWindowDays() {
		String raw = getGp(GP_WINDOW_DAYS);
		if (StringUtils.isBlank(raw)) {
			return DEFAULT_WINDOW_DAYS;
		}
		try {
			int days = Integer.parseInt(raw.trim());
			return days > 0 ? days : DEFAULT_WINDOW_DAYS;
		}
		catch (NumberFormatException e) {
			return DEFAULT_WINDOW_DAYS;
		}
	}
	
	public String getOdooHost() {
		return defaultIfBlank(getGp(GP_ODOO_HOST), "localhost");
	}
	
	public int getOdooPort() {
		String raw = getGp(GP_ODOO_PORT);
		if (StringUtils.isBlank(raw)) {
			return DEFAULT_ODOO_PORT;
		}
		try {
			return Integer.parseInt(raw.trim());
		}
		catch (NumberFormatException e) {
			return DEFAULT_ODOO_PORT;
		}
	}
	
	public String getOdooDatabase() {
		return defaultIfBlank(getGp(GP_ODOO_DATABASE), "odoo");
	}
	
	public String getOdooUser() {
		return StringUtils.trimToEmpty(envReader.get(ENV_ODOO_USER));
	}
	
	public String getOdooPassword() {
		String password = envReader.get(ENV_ODOO_PASSWORD);
		return password == null ? "" : password;
	}
	
	public boolean isOdooConfigured() {
		return StringUtils.isNotBlank(getOdooUser()) && StringUtils.isNotBlank(getOdooHost());
	}
	
	public String getShopName() {
		return defaultIfBlank(getGp(GP_SHOP_NAME), "MRU");
	}
	
	public List<String> getProductNames() {
		String raw = getGp(GP_PRODUCT_NAMES);
		if (StringUtils.isBlank(raw)) {
			return Arrays.asList("Regular Registration Fee", "Emergency Registration Fee");
		}
		List<String> names = new ArrayList<String>();
		for (String part : raw.split(",")) {
			if (StringUtils.isNotBlank(part)) {
				names.add(part.trim());
			}
		}
		return names.isEmpty() ? Arrays.asList("Regular Registration Fee", "Emergency Registration Fee") : names;
	}
	
	public String getOrderTypeName() {
		return defaultIfBlank(getGp(GP_ORDER_TYPE_NAME), "Registration Fee");
	}
	
	public Set<String> getAllowedEncounterTypeNames() {
		String raw = getGp(GP_ALLOWED_ENCOUNTER_TYPES);
		// Bahmni registration UI uses encounter type key/name "REG"; keep "Registration" for aliases.
		if (StringUtils.isBlank(raw)) {
			return new HashSet<String>(Arrays.asList("reg", "registration"));
		}
		Set<String> names = new HashSet<String>();
		for (String part : raw.split(",")) {
			if (StringUtils.isNotBlank(part)) {
				names.add(part.trim().toLowerCase(Locale.ENGLISH));
			}
		}
		return names.isEmpty() ? new HashSet<String>(Arrays.asList("reg", "registration")) : names;
	}
	
	public Set<String> getProductNameSetLower() {
		Set<String> names = new HashSet<String>();
		for (String name : getProductNames()) {
			names.add(name.toLowerCase(Locale.ENGLISH));
		}
		return names;
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
	
	public interface EnvReader {
		
		String get(String name);
	}
}
