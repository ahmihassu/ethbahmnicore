/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.deposit.odoo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ethbahmnicore.registrationfee.odoo.OdooClient;

/**
 * Reads IPD deposit balance and product prices from Odoo.
 */
public class OdooDepositRepository {
	
	private static final Log log = LogFactory.getLog(OdooDepositRepository.class);
	
	private static final List<String> PARTNER_FIELDS = Arrays.asList("id", "payment_method", "ipd_deposit_balance");
	
	private OdooClient odooClient;
	
	public void setOdooClient(OdooClient odooClient) {
		this.odooClient = odooClient;
	}
	
	public OdooDepositPartner findPartnerDeposit(String patientUuid, String patientIdentifier) {
		List<Integer> partnerIds = findPartnerIds(patientUuid, patientIdentifier);
		if (partnerIds.isEmpty()) {
			return null;
		}
		List<Object> domain = new ArrayList<Object>();
		domain.add(Arrays.asList("id", "in", partnerIds));
		List<Object> rows = odooClient.searchRead("res.partner", domain, PARTNER_FIELDS, "id desc", 1);
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		OdooDepositPartner partner = toPartner(rows.get(0));
		if (partner != null) {
			partner.setCompanyMinDeposit(findCompanyMinDeposit());
			double committed = findCommittedIpdAmount(partner.getId());
			partner.setCommittedAmount(committed);
			double balance = partner.getIpdDepositBalance() == null ? 0.0 : partner.getIpdDepositBalance();
			partner.setAvailableBalance(Math.max(balance - committed, 0.0));
		}
		return partner;
	}
	
	/**
	 * Sum of open IPD commitments (draft/sent totals + unpaid confirmed residuals). Prefers Odoo
	 * partner method so OpenMRS and cashiers share one formula.
	 */
	public double findCommittedIpdAmount(Integer partnerId) {
		if (partnerId == null) {
			return 0.0;
		}
		try {
			Object result = odooClient.execute("res.partner", "bahmni_ipd_deposit_committed",
			    Arrays.<Object> asList(Collections.singletonList(partnerId)));
			Double value = asDouble(result);
			if (value != null) {
				return value;
			}
		}
		catch (RuntimeException e) {
			log.warn("Odoo bahmni_ipd_deposit_committed failed; falling back to search_read", e);
		}
		return findCommittedIpdAmountFallback(partnerId);
	}
	
	private double findCommittedIpdAmountFallback(Integer partnerId) {
		List<Object> domain = new ArrayList<Object>();
		domain.add(Arrays.asList("partner_id", "=", partnerId));
		domain.add(Arrays.asList("care_setting", "=", "ipd"));
		domain.add(Arrays.asList("state", "in", Arrays.asList("draft", "sent", "sale")));
		List<Object> rows = odooClient.searchRead("sale.order", domain,
		    Arrays.asList("id", "amount_total", "state", "invoice_ids"), null, 200);
		if (rows == null || rows.isEmpty()) {
			return 0.0;
		}
		double committed = 0.0;
		for (Object row : rows) {
			if (!(row instanceof Map)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) row;
			String state = map.get("state") == null ? "" : String.valueOf(map.get("state"));
			Double amountTotal = asDouble(map.get("amount_total"));
			if (amountTotal == null) {
				amountTotal = 0.0;
			}
			if ("draft".equals(state) || "sent".equals(state)) {
				committed += amountTotal;
				continue;
			}
			List<Integer> invoiceIds = asIdList(map.get("invoice_ids"));
			if (invoiceIds.isEmpty()) {
				committed += amountTotal;
				continue;
			}
			committed += sumInvoiceResiduals(invoiceIds);
		}
		return committed;
	}
	
	private double sumInvoiceResiduals(List<Integer> invoiceIds) {
		if (invoiceIds.isEmpty()) {
			return 0.0;
		}
		List<Object> domain = new ArrayList<Object>();
		domain.add(Arrays.asList("id", "in", invoiceIds));
		domain.add(Arrays.asList("state", "not in", Arrays.asList("cancel")));
		List<Object> rows = odooClient.searchRead("account.invoice", domain, Arrays.asList("id", "residual", "state"), null,
		    invoiceIds.size());
		if (rows == null || rows.isEmpty()) {
			return 0.0;
		}
		double residual = 0.0;
		for (Object row : rows) {
			if (!(row instanceof Map)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) row;
			Double value = asDouble(map.get("residual"));
			if (value != null && value > 0) {
				residual += value;
			}
		}
		return residual;
	}
	
