/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.eapts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EaptsUrlsTest {
	
	@Test
	public void buildsTheMediatorPathsFromTheProductionBaseUrl() {
		String base = "http://10.198.144.8/";
		
		assertEquals("http://10.198.144.8/api/api/Patient/Dispensing-unit", EaptsUrls.dispensingUnits(base));
		assertEquals("http://10.198.144.8/api/api/Patient/ItemStock?institutionId=abc-123",
		    EaptsUrls.itemStock(base, "abc-123"));
		assertEquals("http://10.198.144.8/api/api/Patient/GetItemStock?institutionId=abc-123&itemId=drug-uuid",
		    EaptsUrls.itemStockForDrug(base, "abc-123", "drug-uuid"));
	}
	
	@Test
	public void encodesQueryValues() {
		assertEquals("http://10.198.144.8/api/api/Patient/ItemStock?institutionId=a+b",
		    EaptsUrls.itemStock("http://10.198.144.8", "a b"));
	}
}
