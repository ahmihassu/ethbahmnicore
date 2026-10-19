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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.ethbahmnicore.eapts.EaptsClient;
import org.openmrs.module.ethbahmnicore.eapts.EaptsException;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Stock for the medication page. The browser calls these over the existing HTTPS session; this
 * module calls eAPTS over HTTP.
 * <p>
 * GET /ws/rest/v1/ethbahmnicore/eapts/dispensingUnits
 * <p>
 * GET /ws/rest/v1/ethbahmnicore/eapts/stock?institutionId=
 * <p>
 * GET /ws/rest/v1/ethbahmnicore/eapts/stock?institutionId=&amp;itemId=
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ethbahmnicore/eapts")
public class EaptsStockController extends BaseRestController {
	
	@RequestMapping(method = RequestMethod.GET, value = "/dispensingUnits")
	@ResponseBody
	public SimpleObject dispensingUnits() {
		SimpleObject response = new SimpleObject();
		response.add("model", client().getDispensingUnits());
		return response;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/stock")
	@ResponseBody
	public SimpleObject stock(@RequestParam(value = "institutionId") String institutionId,
	        @RequestParam(value = "itemId", required = false) String itemId) {
		if (StringUtils.isBlank(institutionId)) {
			throw new EaptsException("institutionId is required");
		}
		SimpleObject response = new SimpleObject();
		if (StringUtils.isBlank(itemId)) {
			response.add("model", client().getItemStock(institutionId));
		} else {
			response.add("model", client().getItem(institutionId, itemId));
		}
		return response;
	}
	
	private EaptsClient client() {
		List<EaptsClient> clients = Context.getRegisteredComponents(EaptsClient.class);
		if (clients == null || clients.isEmpty()) {
			throw new EaptsException("eAPTS client is not available");
		}
		return clients.get(0);
	}
}
