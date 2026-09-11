/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore;

import org.openmrs.BaseOpenmrsData;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A node in the CBHI Region → Zone → Woreda hierarchy. Values are looked up via REST and stored on
 * the patient as person attributes (not person_address / Address Hierarchy).
 */
@Entity(name = "ethbahmnicore.CbhiLocation")
@Table(name = "ethbahmnicore_cbhi_location")
public class CbhiLocation extends BaseOpenmrsData {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cbhi_location_id")
	private Integer cbhiLocationId;
	
	@Basic
	@Column(name = "name", nullable = false, length = 255)
	private String name;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "level", nullable = false, length = 50)
	private CbhiLocationLevel level;
	
	@ManyToOne
	@JoinColumn(name = "parent_id")
	private CbhiLocation parent;
	
	@Override
	public Integer getId() {
		return cbhiLocationId;
	}
	
	@Override
	public void setId(Integer id) {
		this.cbhiLocationId = id;
	}
	
	public Integer getCbhiLocationId() {
		return cbhiLocationId;
	}
	
	public void setCbhiLocationId(Integer cbhiLocationId) {
		this.cbhiLocationId = cbhiLocationId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public CbhiLocationLevel getLevel() {
		return level;
	}
	
	public void setLevel(CbhiLocationLevel level) {
		this.level = level;
	}
	
	public CbhiLocation getParent() {
		return parent;
	}
	
	public void setParent(CbhiLocation parent) {
		this.parent = parent;
	}
}
