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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.AdministrationService;

public class EaptsPropertiesTest {
	
	@Mock
	private AdministrationService administrationService;
	
	private EaptsProperties properties;
	
	private final Map<String, String> env = new HashMap<String, String>();
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		properties = new EaptsProperties();
		properties.setAdministrationService(administrationService);
		properties.setEnvReader(new EaptsProperties.EnvReader() {
			
			@Override
			public String get(String name) {
				return env.get(name);
			}
		});
	}
	
	@Test
	public void defaultsBaseUrlToTheWachemoEaptsHost() {
		when(administrationService.getGlobalProperty(EaptsProperties.GP_BASE_URL)).thenReturn(null);
		assertEquals("http://10.198.144.8", properties.getBaseUrl());
	}
	
	@Test
	public void stripsTrailingSlashFromConfiguredBaseUrl() {
		when(administrationService.getGlobalProperty(EaptsProperties.GP_BASE_URL)).thenReturn("http://10.198.144.8/");
		assertEquals("http://10.198.144.8", properties.getBaseUrl());
	}
	
	@Test
	public void credentialsPreferEthbahmnicoreEnvAndFallBackToMediatorNames() {
		env.put(EaptsProperties.ENV_USER_FALLBACK, "mediator-user");
		env.put(EaptsProperties.ENV_PASSWORD_FALLBACK, "mediator-secret");
		assertEquals("mediator-user", properties.getUsername());
		assertEquals("mediator-secret", properties.getPassword());
		assertTrue(properties.isConfigured());
		
		env.put(EaptsProperties.ENV_USER, "emr-user");
		env.put(EaptsProperties.ENV_PASSWORD, "emr-secret");
		assertEquals("emr-user", properties.getUsername());
		assertEquals("emr-secret", properties.getPassword());
	}
	
	@Test
	public void missingPasswordIsNotConfigured() {
		env.put(EaptsProperties.ENV_USER, "emr-user");
		assertFalse(properties.isConfigured());
	}
	
	@Test(expected = EaptsException.class)
	public void rejectsABaseUrlThatIsNotHttp() {
		when(administrationService.getGlobalProperty(EaptsProperties.GP_BASE_URL)).thenReturn("10.198.144.8");
		properties.getBaseUrl();
	}
}
