/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.cbhi.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ethbahmnicore.cbhi.CbhiConstants;
import org.openmrs.module.ethbahmnicore.cbhi.model.CbhiLocation;
import org.openmrs.module.ethbahmnicore.cbhi.model.CbhiLocationLevel;
import org.openmrs.module.ethbahmnicore.cbhi.service.CbhiLocationService;
import org.openmrs.module.ethbahmnicore.cbhi.dao.CbhiLocationDao;
import org.openmrs.module.ethbahmnicore.cbhi.importer.CbhiLocationCsvParser;
import org.springframework.transaction.annotation.Transactional;

public class CbhiLocationServiceImpl extends BaseOpenmrsService implements CbhiLocationService {
	
	private static final Log log = LogFactory.getLog(CbhiLocationServiceImpl.class);
	
	private CbhiLocationDao dao;
	
	public void setDao(CbhiLocationDao dao) {
		this.dao = dao;
	}
	
	@Override
	@Transactional(readOnly = true)
	public CbhiLocation getByUuid(String uuid) throws APIException {
		return dao.getByUuid(uuid);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<CbhiLocation> getLocations(String parentUuid, CbhiLocationLevel level, String q) throws APIException {
		String parent = StringUtils.isBlank(parentUuid) ? null : parentUuid.trim();
		boolean hasQuery = StringUtils.isNotBlank(q);
		
		// Cascading UI: no parent → root regions (unless free-text search with explicit level).
		boolean rootsOnly = (parent == null) && !(hasQuery && level != null);
		if (parent == null && !hasQuery && level == null) {
			level = CbhiLocationLevel.REGION;
			rootsOnly = true;
		}
		return dao.find(parent, level, q, rootsOnly);
	}
	
	@Override
	@Transactional
	public CbhiLocation save(CbhiLocation location) throws APIException {
		return dao.save(location);
	}
	
	@Override
	@Transactional
	public int importFromCsv(InputStream csvStream, boolean replaceExisting) throws APIException {
		CbhiLocationCsvParser parser = new CbhiLocationCsvParser();
		List<CbhiLocationCsvParser.Row> rows;
		try {
			rows = parser.parse(csvStream);
		}
		catch (IOException e) {
			throw new APIException("Failed to parse CBHI location CSV", e);
		}
		
		if (replaceExisting) {
			dao.deleteAll();
		}
		
		Map<String, CbhiLocation> regionByName = new HashMap<String, CbhiLocation>();
		Map<String, CbhiLocation> zoneByKey = new HashMap<String, CbhiLocation>();
		
		int leafCount = 0;
		for (CbhiLocationCsvParser.Row row : rows) {
			CbhiLocation region = regionByName.get(row.getRegion().toLowerCase());
			if (region == null) {
				region = findOrCreate(null, row.getRegion(), CbhiLocationLevel.REGION);
				regionByName.put(row.getRegion().toLowerCase(), region);
			}
			
			String zoneKey = row.getRegion().toLowerCase() + "|" + row.getZone().toLowerCase();
			CbhiLocation zone = zoneByKey.get(zoneKey);
			if (zone == null) {
				zone = findOrCreate(region, row.getZone(), CbhiLocationLevel.ZONE);
				zoneByKey.put(zoneKey, zone);
			}
			
			findOrCreate(zone, row.getWoreda(), CbhiLocationLevel.WOREDA);
			leafCount++;
		}
		
		log.info("Imported CBHI hierarchy: " + regionByName.size() + " regions, " + zoneByKey.size() + " zones, "
		        + leafCount + " woredas");
		return leafCount;
	}
	
	private CbhiLocation findOrCreate(CbhiLocation parent, String name, CbhiLocationLevel level) {
		List<CbhiLocation> existing = dao.findByParentAndName(parent, name, level);
		if (!existing.isEmpty()) {
			return existing.get(0);
		}
		CbhiLocation location = new CbhiLocation();
		location.setName(name);
		location.setLevel(level);
		location.setParent(parent);
		return dao.save(location);
	}
	
	@Override
	@Transactional
	public int ensureDefaultHierarchyImported() throws APIException {
		if (dao.getCount() > 0) {
			return 0;
		}
		InputStream in = getClass().getClassLoader().getResourceAsStream(CbhiConstants.DEFAULT_HIERARCHY_CSV);
		if (in == null) {
			throw new APIException("Packaged CBHI CSV not found: " + CbhiConstants.DEFAULT_HIERARCHY_CSV);
		}
		try {
			return importFromCsv(in, false);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
	}
	
	@Override
	@Transactional(readOnly = true)
	public long getCount() throws APIException {
		return dao.getCount();
	}
}
