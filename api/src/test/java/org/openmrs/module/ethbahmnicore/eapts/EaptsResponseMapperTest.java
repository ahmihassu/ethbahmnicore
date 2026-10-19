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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class EaptsResponseMapperTest {
	
	@Test
	public void mapsDispensingUnitsAndRenamesRowguid() {
		String json = "{\"model\":[{\"institutionName\":\"OPD Pharmacy\",\"rowguid\":\"abc-123\"}]}";
		
		List<Map<String, Object>> units = EaptsResponseMapper.dispensingUnits(json);
		
		assertEquals(1, units.size());
		assertEquals("OPD Pharmacy", units.get(0).get("institutionName"));
		assertEquals("abc-123", units.get(0).get("rowGuid"));
	}
	
	@Test
	public void mapsDispensingUnitsWhenModelIsADictionary() {
		String json = "{\"model\":{\"0\":{\"institutionName\":\"Store\",\"rowGuid\":\"guid-1\"}}}";
		
		List<Map<String, Object>> units = EaptsResponseMapper.dispensingUnits(json);
		
		assertEquals("guid-1", units.get(0).get("rowGuid"));
	}
	
	@Test
	public void mapsItemStockRows() {
		String json = "{\"model\":[{\"itemUnit\":\"drug-uuid\",\"institution\":\"OPD Pharmacy\",\"hasStock\":true,\"quantity\":12.5}]}";
		
		List<Map<String, Object>> items = EaptsResponseMapper.itemStock(json);
		
		assertEquals("drug-uuid", items.get(0).get("itemUnit"));
		assertEquals("OPD Pharmacy", items.get(0).get("institution"));
		assertEquals(Boolean.TRUE, items.get(0).get("hasStock"));
		assertEquals(Double.valueOf(12.5), items.get(0).get("quantity"));
	}
	
	@Test
	public void singleItemObjectIsReturnedAsTheMedicationPageExpects() {
		String json = "{\"model\":{\"itemUnit\":\"drug-uuid\",\"hasStock\":false,\"quantity\":0}}";
		
		Map<String, Object> item = EaptsResponseMapper.singleItem(json, "drug-uuid");
		
		assertFalse((Boolean) item.get("hasStock"));
		assertEquals(Long.valueOf(0), item.get("quantity"));
		assertEquals("drug-uuid", item.get("itemUnit"));
	}
	
	@Test
	public void singleItemUnwrapsAOneElementListAndPrefersTheRequestedDrug() {
		String json = "{\"model\":[" + "{\"itemUnit\":\"other\",\"hasStock\":true,\"quantity\":4},"
		        + "{\"itemUnit\":\"wanted\",\"hasStock\":false,\"quantity\":1}" + "]}";
		
		Map<String, Object> item = EaptsResponseMapper.singleItem(json, "wanted");
		
		assertEquals("wanted", item.get("itemUnit"));
		assertFalse((Boolean) item.get("hasStock"));
	}
	
	@Test
	public void nullModelBecomesAnEmptyStockList() {
		assertTrue(EaptsResponseMapper.itemStock("{\"model\":null}").isEmpty());
		assertNull(EaptsResponseMapper.singleItem("{\"model\":null}", "drug-uuid"));
	}
	
	@Test(expected = EaptsException.class)
	public void rejectsNonJson() {
		EaptsResponseMapper.dispensingUnits("<html>nope</html>");
	}
}
