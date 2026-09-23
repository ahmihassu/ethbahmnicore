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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.ethbahmnicore.deposit.service.DepositEligibilityService;
import org.springframework.aop.MethodBeforeAdvice;

/**
 * Blocks Admission encounter saves for Cash patients without minimum IPD deposit.
 */
public class DepositEncounterAdvice implements MethodBeforeAdvice {
	
	private static final Log log = LogFactory.getLog(DepositEncounterAdvice.class);
	
	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (!"saveEncounter".equals(method.getName()) || args == null || args.length == 0) {
			return;
		}
		if (!(args[0] instanceof Encounter)) {
			return;
		}
		Encounter encounter = (Encounter) args[0];
		DepositEligibilityService service = Context.getService(DepositEligibilityService.class);
		if (service == null || !service.isEnforceIpdDeposit()) {
			return;
		}
		
		EncounterType type = encounter.getEncounterType();
		if (type == null || !service.isAdmissionEncounterType(type.getName())) {
			return;
		}
		
		Patient patient = encounter.getPatient();
		if (patient == null) {
			return;
		}
		log.debug("Checking IPD deposit before admission encounter for " + patient.getUuid());
		service.assertAdmitAllowed(patient.getUuid());
	}
}
