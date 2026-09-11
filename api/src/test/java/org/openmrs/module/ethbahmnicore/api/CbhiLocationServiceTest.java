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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.ethbahmnicore.CbhiLocation;
import org.openmrs.module.ethbahmnicore.CbhiLocationLevel;
import org.openmrs.module.ethbahmnicore.api.dao.CbhiLocationDao;
import org.openmrs.module.ethbahmnicore.api.impl.CbhiLocationServiceImpl;

public class CbhiLocationServiceTest {
	
	@InjectMocks
	private CbhiLocationServiceImpl service;
	
	@Mock
	private CbhiLocationDao dao;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void getLocations_withoutParent_shouldRequestRootRegions() {
		when(dao.find(isNull(String.class), eq(CbhiLocationLevel.REGION), isNull(String.class), eq(true))).thenReturn(
		    Collections.<CbhiLocation> emptyList());
		
		service.getLocations(null, null, null);
		
		verify(dao).find(null, CbhiLocationLevel.REGION, null, true);
	}
	
	@Test
	public void getLocations_withParentAndQuery_shouldPassThrough() {
		when(dao.find(eq("parent-uuid"), eq(CbhiLocationLevel.ZONE), eq("awi"), eq(false))).thenReturn(
		    Collections.<CbhiLocation> emptyList());
		
		service.getLocations("parent-uuid", CbhiLocationLevel.ZONE, "awi");
		
		verify(dao).find("parent-uuid", CbhiLocationLevel.ZONE, "awi", false);
	}
	
	@Test
	public void getLocations_withLevelAndQueryOnly_shouldSearchAcrossLevel() {
		when(dao.find(isNull(String.class), eq(CbhiLocationLevel.WOREDA), eq("ada"), eq(false))).thenReturn(
		    Collections.<CbhiLocation> emptyList());
		
		service.getLocations(null, CbhiLocationLevel.WOREDA, "ada");
		
		verify(dao).find(null, CbhiLocationLevel.WOREDA, "ada", false);
	}
	
	@Test
	public void importFromCsv_shouldCreateRegionZoneWoredaHierarchy() {
		when(dao.findByParentAndName(any(CbhiLocation.class), any(String.class), any(CbhiLocationLevel.class)))
		        .thenReturn(Collections.<CbhiLocation> emptyList());
		when(dao.findByParentAndName(isNull(CbhiLocation.class), any(String.class), eq(CbhiLocationLevel.REGION)))
		        .thenReturn(Collections.<CbhiLocation> emptyList());
		when(dao.save(any(CbhiLocation.class))).thenAnswer(invocation -> {
			CbhiLocation loc = (CbhiLocation) invocation.getArguments()[0];
			if (loc.getUuid() == null) {
				loc.setUuid(java.util.UUID.randomUUID().toString());
			}
			return loc;
		});
		
		String csv = "region,zone,woreda\n" + "AFAR,AWSI_RASU_ZONE1,ADA'AR\n" + "AFAR,AWSI_RASU_ZONE1,AFAMBO\n"
		        + "AFAR,KILBET RASU (ZONE 2),ABALA\n";
		
		int leaves = service.importFromCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), false);
		
		assertEquals(3, leaves);
		ArgumentCaptor<CbhiLocation> captor = ArgumentCaptor.forClass(CbhiLocation.class);
		// 1 region + 2 zones + 3 woredas
		verify(dao, times(6)).save(captor.capture());
		List<CbhiLocation> saved = captor.getAllValues();
		assertEquals(CbhiLocationLevel.REGION, saved.get(0).getLevel());
		assertEquals("AFAR", saved.get(0).getName());
		assertEquals(CbhiLocationLevel.ZONE, saved.get(1).getLevel());
		assertEquals("AWSI_RASU_ZONE1", saved.get(1).getName());
		assertSame(saved.get(0), saved.get(1).getParent());
		assertEquals(CbhiLocationLevel.WOREDA, saved.get(2).getLevel());
		assertEquals("ADA'AR", saved.get(2).getName());
		verify(dao, never()).deleteAll();
	}
	
	@Test
	public void importFromCsv_withReplace_shouldClearExisting() {
		when(dao.findByParentAndName(any(CbhiLocation.class), any(String.class), any(CbhiLocationLevel.class)))
		        .thenReturn(Collections.<CbhiLocation> emptyList());
		when(dao.findByParentAndName(isNull(CbhiLocation.class), any(String.class), any(CbhiLocationLevel.class)))
		        .thenReturn(Collections.<CbhiLocation> emptyList());
		when(dao.save(any(CbhiLocation.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
		
		String csv = "region,zone,woreda\nAFAR,Z1,W1\n";
		service.importFromCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), true);
		
		verify(dao).deleteAll();
	}
	
	@Test
	public void importFromCsv_shouldReuseExistingRegion() {
		CbhiLocation existingRegion = new CbhiLocation();
		existingRegion.setName("AFAR");
		existingRegion.setLevel(CbhiLocationLevel.REGION);
		existingRegion.setUuid("region-uuid");
		
		when(dao.findByParentAndName(isNull(CbhiLocation.class), eq("AFAR"), eq(CbhiLocationLevel.REGION)))
		        .thenReturn(Arrays.asList(existingRegion));
		when(dao.findByParentAndName(eq(existingRegion), any(String.class), eq(CbhiLocationLevel.ZONE)))
		        .thenReturn(Collections.<CbhiLocation> emptyList());
		when(dao.findByParentAndName(any(CbhiLocation.class), any(String.class), eq(CbhiLocationLevel.WOREDA)))
		        .thenReturn(Collections.<CbhiLocation> emptyList());
		when(dao.save(any(CbhiLocation.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
		
		String csv = "region,zone,woreda\nAFAR,Z1,W1\n";
		service.importFromCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), false);
		
		ArgumentCaptor<CbhiLocation> captor = ArgumentCaptor.forClass(CbhiLocation.class);
		verify(dao, times(2)).save(captor.capture());
		assertEquals(CbhiLocationLevel.ZONE, captor.getAllValues().get(0).getLevel());
		assertSame(existingRegion, captor.getAllValues().get(0).getParent());
	}
}
