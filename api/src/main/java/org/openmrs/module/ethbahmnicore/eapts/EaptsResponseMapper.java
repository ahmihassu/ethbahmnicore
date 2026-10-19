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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Turns eAPTS {@code model: ... } payloads into the shape the medication page already reads:
 * dispensing units use {@code rowGuid}, and stock rows use {@code itemUnit}, {@code hasStock}, and
 * {@code quantity}.
 */
public final class EaptsResponseMapper {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private EaptsResponseMapper() {
	}
	
	public static List<Map<String, Object>> dispensingUnits(String json) {
		List<Map<String, Object>> units = new ArrayList<Map<String, Object>>();
		for (JsonNode record : records(modelNode(json))) {
			Map<String, Object> unit = new LinkedHashMap<String, Object>();
			unit.put("institutionName", text(record, "institutionName"));
			unit.put("rowGuid", firstText(record, "rowGuid", "rowguid"));
			units.add(unit);
		}
		return units;
	}
	
	public static List<Map<String, Object>> itemStock(String json) {
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		for (JsonNode record : records(modelNode(json))) {
			items.add(toItem(record));
		}
		return items;
	}
	
	/**
	 * One drug's stock. eAPTS sometimes returns a single object and sometimes a one-element list.
	 * The medication page reads {@code hasStock} and {@code quantity} on the object itself.
	 */
	public static Map<String, Object> singleItem(String json, String itemId) {
		JsonNode model = modelNode(json);
		if (model == null || model.isNull()) {
			return null;
		}
		if (model.isObject() && isRecord(model)) {
			return toItem(model);
		}
		List<JsonNode> rows = records(model);
		if (rows.isEmpty()) {
			return null;
		}
		if (itemId != null) {
			for (JsonNode row : rows) {
				if (itemId.equals(text(row, "itemUnit"))) {
					return toItem(row);
				}
			}
		}
		return toItem(rows.get(0));
	}
	
	private static Map<String, Object> toItem(JsonNode record) {
		Map<String, Object> item = new LinkedHashMap<String, Object>();
		item.put("itemUnit", text(record, "itemUnit"));
		item.put("institution", text(record, "institution"));
		item.put("hasStock", bool(record, "hasStock"));
		item.put("quantity", number(record, "quantity"));
		return item;
	}
	
	private static JsonNode modelNode(String json) {
		if (json == null || json.trim().isEmpty()) {
			throw new EaptsException("eAPTS returned an empty response");
		}
		try {
			JsonNode root = MAPPER.readTree(json);
			if (root == null || root.isNull()) {
				throw new EaptsException("eAPTS returned an empty response");
			}
			if (root.isObject() && root.has("model")) {
				return root.get("model");
			}
			return root;
		}
		catch (EaptsException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EaptsException("eAPTS returned a response that is not JSON", e);
		}
	}
	
	private static List<JsonNode> records(JsonNode model) {
		if (model == null || model.isNull()) {
			return Collections.emptyList();
		}
		if (model.isArray()) {
			List<JsonNode> list = new ArrayList<JsonNode>();
			for (JsonNode child : model) {
				if (child != null && child.isObject()) {
					list.add(child);
				}
			}
			return list;
		}
		if (isRecord(model)) {
			return Collections.singletonList(model);
		}
		if (model.isObject()) {
			List<JsonNode> list = new ArrayList<JsonNode>();
			Iterator<JsonNode> children = model.getElements();
			while (children.hasNext()) {
				JsonNode child = children.next();
				if (child != null && child.isObject()) {
					list.add(child);
				}
			}
			return list;
		}
		return Collections.emptyList();
	}
	
	private static boolean isRecord(JsonNode node) {
		return node != null
		        && node.isObject()
		        && (node.has("institutionName") || node.has("rowguid") || node.has("rowGuid") || node.has("itemUnit") || node
		                .has("hasStock"));
	}
	
	private static String firstText(JsonNode node, String... fields) {
		for (String field : fields) {
			String value = text(node, field);
			if (value != null) {
				return value;
			}
		}
		return null;
	}
	
	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		if (value.isTextual()) {
			return value.getTextValue();
		}
		return value.getValueAsText();
	}
	
	private static Boolean bool(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return Boolean.FALSE;
		}
		if (value.isBoolean()) {
			return value.getBooleanValue();
		}
		if (value.isNumber()) {
			return value.getIntValue() != 0;
		}
		if (value.isTextual()) {
			String text = value.getTextValue();
			return "true".equalsIgnoreCase(text) || "1".equals(text);
		}
		return Boolean.FALSE;
	}
	
	private static Number number(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || !value.isNumber()) {
			return Integer.valueOf(0);
		}
		double asDouble = value.getDoubleValue();
		if (asDouble == Math.rint(asDouble) && !Double.isInfinite(asDouble)) {
			return Long.valueOf((long) asDouble);
		}
		return Double.valueOf(asDouble);
	}
}
