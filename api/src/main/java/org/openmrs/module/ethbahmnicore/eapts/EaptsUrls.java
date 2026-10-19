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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * eAPTS paths used by the stock mediator ({@code DU_URL} and {@code STOCK_URL + '=' + rowGuid}).
 * Per-drug lookup is the same query the medication page used to call directly.
 */
public final class EaptsUrls {
	
	private EaptsUrls() {
	}
	
	public static String dispensingUnits(String baseUrl) {
		return normalize(baseUrl) + "/api/api/Patient/Dispensing-unit";
	}
	
	public static String itemStock(String baseUrl, String institutionId) {
		return normalize(baseUrl) + "/api/api/Patient/ItemStock?institutionId=" + encode(institutionId);
	}
	
	public static String itemStockForDrug(String baseUrl, String institutionId, String itemId) {
		return normalize(baseUrl) + "/api/api/Patient/GetItemStock?institutionId=" + encode(institutionId) + "&itemId="
		        + encode(itemId);
	}
	
	static String normalize(String baseUrl) {
		if (baseUrl == null) {
			return "";
		}
		String trimmed = baseUrl.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}
	
	private static String encode(String value) {
		try {
			return URLEncoder.encode(value == null ? "" : value, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new EaptsException("UTF-8 is not available", e);
		}
	}
}
