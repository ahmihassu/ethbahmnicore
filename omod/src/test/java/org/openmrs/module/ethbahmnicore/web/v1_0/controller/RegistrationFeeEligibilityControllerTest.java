/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.web.v1_0.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openmrs.api.APIException;
import org.openmrs.module.ethbahmnicore.registrationfee.contract.EligibilityStatus;
import org.openmrs.module.ethbahmnicore.registrationfee.contract.RegistrationFeeEligibility;
import org.openmrs.module.webservices.rest.SimpleObject;

public class RegistrationFeeEligibilityControllerTest {
	
	@Test
	public void mapsEligibilityToRestContract() {
		RegistrationFeeEligibilityController controller = new RegistrationFeeEligibilityController();
		RegistrationFeeEligibility eligibility = RegistrationFeeEligibility.of(true, EligibilityStatus.CASH_PAID_OK,
		    "Cash invoice is paid");
		eligibility.setPaymentMethod("Cash");
		eligibility.setInvoiceId(55);
		eligibility.setInvoiceState("paid");
		eligibility.setWindowDays(30);
		eligibility.setEnforceRegistrationFee(true);
		eligibility.setPatientUuid("p-1");
		eligibility.setIdentifier("GAN1");
		
		SimpleObject response = controller.toRepresentation(eligibility);
		assertTrue((Boolean) response.get("allowed"));
		assertEquals("CASH_PAID_OK", response.get("status"));
		assertEquals("Cash", response.get("paymentMethod"));
		assertEquals(Integer.valueOf(55), response.get("invoiceId"));
		assertEquals("paid", response.get("invoiceState"));
		assertEquals(Integer.valueOf(30), response.get("windowDays"));
		assertEquals(Boolean.TRUE, response.get("enforceRegistrationFee"));
		assertEquals("p-1", response.get("patientUuid"));
		assertEquals("GAN1", response.get("identifier"));
	}
	
	@Test(expected = APIException.class)
	public void requiresPatientUuidOrIdentifier() {
		new RegistrationFeeEligibilityController().eligibility(null, "  ");
	}
}
