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

public class OdooDepositPartner {
	
	private Integer id;
	
	private String paymentMethod;
	
	private Double ipdDepositBalance;
	
	private Double companyMinDeposit;
	
	private Double committedAmount;
	
	private Double availableBalance;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getPaymentMethod() {
		return paymentMethod;
	}
	
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	
	public Double getIpdDepositBalance() {
		return ipdDepositBalance;
	}
	
	public void setIpdDepositBalance(Double ipdDepositBalance) {
		this.ipdDepositBalance = ipdDepositBalance;
	}
	
	public Double getCompanyMinDeposit() {
		return companyMinDeposit;
	}
	
	public void setCompanyMinDeposit(Double companyMinDeposit) {
		this.companyMinDeposit = companyMinDeposit;
	}
	
	public Double getCommittedAmount() {
		return committedAmount;
	}
	
	public void setCommittedAmount(Double committedAmount) {
		this.committedAmount = committedAmount;
	}
	
	public Double getAvailableBalance() {
		return availableBalance;
	}
	
	public void setAvailableBalance(Double availableBalance) {
		this.availableBalance = availableBalance;
	}
}
