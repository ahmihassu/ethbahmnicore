/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.deposit.contract;

public enum DepositEligibilityStatus {
	ENFORCEMENT_DISABLED, NOT_REQUIRED, CASH_DEPOSIT_OK, CASH_DEPOSIT_INSUFFICIENT, ODOO_NOT_CONFIGURED, ODOO_ERROR, PATIENT_NOT_FOUND, PARTNER_NOT_FOUND
}
