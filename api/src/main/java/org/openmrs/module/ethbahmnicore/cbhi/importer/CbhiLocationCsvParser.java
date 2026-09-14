/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.cbhi.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Parses CBHI hierarchy CSV with header {@code region,zone,woreda}. Names are kept as in the source
 * file (spaces, underscores, punctuation). No mapping to Address Hierarchy names.
 */
public class CbhiLocationCsvParser {
	
	public static class Row {
		
		private final String region;
		
		private final String zone;
		
		private final String woreda;
		
		public Row(String region, String zone, String woreda) {
			this.region = region;
			this.zone = zone;
			this.woreda = woreda;
		}
		
		public String getRegion() {
			return region;
		}
		
		public String getZone() {
			return zone;
		}
		
		public String getWoreda() {
			return woreda;
		}
	}
	
	public List<Row> parse(InputStream inputStream) throws IOException {
		List<Row> rows = new ArrayList<Row>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String line;
		boolean headerSeen = false;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}
			List<String> cols = splitCsvLine(line);
			if (!headerSeen) {
				headerSeen = true;
				if (looksLikeHeader(cols)) {
					continue;
				}
			}
			if (cols.size() < 3) {
				continue;
			}
			String region = normalize(cols.get(0));
			String zone = normalize(cols.get(1));
			String woreda = normalize(cols.get(2));
			if (StringUtils.isBlank(region) || StringUtils.isBlank(zone) || StringUtils.isBlank(woreda)) {
				continue;
			}
			rows.add(new Row(region, zone, woreda));
		}
		return rows;
	}
	
	private boolean looksLikeHeader(List<String> cols) {
		if (cols.isEmpty()) {
			return false;
		}
		String first = cols.get(0).trim().toLowerCase();
		return "region".equals(first);
	}
	
	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		// Strip optional surrounding quotes; preserve internal punctuation/underscores.
		String v = value.trim();
		if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
			v = v.substring(1, v.length() - 1);
		}
		return v.replace('\u00A0', ' ').trim();
	}
	
	/**
	 * Minimal CSV split supporting quoted fields with commas.
	 */
	List<String> splitCsvLine(String line) {
		List<String> result = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes) {
				result.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		result.add(current.toString());
		return result;
	}
}
