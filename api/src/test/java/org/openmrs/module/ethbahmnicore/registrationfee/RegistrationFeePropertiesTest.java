/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.registrationfee;

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
import org.openmrs.module.ethbahmnicore.registrationfee.properties.RegistrationFeeProperties;

public class RegistrationFeePropertiesTest {
	
	@Mock
	private AdministrationService administrationService;
	
	private RegistrationFeeProperties properties;
	
	private final Map<String, String> env = new HashMap<String, String>();
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		properties = new RegistrationFeeProperties();
		properties.setAdministrationService(administrationService);
		properties.setEnvReader(new RegistrationFeeProperties.EnvReader() {
			
			@Override
			public String get(String name) {
				return env.get(name);
			}
		});
	}
	
	@Test
	public void readsEnforceFlagFromGp() {
		when(administrationService.getGlobalProperty(RegistrationFeeProperties.GP_ENFORCE)).thenReturn("true");
		assertTrue(properties.isEnforceRegistrationFee());
	}
	
	@Test
	public void credentialsComeFromEnvNotGps() {
		env.put(RegistrationFeeProperties.ENV_ODOO_USER, "mru_reader");
		env.put(RegistrationFeeProperties.ENV_ODOO_PASSWORD, "secret");
		when(administrationService.getGlobalProperty(RegistrationFeeProperties.GP_ODOO_HOST)).thenReturn("odoo.local");
		assertEquals("mru_reader", properties.getOdooUser());
		assertEquals("secret", properties.getOdooPassword());
		assertTrue(properties.isOdooConfigured());
	}
	
	@Test
	public void missingEnvUser_isNotConfigured() {
		env.clear();
		when(administrationService.getGlobalProperty(RegistrationFeeProperties.GP_ODOO_HOST)).thenReturn("localhost");
		assertFalse(properties.isOdooConfigured());
	}
}
