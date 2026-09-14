/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.registrationfee.advice;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.ethbahmnicore.registrationfee.service.RegistrationFeeEligibilityService;
import org.springframework.aop.MethodBeforeAdvice;

/**
 * Blocks non–Registration-Fee order saves when MRU payment is invalid for the current window.
 * Registration Fee orders themselves remain allowed so billing can proceed after visit create.
 */
public class RegistrationFeeOrderAdvice implements MethodBeforeAdvice {
	
	private static final Log log = LogFactory.getLog(RegistrationFeeOrderAdvice.class);
	
	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (!"saveOrder".equals(method.getName()) || args == null || args.length == 0) {
			return;
		}
		if (!(args[0] instanceof Order)) {
			return;
		}
		Order order = (Order) args[0];
		
		RegistrationFeeEligibilityService eligibilityService = Context.getService(RegistrationFeeEligibilityService.class);
		if (eligibilityService == null || !eligibilityService.isEnforceRegistrationFee()) {
			return;
		}
		if (eligibilityService.isRegistrationFeeOrder(order)) {
			log.debug("Allowing Registration Fee order save");
			return;
		}
		
		Patient patient = order.getPatient();
		if (patient == null && order.getEncounter() != null) {
			patient = order.getEncounter().getPatient();
		}
		if (patient == null) {
			return;
		}
		eligibilityService.assertAllowed(patient.getUuid(), null);
	}
}
