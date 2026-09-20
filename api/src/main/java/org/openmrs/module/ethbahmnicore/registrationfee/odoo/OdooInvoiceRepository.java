/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.registrationfee.odoo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ethbahmnicore.registrationfee.properties.RegistrationFeeProperties;

/**
 * Looks up current-window MRU / Registration Fee invoices in Odoo.
 */
public class OdooInvoiceRepository {
	
	private static final Log log = LogFactory.getLog(OdooInvoiceRepository.class);
	
	private static final List<String> INVOICE_FIELDS = Arrays.asList("id", "state", "payment_method", "date_invoice",
	    "number");
	
	private OdooClient odooClient;
	
	private RegistrationFeeProperties properties;
	
	public void setOdooClient(OdooClient odooClient) {
		this.odooClient = odooClient;
	}
	
	public void setProperties(RegistrationFeeProperties properties) {
		this.properties = properties;
	}
	
	/**
	 * Returns the newest in-window MRU invoice for the patient, or null if none.
	 */
	public OdooInvoice findLatestMruInvoice(String patientUuid, String patientIdentifier, Date windowStart, Date windowEnd) {
		List<Integer> partnerIds = findPartnerIds(patientUuid, patientIdentifier);
		if (partnerIds.isEmpty()) {
			return null;
		}
		return findLatest(partnerIds, windowStart, windowEnd);
	}
	
	/**
	 * Newest MRU invoice with no date filter (used to distinguish outside-window vs never billed).
	 */
	public OdooInvoice findLatestMruInvoiceAnyTime(String patientUuid, String patientIdentifier) {
		List<Integer> partnerIds = findPartnerIds(patientUuid, patientIdentifier);
		if (partnerIds.isEmpty()) {
			return null;
		}
		return findLatest(partnerIds, null, null);
	}
	
	private OdooInvoice findLatest(List<Integer> partnerIds, Date windowStart, Date windowEnd) {
		String shopName = properties.getShopName();
		
		List<Object> domain = new ArrayList<Object>();
		domain.add(Arrays.asList("type", "=", "out_invoice"));
		domain.add(Arrays.asList("state", "!=", "cancel"));
		domain.add(Arrays.asList("shop_id.name", "=", shopName));
		if (windowStart != null && windowEnd != null) {
			domain.add(Arrays.asList("date_invoice", ">=", formatDate(windowStart)));
			domain.add(Arrays.asList("date_invoice", "<=", formatDate(windowEnd)));
		}
		domain.add("|");
		domain.add(Arrays.asList("partner_id", "in", partnerIds));
		domain.add(Arrays.asList("patient_partner_id", "in", partnerIds));
		
		List<Object> rows = odooClient.searchRead("account.invoice", domain, INVOICE_FIELDS, "date_invoice desc, id desc",
		    10);
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		
		for (Object row : rows) {
			OdooInvoice invoice = toInvoice(row);
			if (invoice != null) {
				return invoice;
			}
		}
		return null;
	}
	
	private List<Integer> findPartnerIds(String patientUuid, String patientIdentifier) {
		List<Integer> ids = new ArrayList<Integer>();
		if (StringUtils.isNotBlank(patientUuid)) {
			ids.addAll(searchPartnerIds(Arrays.<Object> asList(Arrays.asList("uuid", "=", patientUuid.trim()))));
		}
		if (ids.isEmpty() && StringUtils.isNotBlank(patientIdentifier)) {
			ids.addAll(searchPartnerIds(Arrays.<Object> asList(Arrays.asList("ref", "=", patientIdentifier.trim()))));
		}
		return ids;
	}
	
	private List<Integer> searchPartnerIds(List<Object> domain) {
		List<Object> rows = odooClient.searchRead("res.partner", domain, Collections.singletonList("id"), null, 5);
		List<Integer> ids = new ArrayList<Integer>();
		if (rows == null) {
			return ids;
		}
		for (Object row : rows) {
			Integer id = extractInt(row, "id");
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}
	
	@SuppressWarnings("unchecked")
	private OdooInvoice toInvoice(Object row) {
		if (!(row instanceof Map)) {
			log.warn("Unexpected invoice row type: " + (row == null ? "null" : row.getClass()));
			return null;
		}
		Map<String, Object> map = (Map<String, Object>) row;
		OdooInvoice invoice = new OdooInvoice();
		invoice.setId(extractInt(map, "id"));
		invoice.setState(asString(map.get("state")));
		invoice.setPaymentMethod(asString(map.get("payment_method")));
		invoice.setNumber(asString(map.get("number")));
		invoice.setDateInvoice(parseDate(asString(map.get("date_invoice"))));
		return invoice;
	}
	
	@SuppressWarnings("unchecked")
	private Integer extractInt(Object row, String field) {
		if (!(row instanceof Map)) {
			return null;
		}
		Object value = ((Map<String, Object>) row).get(field);
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return null;
	}
	
	private static String asString(Object value) {
		if (value == null || Boolean.FALSE.equals(value)) {
			return null;
		}
		return String.valueOf(value).trim();
	}
	
	private static String formatDate(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}
	
	private static Date parseDate(String raw) {
		if (StringUtils.isBlank(raw)) {
			return null;
		}
		try {
			return new SimpleDateFormat("yyyy-MM-dd").parse(raw.trim());
		}
		catch (ParseException e) {
			return null;
		}
	}
}
