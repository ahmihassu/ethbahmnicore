/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.registrationfee.util;

import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Concept;
import org.openmrs.module.ethbahmnicore.registrationfee.properties.RegistrationFeeProperties;

/**
 * Helpers to recognize Registration Fee / MRU billing writes that must remain allowed.
 */
public class RegistrationFeeAllowList {
	
	private RegistrationFeeProperties properties;
	
	public void setProperties(RegistrationFeeProperties properties) {
		this.properties = properties;
	}
	
	public boolean isRegistrationFeeOrder(Order order) {
		if (order == null) {
			return false;
		}
		OrderType orderType = order.getOrderType();
		if (orderType != null && StringUtils.isNotBlank(orderType.getName())
		        && orderType.getName().trim().equalsIgnoreCase(properties.getOrderTypeName())) {
			return true;
		}
		Concept concept = order.getConcept();
		if (concept == null) {
			return false;
		}
		Set<String> feeNames = properties.getProductNameSetLower();
		if (concept.getNames() != null) {
			for (org.openmrs.ConceptName conceptName : concept.getNames()) {
				if (conceptName != null && StringUtils.isNotBlank(conceptName.getName())
				        && feeNames.contains(conceptName.getName().toLowerCase(Locale.ENGLISH))) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isRegistrationFeeEncounter(Encounter encounter) {
		if (encounter == null) {
			return false;
		}
		EncounterType type = encounter.getEncounterType();
		if (type != null && StringUtils.isNotBlank(type.getName())) {
			String name = type.getName().trim().toLowerCase(Locale.ENGLISH);
			if (properties.getAllowedEncounterTypeNames().contains(name)) {
				return true;
			}
		}
		if (encounter.getOrders() == null || encounter.getOrders().isEmpty()) {
			return false;
		}
		for (Order order : encounter.getOrders()) {
			if (!isRegistrationFeeOrder(order)) {
				return false;
			}
		}
		return true;
	}
}
