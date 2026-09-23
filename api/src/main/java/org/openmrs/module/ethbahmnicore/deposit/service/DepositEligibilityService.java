/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.deposit.service;

import org.openmrs.Order;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.ethbahmnicore.deposit.contract.DepositEligibility;

public interface DepositEligibilityService extends OpenmrsService {
	
	boolean isEnforceIpdDeposit();
	
	DepositEligibility evaluateForAdmit(String patientUuid, String identifier) throws APIException;
	
	DepositEligibility evaluateForOrder(String patientUuid, String identifier, String conceptUuid, Double quantity)
	        throws APIException;
	
	/**
	 * Header / soft-status: deposit applies only when patient is currently IPD and Cash.
	 */
	DepositEligibility evaluateForStatus(String patientUuid, String identifier) throws APIException;
	
	void assertAdmitAllowed(String patientUuid) throws APIException;
	
	void assertOrderAllowed(Order order) throws APIException;
	
	boolean isDepositExemptOrder(Order order);
	
	boolean isAdmissionEncounterType(String encounterTypeName);
}
