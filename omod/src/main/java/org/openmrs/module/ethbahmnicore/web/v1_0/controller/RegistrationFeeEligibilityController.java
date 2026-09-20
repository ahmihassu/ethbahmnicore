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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.ethbahmnicore.registrationfee.contract.RegistrationFeeEligibility;
import org.openmrs.module.ethbahmnicore.registrationfee.service.RegistrationFeeEligibilityService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * MRU / Registration Fee eligibility for UI gating.
 * <p>
 * GET /ws/rest/v1/ethbahmnicore/registrationFee/eligibility?patientUuid=… or ?identifier=…
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ethbahmnicore/registrationFee/eligibility")
public class RegistrationFeeEligibilityController extends BaseRestController {
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public SimpleObject eligibility(@RequestParam(value = "patientUuid", required = false) String patientUuid,
	        @RequestParam(value = "identifier", required = false) String identifier) {
		if (StringUtils.isBlank(patientUuid) && StringUtils.isBlank(identifier)) {
			throw new APIException("Either patientUuid or identifier is required");
		}
		RegistrationFeeEligibility result = getService().evaluate(patientUuid, identifier);
		return toRepresentation(result);
	}
	
	private RegistrationFeeEligibilityService getService() {
		return Context.getService(RegistrationFeeEligibilityService.class);
	}
	
	SimpleObject toRepresentation(RegistrationFeeEligibility eligibility) {
		SimpleObject obj = new SimpleObject();
		obj.add("allowed", eligibility.isAllowed());
		obj.add("status", eligibility.getStatus() == null ? null : eligibility.getStatus().name());
		obj.add("reason", eligibility.getReason());
		obj.add("paymentMethod", eligibility.getPaymentMethod());
		obj.add("invoiceId", eligibility.getInvoiceId());
		obj.add("invoiceState", eligibility.getInvoiceState());
		obj.add("windowDays", eligibility.getWindowDays());
		obj.add("windowStart", formatDate(eligibility.getWindowStart()));
		obj.add("windowEnd", formatDate(eligibility.getWindowEnd()));
		obj.add("enforceRegistrationFee", eligibility.isEnforceRegistrationFee());
		obj.add("patientUuid", eligibility.getPatientUuid());
		obj.add("identifier", eligibility.getIdentifier());
		return obj;
	}
	
	private static String formatDate(Date date) {
		if (date == null) {
			return null;
		}
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}
}
