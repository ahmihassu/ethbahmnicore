/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.api.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

public class CbhiLocationCsvParserTest {
	
	@Test
	public void parse_shouldReadRegionZoneWoredaRows() throws Exception {
		String csv = "region,zone,woreda\n" + "AFAR,AWSI_RASU_ZONE1,ADA'AR\n" + "AFAR,KILBET RASU (ZONE 2),ABALA\n"
		        + "AMHARA,AGEW AWI ZONE,DANGILA\n";
		CbhiLocationCsvParser parser = new CbhiLocationCsvParser();
		List<CbhiLocationCsvParser.Row> rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
		
		assertEquals(3, rows.size());
		assertEquals("AFAR", rows.get(0).getRegion());
		assertEquals("AWSI_RASU_ZONE1", rows.get(0).getZone());
		assertEquals("ADA'AR", rows.get(0).getWoreda());
		assertEquals("KILBET RASU (ZONE 2)", rows.get(1).getZone());
	}
	
	@Test
	public void parse_shouldPreserveQuotedCommasAndPunctuation() throws Exception {
		String csv = "region,zone,woreda\n" + "\"OROMIA\",\"EAST HARARGE\",\"GURA MU\"\"LE\"\n";
		CbhiLocationCsvParser parser = new CbhiLocationCsvParser();
		List<CbhiLocationCsvParser.Row> rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
		assertEquals(1, rows.size());
		assertEquals("GURA MU\"LE", rows.get(0).getWoreda());
	}
	
	@Test
	public void parse_shouldSkipBlankAndIncompleteRows() throws Exception {
		String csv = "region,zone,woreda\n" + "AFAR,ZONE1,W1\n" + "\n" + "AFAR,ZONE1,\n" + ",,\n";
		CbhiLocationCsvParser parser = new CbhiLocationCsvParser();
		List<CbhiLocationCsvParser.Row> rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
		assertEquals(1, rows.size());
		assertTrue(rows.get(0).getWoreda().equals("W1"));
	}
	
	@Test
	public void splitCsvLine_shouldHandleSimpleAndQuotedFields() {
		CbhiLocationCsvParser parser = new CbhiLocationCsvParser();
		List<String> cols = parser.splitCsvLine("A,\"B,C\",D");
		assertEquals(3, cols.size());
		assertEquals("B,C", cols.get(1));
	}
}
