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

public class DepositEligibility {
	
	private boolean allowed;
	
	private boolean depositRequired;
	
	private DepositEligibilityStatus status;
	
	private String reason;
	
	private String paymentMethod;
	
	private Double balance;
	
	private Double availableBalance;
	
	private Double committedAmount;
	
	private Double requiredAmount;
	
	private Double shortfall;
	
	private Double minDeposit;
	
	private boolean enforceIpdDeposit;
	
	private boolean currentlyIpd;
	
	private String patientUuid;
	
	private String identifier;
	
	public boolean isAllowed() {
		return allowed;
	}
	
	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}
	
	public boolean isDepositRequired() {
		return depositRequired;
	}
	
	public void setDepositRequired(boolean depositRequired) {
		this.depositRequired = depositRequired;
	}
	
	public DepositEligibilityStatus getStatus() {
		return status;
	}
	
	public void setStatus(DepositEligibilityStatus status) {
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
	
	public Double getBalance() {
		return balance;
	}
	
	public void setBalance(Double balance) {
		this.balance = balance;
	}
	
	public Double getAvailableBalance() {
		return availableBalance;
	}
	
	public void setAvailableBalance(Double availableBalance) {
		this.availableBalance = availableBalance;
	}
	
	public Double getCommittedAmount() {
		return committedAmount;
	}
	
	public void setCommittedAmount(Double committedAmount) {
		this.committedAmount = committedAmount;
	}
	
	public Double getRequiredAmount() {
		return requiredAmount;
	}
	
	public void setRequiredAmount(Double requiredAmount) {
		this.requiredAmount = requiredAmount;
	}
	
	public Double getShortfall() {
		return shortfall;
	}
	
	public void setShortfall(Double shortfall) {
		this.shortfall = shortfall;
	}
	
	public Double getMinDeposit() {
		return minDeposit;
	}
	
	public void setMinDeposit(Double minDeposit) {
		this.minDeposit = minDeposit;
	}
	
	public boolean isEnforceIpdDeposit() {
		return enforceIpdDeposit;
	}
	
	public void setEnforceIpdDeposit(boolean enforceIpdDeposit) {
		this.enforceIpdDeposit = enforceIpdDeposit;
	}
	
	public boolean isCurrentlyIpd() {
		return currentlyIpd;
	}
	
	public void setCurrentlyIpd(boolean currentlyIpd) {
		this.currentlyIpd = currentlyIpd;
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
