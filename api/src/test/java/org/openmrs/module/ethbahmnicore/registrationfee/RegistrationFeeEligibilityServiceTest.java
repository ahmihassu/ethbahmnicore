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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.PatientService;
import org.openmrs.module.ethbahmnicore.registrationfee.contract.EligibilityStatus;
import org.openmrs.module.ethbahmnicore.registrationfee.contract.RegistrationFeeEligibility;
import org.openmrs.module.ethbahmnicore.registrationfee.odoo.OdooException;
import org.openmrs.module.ethbahmnicore.registrationfee.odoo.OdooInvoice;
import org.openmrs.module.ethbahmnicore.registrationfee.odoo.OdooInvoiceRepository;
import org.openmrs.module.ethbahmnicore.registrationfee.properties.RegistrationFeeProperties;
import org.openmrs.module.ethbahmnicore.registrationfee.service.impl.RegistrationFeeEligibilityServiceImpl;
import org.openmrs.module.ethbahmnicore.registrationfee.util.RegistrationFeeAllowList;

public class RegistrationFeeEligibilityServiceTest {
	
	@InjectMocks
	private RegistrationFeeEligibilityServiceImpl service;
	
	@Mock
	private RegistrationFeeProperties properties;
	
	@Mock
	private OdooInvoiceRepository invoiceRepository;
	
	@Mock
	private PatientService patientService;
	
	@Mock
	private RegistrationFeeAllowList allowList;
	
	private Patient patient;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		patient = new Patient();
		patient.setUuid("patient-uuid-1");
		PatientIdentifier id = new PatientIdentifier();
		id.setIdentifier("GAN200000");
		patient.addIdentifier(id);
		
		when(patientService.getPatientByUuid("patient-uuid-1")).thenReturn(patient);
		when(properties.getPaymentWindowDays()).thenReturn(30);
		when(properties.isOdooConfigured()).thenReturn(true);
		when(properties.isEnforceRegistrationFee()).thenReturn(true);
	}
	
	@Test
	public void cashPaid_isAllowed() {
		stubInvoice("Cash", "paid", 101);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertTrue(result.isAllowed());
		assertEquals(EligibilityStatus.CASH_PAID_OK, result.getStatus());
		assertEquals("Cash", result.getPaymentMethod());
		assertEquals(Integer.valueOf(101), result.getInvoiceId());
	}
	
	@Test
	public void cashUnpaid_isDenied() {
		stubInvoice("Cash", "open", 102);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertFalse(result.isAllowed());
		assertEquals(EligibilityStatus.CASH_UNPAID, result.getStatus());
	}
	
	@Test
	public void freePaid_isAllowed() {
		stubInvoice("Free", "paid", 103);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertTrue(result.isAllowed());
		assertEquals(EligibilityStatus.FREE_PAID_OK, result.getStatus());
	}
	
	@Test
	public void freeUnpaid_isDenied() {
		stubInvoice("Free", "open", 104);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertFalse(result.isAllowed());
		assertEquals(EligibilityStatus.FREE_UNPAID, result.getStatus());
	}
	
	@Test
	public void creditOpen_isAllowed() {
		stubInvoice("Credit", "open", 105);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertTrue(result.isAllowed());
		assertEquals(EligibilityStatus.CREDIT_OPEN_OK, result.getStatus());
	}
	
	@Test
	public void creditPaid_isDenied() {
		stubInvoice("Credit", "paid", 106);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertFalse(result.isAllowed());
		assertEquals(EligibilityStatus.CREDIT_MISSING_OR_CLOSED, result.getStatus());
	}
	
	@Test
	public void noInvoice_isDenied() {
		when(invoiceRepository.findLatestMruInvoice(eq("patient-uuid-1"), anyString(), any(Date.class), any(Date.class)))
		        .thenReturn(null);
		when(invoiceRepository.findLatestMruInvoiceAnyTime(eq("patient-uuid-1"), anyString())).thenReturn(null);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertFalse(result.isAllowed());
		assertEquals(EligibilityStatus.NO_INVOICE, result.getStatus());
	}
	
	@Test
	public void outsideWindow_isDenied() {
		when(invoiceRepository.findLatestMruInvoice(eq("patient-uuid-1"), anyString(), any(Date.class), any(Date.class)))
		        .thenReturn(null);
		OdooInvoice old = invoice("Cash", "paid", 200);
		when(invoiceRepository.findLatestMruInvoiceAnyTime(eq("patient-uuid-1"), anyString())).thenReturn(old);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertFalse(result.isAllowed());
		assertEquals(EligibilityStatus.OUTSIDE_WINDOW, result.getStatus());
	}
	
	@Test
	public void enforceDisabled_isAllowedEvenWithoutInvoice() {
		when(properties.isEnforceRegistrationFee()).thenReturn(false);
		when(invoiceRepository.findLatestMruInvoice(eq("patient-uuid-1"), anyString(), any(Date.class), any(Date.class)))
		        .thenReturn(null);
		when(invoiceRepository.findLatestMruInvoiceAnyTime(eq("patient-uuid-1"), anyString())).thenReturn(null);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertTrue(result.isAllowed());
		assertEquals(EligibilityStatus.ENFORCEMENT_DISABLED, result.getStatus());
		assertFalse(result.isEnforceRegistrationFee());
	}
	
	@Test
	public void odooDown_withEnforce_isDenied() {
		when(invoiceRepository.findLatestMruInvoice(eq("patient-uuid-1"), anyString(), any(Date.class), any(Date.class)))
		        .thenThrow(new OdooException("connection refused"));
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertFalse(result.isAllowed());
		assertEquals(EligibilityStatus.ODOO_UNAVAILABLE, result.getStatus());
	}
	
	@Test
	public void odooNotConfigured_withEnforce_isDenied() {
		when(properties.isOdooConfigured()).thenReturn(false);
		RegistrationFeeEligibility result = service.evaluate("patient-uuid-1", null);
		assertFalse(result.isAllowed());
		assertEquals(EligibilityStatus.ODOO_NOT_CONFIGURED, result.getStatus());
	}
	
	@Test
	public void patientNotFound_withEnforce_isDenied() {
		when(patientService.getPatientByUuid("missing")).thenReturn(null);
		RegistrationFeeEligibility result = service.evaluate("missing", null);
		assertFalse(result.isAllowed());
		assertEquals(EligibilityStatus.PATIENT_NOT_FOUND, result.getStatus());
	}
	
	@Test
	public void resolvesByIdentifier() {
		when(patientService.getPatients(isNull(String.class), eq("GAN200000"), isNull(java.util.List.class), eq(true)))
		        .thenReturn(Collections.singletonList(patient));
		stubInvoice("Cash", "paid", 301);
		RegistrationFeeEligibility result = service.evaluate(null, "GAN200000");
		assertTrue(result.isAllowed());
		assertEquals("GAN200000", result.getIdentifier());
	}
	
	private void stubInvoice(String method, String state, int id) {
		when(invoiceRepository.findLatestMruInvoice(eq("patient-uuid-1"), anyString(), any(Date.class), any(Date.class)))
		        .thenReturn(invoice(method, state, id));
	}
	
	private OdooInvoice invoice(String method, String state, int id) {
		OdooInvoice invoice = new OdooInvoice();
		invoice.setId(id);
		invoice.setPaymentMethod(method);
		invoice.setState(state);
		invoice.setDateInvoice(new Date());
		return invoice;
	}
}
