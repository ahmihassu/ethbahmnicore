/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.registrationfee.service;

import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.ethbahmnicore.registrationfee.contract.RegistrationFeeEligibility;

/**
 * Evaluates whether the patient has a valid MRU / Registration Fee invoice for the current payment
 * window (Odoo invoice truth).
 */
public interface RegistrationFeeEligibilityService extends OpenmrsService {
	
	/**
	 * @param patientUuid patient uuid (optional if identifier provided)
	 * @param identifier patient identifier (optional if patientUuid provided)
	 */
	RegistrationFeeEligibility evaluate(String patientUuid, String identifier) throws APIException;
	
	/**
	 * Convenience: evaluate and throw {@link APIException} when enforcement is on and not allowed.
	 */
	void assertAllowed(String patientUuid, String identifier) throws APIException;
	
	boolean isEnforceRegistrationFee();
	
	boolean isRegistrationFeeOrder(org.openmrs.Order order);
	
	boolean isRegistrationFeeEncounter(org.openmrs.Encounter encounter);
}
