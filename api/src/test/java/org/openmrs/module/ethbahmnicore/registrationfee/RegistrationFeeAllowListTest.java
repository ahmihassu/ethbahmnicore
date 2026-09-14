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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.module.ethbahmnicore.registrationfee.properties.RegistrationFeeProperties;
import org.openmrs.module.ethbahmnicore.registrationfee.util.RegistrationFeeAllowList;

public class RegistrationFeeAllowListTest {
	
	@Mock
	private RegistrationFeeProperties properties;
	
	private RegistrationFeeAllowList allowList;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		allowList = new RegistrationFeeAllowList();
		allowList.setProperties(properties);
		when(properties.getOrderTypeName()).thenReturn("Registration Fee");
		when(properties.getProductNameSetLower()).thenReturn(
		    new HashSet<String>(Arrays.asList("regular registration fee", "emergency registration fee")));
		when(properties.getAllowedEncounterTypeNames()).thenReturn(
		    new HashSet<String>(Collections.singletonList("registration")));
	}
	
	@Test
	public void recognizesFeeOrderByOrderType() {
		Order order = new Order();
		OrderType type = new OrderType();
		type.setName("Registration Fee");
		order.setOrderType(type);
		assertTrue(allowList.isRegistrationFeeOrder(order));
	}
	
	@Test
	public void recognizesFeeOrderByConceptName() {
		Order order = new Order();
		Concept concept = new Concept();
		ConceptName name = new ConceptName("Regular Registration Fee", java.util.Locale.ENGLISH);
		name.setConceptNameType(org.openmrs.api.ConceptNameType.FULLY_SPECIFIED);
		concept.addName(name);
		order.setConcept(concept);
		assertTrue(allowList.isRegistrationFeeOrder(order));
	}
	
	@Test
	public void rejectsClinicalOrder() {
		Order order = new Order();
		Concept concept = new Concept();
		ConceptName name = new ConceptName("Blood Pressure", java.util.Locale.ENGLISH);
		name.setConceptNameType(org.openmrs.api.ConceptNameType.FULLY_SPECIFIED);
		concept.addName(name);
		order.setConcept(concept);
		assertFalse(allowList.isRegistrationFeeOrder(order));
	}
	
	@Test
	public void recognizesRegistrationEncounterType() {
		Encounter encounter = new Encounter();
		EncounterType type = new EncounterType();
		type.setName("Registration");
		encounter.setEncounterType(type);
		assertTrue(allowList.isRegistrationFeeEncounter(encounter));
	}
	
	@Test
	public void recognizesRegEncounterType() {
		when(properties.getAllowedEncounterTypeNames()).thenReturn(
		    new HashSet<String>(Arrays.asList("reg", "registration")));
		Encounter encounter = new Encounter();
		EncounterType type = new EncounterType();
		type.setName("REG");
		encounter.setEncounterType(type);
		assertTrue(allowList.isRegistrationFeeEncounter(encounter));
	}
}
