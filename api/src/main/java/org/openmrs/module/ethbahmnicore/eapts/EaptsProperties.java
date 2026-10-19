/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.eapts;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;

/**
 * eAPTS connection settings. The base URL is a global property. Credentials stay in environment
 * variables, matching the mediator's {@code APTS_USERNAME} / {@code APTS_PASSWORD}.
 */
public class EaptsProperties {
	
	public static final String GP_BASE_URL = "ethbahmnicore.eapts.baseUrl";
	
	public static final String ENV_USER = "ETHBAHMNICORE_EAPTS_USER";
	
	public static final String ENV_PASSWORD = "ETHBAHMNICORE_EAPTS_PASSWORD";
	
	/** Same names the stock mediator reads, accepted when the ethbahmnicore vars are unset. */
	public static final String ENV_USER_FALLBACK = "APTS_USERNAME";
	
	public static final String ENV_PASSWORD_FALLBACK = "APTS_PASSWORD";
	
	public static final String DEFAULT_BASE_URL = "http://10.198.144.8";
	
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
	
	public String getBaseUrl() {
		String baseUrl = defaultIfBlank(getGp(GP_BASE_URL), DEFAULT_BASE_URL);
		if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
			throw new EaptsException("eAPTS base URL must start with http:// or https://");
		}
		return EaptsUrls.normalize(baseUrl);
	}
	
	public String getUsername() {
		return firstNonBlank(envReader.get(ENV_USER), envReader.get(ENV_USER_FALLBACK));
	}
	
	public String getPassword() {
		String password = envReader.get(ENV_PASSWORD);
		if (StringUtils.isEmpty(password)) {
			password = envReader.get(ENV_PASSWORD_FALLBACK);
		}
		return password == null ? "" : password;
	}
	
	public boolean isConfigured() {
		return StringUtils.isNotBlank(getUsername()) && StringUtils.isNotEmpty(getPassword());
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
	
	private static String firstNonBlank(String primary, String fallback) {
		if (StringUtils.isNotBlank(primary)) {
			return primary.trim();
		}
		return StringUtils.trimToEmpty(fallback);
	}
	
	private static String defaultIfBlank(String value, String defaultValue) {
		return StringUtils.isBlank(value) ? defaultValue : value.trim();
	}
	
	public interface EnvReader {
		
		String get(String name);
	}
}
