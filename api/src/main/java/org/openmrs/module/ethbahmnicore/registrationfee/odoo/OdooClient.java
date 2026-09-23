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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.openmrs.module.ethbahmnicore.registrationfee.properties.RegistrationFeeProperties;

/**
 * Lightweight Odoo XML-RPC client (same transport style as bahmni-erp-connect). Credentials come
 * from environment variables via {@link RegistrationFeeProperties}.
 */
public class OdooClient {
	
	private static final Log log = LogFactory.getLog(OdooClient.class);
	
	private static final String COMMON = "/xmlrpc/2/common";
	
	private static final String OBJECT = "/xmlrpc/2/object";
	
	private static final int CONNECT_TIMEOUT_MS = 5000;
	
	private static final int REPLY_TIMEOUT_MS = 20000;
	
	private RegistrationFeeProperties properties;
	
	private Integer uid;
	
	private XmlRpcClient xmlRpcClient;
	
	public void setProperties(RegistrationFeeProperties properties) {
		this.properties = properties;
	}
	
	public synchronized void resetSession() {
		uid = null;
	}
	
	@SuppressWarnings("unchecked")
	public List<Object> searchRead(String model, List<Object> domain, List<String> fields, String order, int limit) {
		login();
		Map<String, Object> kwargs = new HashMap<String, Object>();
		kwargs.put("fields", fields);
		if (order != null) {
			kwargs.put("order", order);
		}
		if (limit > 0) {
			kwargs.put("limit", limit);
		}
		Object result = executeKw(model, "search_read", Arrays.<Object> asList(domain), kwargs);
		if (result instanceof Object[]) {
			return Arrays.asList((Object[]) result);
		}
		if (result instanceof List) {
			return (List<Object>) result;
		}
		throw new OdooException("Unexpected search_read result type: " + (result == null ? "null" : result.getClass()));
	}
	
	/**
	 * Call a model method via execute_kw (e.g. res.partner.bahmni_ipd_deposit_available).
	 */
	public Object execute(String model, String method, List<Object> args) {
		login();
		return executeKw(model, method, args, new HashMap<String, Object>());
	}
	
	private void login() {
		if (uid != null) {
			return;
		}
		if (!properties.isOdooConfigured()) {
			throw new OdooException("Odoo credentials are not configured (set " + RegistrationFeeProperties.ENV_ODOO_USER
			        + " and " + RegistrationFeeProperties.ENV_ODOO_PASSWORD + ")");
		}
		try {
			XmlRpcClient client = clientFor(COMMON);
			Object loginId = client.execute("authenticate",
			    new Object[] { properties.getOdooDatabase(), properties.getOdooUser(), properties.getOdooPassword(),
			            new HashMap<String, Object>() });
			if (!(loginId instanceof Integer) || ((Integer) loginId) <= 0) {
				throw new OdooException("Odoo authenticate failed for user " + properties.getOdooUser());
			}
			uid = (Integer) loginId;
			log.debug("Authenticated to Odoo as uid=" + uid);
		}
		catch (XmlRpcException e) {
			throw new OdooException("Odoo authenticate XML-RPC error", e);
		}
	}
	
	private Object executeKw(String model, String method, List<Object> args, Map<String, Object> kwargs) {
		try {
			XmlRpcClient client = clientFor(OBJECT);
			return client
			        .execute("execute_kw", new Object[] { properties.getOdooDatabase(), uid, properties.getOdooPassword(),
			                model, method, args, kwargs });
		}
		catch (XmlRpcException e) {
			uid = null;
			throw new OdooException("Odoo execute_kw failed for " + model + "." + method, e);
		}
	}
	
	private XmlRpcClient clientFor(String endpoint) {
		if (xmlRpcClient == null) {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setEnabledForExtensions(true);
			config.setConnectionTimeout(CONNECT_TIMEOUT_MS);
			config.setReplyTimeout(REPLY_TIMEOUT_MS);
			xmlRpcClient = new XmlRpcClient();
			xmlRpcClient.setTransportFactory(new XmlRpcSun15HttpTransportFactory(xmlRpcClient));
			xmlRpcClient.setConfig(config);
		}
		XmlRpcClientConfigImpl config = (XmlRpcClientConfigImpl) xmlRpcClient.getClientConfig();
		try {
			config.setServerURL(new URL("http", properties.getOdooHost(), properties.getOdooPort(), endpoint));
		}
		catch (MalformedURLException e) {
			throw new OdooException("Invalid Odoo URL", e);
		}
		return xmlRpcClient;
	}
}