	private static List<Integer> asIdList(Object value) {
		List<Integer> ids = new ArrayList<Integer>();
		if (value instanceof Object[]) {
			value = Arrays.asList((Object[]) value);
		}
		if (!(value instanceof List)) {
			return ids;
		}
		for (Object item : (List<?>) value) {
			if (item instanceof Integer) {
				ids.add((Integer) item);
			} else if (item instanceof Number) {
				ids.add(((Number) item).intValue());
			}
		}
		return ids;
	}
	
	/**
	 * True when this OpenMRS order uuid already exists as a sale.order.line.external_order_id in
	 * Odoo (synced — do not reserve deposit again for sibling checks).
	 */
	public boolean isOrderSyncedToOdoo(String orderUuid) {
		if (StringUtils.isBlank(orderUuid)) {
			return false;
		}
		List<Object> domain = new ArrayList<Object>();
		domain.add(Arrays.asList("external_order_id", "=", orderUuid.trim()));
		List<Object> rows = odooClient.searchRead("sale.order.line", domain, Arrays.asList("id"), null, 1);
		return rows != null && !rows.isEmpty();
	}
	
	/**
	 * Looks up list price for a product matched by OpenMRS concept / product uuid.
	 */
	public Double findProductListPrice(String conceptUuid) {
		if (StringUtils.isBlank(conceptUuid)) {
			return null;
		}
		List<Object> domain = new ArrayList<Object>();
		domain.add(Arrays.asList("uuid", "=", conceptUuid));
		List<Object> rows = odooClient.searchRead("product.product", domain, Arrays.asList("id", "list_price", "lst_price"),
		    null, 1);
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		Object row = rows.get(0);
		if (!(row instanceof Map)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) row;
		Double price = asDouble(map.get("lst_price"));
		if (price == null) {
			price = asDouble(map.get("list_price"));
		}
		return price;
	}
	
	private Double findCompanyMinDeposit() {
		List<Object> rows = odooClient.searchRead("res.company", Collections.<Object> emptyList(),
		    Arrays.asList("id", "ipd_min_deposit_amount"), "id asc", 1);
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		Object row = rows.get(0);
		if (!(row instanceof Map)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) row;
		return asDouble(map.get("ipd_min_deposit_amount"));
	}
	
	private List<Integer> findPartnerIds(String patientUuid, String patientIdentifier) {
		List<Integer> ids = new ArrayList<Integer>();
		if (StringUtils.isNotBlank(patientUuid)) {
			ids.addAll(searchPartnerIds(Arrays.<Object> asList(Arrays.asList("uuid", "=", patientUuid))));
		}
		if (ids.isEmpty() && StringUtils.isNotBlank(patientIdentifier)) {
			ids.addAll(searchPartnerIds(Arrays.<Object> asList(Arrays.asList("ref", "=", patientIdentifier))));
		}
		return ids;
	}
	
	private List<Integer> searchPartnerIds(List<Object> domain) {
		List<Object> rows = odooClient.searchRead("res.partner", domain, Arrays.asList("id"), null, 5);
		List<Integer> ids = new ArrayList<Integer>();
		if (rows == null) {
			return ids;
		}
		for (Object row : rows) {
			if (row instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) row;
				Object id = map.get("id");
				if (id instanceof Integer) {
					ids.add((Integer) id);
				} else if (id instanceof Number) {
					ids.add(((Number) id).intValue());
				}
			}
		}
		return ids;
	}
	
	private OdooDepositPartner toPartner(Object row) {
		if (!(row instanceof Map)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) row;
		OdooDepositPartner partner = new OdooDepositPartner();
		Object id = map.get("id");
		if (id instanceof Number) {
			partner.setId(((Number) id).intValue());
		}
		Object method = map.get("payment_method");
		if (method != null && !(method instanceof Boolean)) {
			partner.setPaymentMethod(String.valueOf(method));
		}
		partner.setIpdDepositBalance(asDouble(map.get("ipd_deposit_balance")));
		return partner;
	}
	
	private static Double asDouble(Object value) {
		if (value == null || value instanceof Boolean) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(value));
		}
		catch (NumberFormatException e) {
			log.debug("Could not parse double: " + value);
			return null;
		}
	}
}
