/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.registrationfee.contract;

import java.util.Date;

/**
 * Structured eligibility result for UI gating and backend enforcement.
 */
public class RegistrationFeeEligibility {
	
	private boolean allowed;
	
	private EligibilityStatus status;
	
	private String reason;
	
	private String paymentMethod;
	
	private Integer invoiceId;
	
	private String invoiceState;
	
	private int windowDays;
	
	private Date windowStart;
	
	private Date windowEnd;
	
	private boolean enforceRegistrationFee;
	
	private String patientUuid;
	
	private String identifier;
	
	public static RegistrationFeeEligibility of(boolean allowed, EligibilityStatus status, String reason) {
		RegistrationFeeEligibility result = new RegistrationFeeEligibility();
		result.allowed = allowed;
		result.status = status;
		result.reason = reason;
		return result;
	}
	
	public boolean isAllowed() {
		return allowed;
	}
	
	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}
	
	public EligibilityStatus getStatus() {
		return status;
	}
	
	public void setStatus(EligibilityStatus status) {
		this.status = status;
	}
	
	public String getReason() {
		return reason;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public String getPaymentMethod() {
		return paymentMethod;
	}
	
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	
	public Integer getInvoiceId() {
		return invoiceId;
	}
	
	public void setInvoiceId(Integer invoiceId) {
		this.invoiceId = invoiceId;
	}
	
	public String getInvoiceState() {
		return invoiceState;
	}
	
	public void setInvoiceState(String invoiceState) {
		this.invoiceState = invoiceState;
	}
	
	public int getWindowDays() {
		return windowDays;
	}
	
	public void setWindowDays(int windowDays) {
		this.windowDays = windowDays;
	}
	
	public Date getWindowStart() {
		return windowStart;
	}
	
	public void setWindowStart(Date windowStart) {
		this.windowStart = windowStart;
	}
	
	public Date getWindowEnd() {
		return windowEnd;
	}
	
	public void setWindowEnd(Date windowEnd) {
		this.windowEnd = windowEnd;
	}
	
	public boolean isEnforceRegistrationFee() {
		return enforceRegistrationFee;
	}
	
	public void setEnforceRegistrationFee(boolean enforceRegistrationFee) {
		this.enforceRegistrationFee = enforceRegistrationFee;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
