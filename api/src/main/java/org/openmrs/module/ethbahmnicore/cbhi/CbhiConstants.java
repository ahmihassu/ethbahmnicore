/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.cbhi;

/**
 * Shared constants for CBHI / SHI / Insurance geography person attributes and packaged hierarchy
 * data. Person attribute type names must match exactly what registration UI writes.
 */
public final class CbhiConstants {
	
	public static final String PERSON_ATTR_CBHI_REGION = "CBHI Region";
	
	public static final String PERSON_ATTR_CBHI_ZONE = "CBHI Zone";
	
	public static final String PERSON_ATTR_CBHI_WOREDA = "CBHI Woreda";
	
	public static final String PERSON_ATTR_CBHI_KEBELE = "CBHI Kebele";
	
	/** Existing attributes (created outside this module; documented for UI contract). */
	public static final String PERSON_ATTR_CBHI_ID = "CBHI ID";
	
	public static final String PERSON_ATTR_CBHI_EXPIRY_DATE = "CBHIExpiryDate";
	
	public static final String PERSON_ATTR_SHI_ID = "SHI ID";
	
	public static final String PERSON_ATTR_SHI_REGION = "SHI Region";
	
	public static final String PERSON_ATTR_SHI_ZONE = "SHI Zone";
	
	public static final String PERSON_ATTR_SHI_WOREDA = "SHI Woreda";
	
	public static final String PERSON_ATTR_SHI_KEBELE = "SHI Kebele";
	
	public static final String PERSON_ATTR_POLICE_OFFICER_NAME = "Police Officer Name";
	
	public static final String PERSON_ATTR_POLICE_OFFICER_PHONE = "Police Officer Phone";
	
	public static final String PERSON_ATTR_INSURANCE_REGION = "Insurance Region";
	
	public static final String PERSON_ATTR_INSURANCE_ZONE = "Insurance Zone";
	
	public static final String PERSON_ATTR_INSURANCE_WOREDA = "Insurance Woreda";
	
	public static final String PERSON_ATTR_TYPE_UUID_REGION = "5e3c8f91-4b2a-4d6e-9c1f-a1b2c3d4e001";
	
	public static final String PERSON_ATTR_TYPE_UUID_ZONE = "5e3c8f91-4b2a-4d6e-9c1f-a1b2c3d4e002";
	
	public static final String PERSON_ATTR_TYPE_UUID_WOREDA = "5e3c8f91-4b2a-4d6e-9c1f-a1b2c3d4e003";
	
	public static final String PERSON_ATTR_TYPE_UUID_KEBELE = "5e3c8f91-4b2a-4d6e-9c1f-a1b2c3d4e004";
	
	/** Classpath resource packaged with the module (CSV header: region,zone,woreda). */
	public static final String DEFAULT_HIERARCHY_CSV = "cbhi/cbhi_locations.csv";
	
	private CbhiConstants() {
	}
}
