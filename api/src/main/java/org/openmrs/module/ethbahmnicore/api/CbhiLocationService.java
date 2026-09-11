/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.api;

import java.io.InputStream;
import java.util.List;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.ethbahmnicore.CbhiLocation;
import org.openmrs.module.ethbahmnicore.CbhiLocationLevel;
import org.openmrs.module.ethbahmnicore.EthioBahmniCoreConfig;

/**
 * Service for CBHI geography hierarchy (Region → Zone → Woreda).
 */
public interface CbhiLocationService extends OpenmrsService {
	
	@Authorized()
	CbhiLocation getByUuid(String uuid) throws APIException;
	
	@Authorized()
	List<CbhiLocation> getLocations(String parentUuid, CbhiLocationLevel level, String q) throws APIException;
	
	@Authorized(EthioBahmniCoreConfig.MODULE_PRIVILEGE)
	CbhiLocation save(CbhiLocation location) throws APIException;
	
	/**
	 * Imports hierarchy from CSV. When {@code replaceExisting} is true, existing rows are removed
	 * first. Returns number of woreda leaf rows processed.
	 */
	@Authorized(EthioBahmniCoreConfig.MODULE_PRIVILEGE)
	int importFromCsv(InputStream csvStream, boolean replaceExisting) throws APIException;
	
	/**
	 * Loads packaged {@code cbhi/cbhi_locations.csv} when the table is empty. Returns imported leaf
	 * count, or 0 if data already present.
	 */
	@Authorized(EthioBahmniCoreConfig.MODULE_PRIVILEGE)
	int ensureDefaultHierarchyImported() throws APIException;
	
	@Authorized()
	long getCount() throws APIException;
}
