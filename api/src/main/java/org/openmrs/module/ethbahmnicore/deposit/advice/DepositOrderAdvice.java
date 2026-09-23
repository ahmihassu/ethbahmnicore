/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.deposit.advice;

import java.lang.reflect.Method;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.ethbahmnicore.deposit.properties.DepositProperties;
import org.openmrs.module.ethbahmnicore.deposit.service.DepositEligibilityService;
import org.springframework.aop.MethodBeforeAdvice;

/**
 * Blocks Cash IPD order saves when deposit cannot cover the order price.
 */
public class DepositOrderAdvice implements MethodBeforeAdvice {
	
	private static final Log log = LogFactory.getLog(DepositOrderAdvice.class);
	
	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (!"saveOrder".equals(method.getName()) || args == null || args.length == 0) {
			return;
		}
		if (!(args[0] instanceof Order)) {
			return;
		}
		Order order = (Order) args[0];
		DepositEligibilityService service = Context.getService(DepositEligibilityService.class);
		if (service == null || !service.isEnforceIpdDeposit()) {
			return;
		}
		service.assertOrderAllowed(order);
	}
}
