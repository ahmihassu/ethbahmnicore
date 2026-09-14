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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.ethbahmnicore.cbhi.CbhiConstants;
import org.openmrs.module.ethbahmnicore.cbhi.model.CbhiLocation;
import org.openmrs.module.ethbahmnicore.cbhi.model.CbhiLocationLevel;
import org.openmrs.module.ethbahmnicore.cbhi.service.CbhiLocationService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Cascading CBHI geography lookup for registration UI.
 * <p>
 * GET /ws/rest/v1/ethbahmnicore/cbhiLocation?parentUuid=&amp;q=&amp;level=
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ethbahmnicore/cbhiLocation")
public class CbhiLocationController extends BaseRestController {
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public SimpleObject search(@RequestParam(value = "parentUuid", required = false) String parentUuid,
	        @RequestParam(value = "q", required = false) String q,
	        @RequestParam(value = "level", required = false) String level) {
		CbhiLocationLevel locationLevel = parseLevel(level);
		List<CbhiLocation> locations = getService().getLocations(parentUuid, locationLevel, q);
		
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		for (CbhiLocation location : locations) {
			results.add(toRepresentation(location));
		}
		
		SimpleObject response = new SimpleObject();
		response.add("results", results);
		return response;
	}
	
	/**
	 * Imports packaged default CSV. Use replace=true to wipe and reload. POST
	 * /ws/rest/v1/ethbahmnicore/cbhiLocation/import?replace=false
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/import")
	@ResponseBody
	public SimpleObject importHierarchy(
	        @RequestParam(value = "replace", required = false, defaultValue = "false") boolean replace) throws Exception {
		CbhiLocationService service = getService();
		int imported;
		if (!replace && service.getCount() > 0) {
			imported = 0;
		} else {
			InputStream in = Thread.currentThread().getContextClassLoader()
			        .getResourceAsStream(CbhiConstants.DEFAULT_HIERARCHY_CSV);
			if (in == null) {
				in = getClass().getClassLoader().getResourceAsStream(CbhiConstants.DEFAULT_HIERARCHY_CSV);
			}
			if (in == null) {
				throw new IllegalStateException("Packaged CBHI CSV not found: " + CbhiConstants.DEFAULT_HIERARCHY_CSV);
			}
			try {
				imported = service.importFromCsv(in, replace);
			}
			finally {
				in.close();
			}
		}
		SimpleObject response = new SimpleObject();
		response.add("importedWoredas", imported);
		response.add("totalLocations", service.getCount());
		return response;
	}
	
	private CbhiLocationService getService() {
		return Context.getService(CbhiLocationService.class);
	}
	
	private CbhiLocationLevel parseLevel(String level) {
		if (StringUtils.isBlank(level)) {
			return null;
		}
		try {
			return CbhiLocationLevel.valueOf(level.trim().toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid level '" + level + "'. Expected REGION, ZONE, or WOREDA.");
		}
	}
	
	private SimpleObject toRepresentation(CbhiLocation location) {
		SimpleObject obj = new SimpleObject();
		obj.add("uuid", location.getUuid());
		obj.add("name", location.getName());
		obj.add("level", location.getLevel() == null ? null : location.getLevel().name());
		obj.add("parentUuid", location.getParent() == null ? null : location.getParent().getUuid());
		obj.add("display", location.getName());
		return obj;
	}
}
