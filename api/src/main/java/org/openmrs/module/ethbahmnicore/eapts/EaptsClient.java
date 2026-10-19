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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Server-side calls to eAPTS. The medication page used to call these HTTP URLs from the browser,
 * which forced the consultation onto plain HTTP.
 */
public class EaptsClient {
	
	private static final Log log = LogFactory.getLog(EaptsClient.class);
	
	private static final int CONNECT_TIMEOUT_MS = 10000;
	
	/** Same budget the stock mediator uses. eAPTS stock responses can be slow. */
	private static final int READ_TIMEOUT_MS = 90000;
	
	private EaptsProperties properties;
	
	public void setProperties(EaptsProperties properties) {
		this.properties = properties;
	}
	
	public List<Map<String, Object>> getDispensingUnits() {
		requireConfigured();
		String body = get(EaptsUrls.dispensingUnits(properties.getBaseUrl()));
		return EaptsResponseMapper.dispensingUnits(body);
	}
	
	public List<Map<String, Object>> getItemStock(String institutionId) {
		requireConfigured();
		if (StringUtils.isBlank(institutionId)) {
			throw new EaptsException("institutionId is required");
		}
		String body = get(EaptsUrls.itemStock(properties.getBaseUrl(), institutionId.trim()));
		return EaptsResponseMapper.itemStock(body);
	}
	
	public Map<String, Object> getItem(String institutionId, String itemId) {
		requireConfigured();
		if (StringUtils.isBlank(institutionId) || StringUtils.isBlank(itemId)) {
			throw new EaptsException("institutionId and itemId are required");
		}
		String body = get(EaptsUrls.itemStockForDrug(properties.getBaseUrl(), institutionId.trim(), itemId.trim()));
		return EaptsResponseMapper.singleItem(body, itemId.trim());
	}
	
	private void requireConfigured() {
		if (properties == null || !properties.isConfigured()) {
			throw new EaptsException(
			        "eAPTS is not configured. Set ETHBAHMNICORE_EAPTS_USER and ETHBAHMNICORE_EAPTS_PASSWORD.");
		}
	}
	
	private String get(String url) {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
			connection.setReadTimeout(READ_TIMEOUT_MS);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Authorization", basicAuthHeader());
			
			int code = connection.getResponseCode();
			InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
			String body = stream == null ? "" : IOUtils.toString(stream, "UTF-8");
			if (code < 200 || code >= 300) {
				log.error("eAPTS HTTP " + code + " for " + url + " body=" + abbreviate(body));
				throw new EaptsException("eAPTS returned HTTP " + code);
			}
			return body;
		}
		catch (EaptsException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("eAPTS request failed for " + url, e);
			throw new EaptsException("Could not reach eAPTS", e);
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	private String basicAuthHeader() {
		String token = properties.getUsername() + ":" + properties.getPassword();
		String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}
	
	private static String abbreviate(String body) {
		if (body == null) {
			return "";
		}
		String trimmed = body.replaceAll("\\s+", " ").trim();
		return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300);
	}
}
