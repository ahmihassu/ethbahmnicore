/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ethbahmnicore.cbhi.dao;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ethbahmnicore.cbhi.model.CbhiLocation;
import org.openmrs.module.ethbahmnicore.cbhi.model.CbhiLocationLevel;

public class CbhiLocationDao {
	
	private DbSessionFactory sessionFactory;
	
	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	public CbhiLocation getByUuid(String uuid) {
		return (CbhiLocation) getSession().createCriteria(CbhiLocation.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public CbhiLocation getById(Integer id) {
		return (CbhiLocation) getSession().get(CbhiLocation.class, id);
	}
	
	public CbhiLocation save(CbhiLocation location) {
		getSession().saveOrUpdate(location);
		return location;
	}
	
	public void deleteAll() {
		// Delete leaves first to satisfy self-FK on parent_id.
		getSession().createQuery("delete from ethbahmnicore.CbhiLocation where level = :level")
		        .setParameter("level", CbhiLocationLevel.WOREDA).executeUpdate();
		getSession().createQuery("delete from ethbahmnicore.CbhiLocation where level = :level")
		        .setParameter("level", CbhiLocationLevel.ZONE).executeUpdate();
		getSession().createQuery("delete from ethbahmnicore.CbhiLocation where level = :level")
		        .setParameter("level", CbhiLocationLevel.REGION).executeUpdate();
	}
	
	public long getCount() {
		Number count = (Number) getSession().createCriteria(CbhiLocation.class).setProjection(Projections.rowCount())
		        .uniqueResult();
		return count == null ? 0L : count.longValue();
	}
	
	/**
	 * Finds non-voided locations. When parentUuid is null, only roots (regions) are returned unless
	 * a level filter is applied without parent constraint for free-text search.
	 */
	@SuppressWarnings("unchecked")
	public List<CbhiLocation> find(String parentUuid, CbhiLocationLevel level, String q, boolean rootsOnlyWhenNoParent) {
		Criteria criteria = getSession().createCriteria(CbhiLocation.class);
		criteria.add(Restrictions.eq("voided", false));
		
		if (StringUtils.isNotBlank(parentUuid)) {
			criteria.createAlias("parent", "p");
			criteria.add(Restrictions.eq("p.uuid", parentUuid));
		} else if (rootsOnlyWhenNoParent) {
			criteria.add(Restrictions.isNull("parent"));
		}
		
		if (level != null) {
			criteria.add(Restrictions.eq("level", level));
		}
		
		if (StringUtils.isNotBlank(q)) {
			criteria.add(Restrictions.ilike("name", q.trim(), MatchMode.ANYWHERE));
		}
		
		criteria.addOrder(Order.asc("name"));
		return criteria.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<CbhiLocation> findByParentAndName(CbhiLocation parent, String name, CbhiLocationLevel level) {
		Criteria criteria = getSession().createCriteria(CbhiLocation.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.add(Restrictions.eq("level", level));
		criteria.add(Restrictions.eq("name", name).ignoreCase());
		if (parent == null) {
			criteria.add(Restrictions.isNull("parent"));
		} else {
			criteria.add(Restrictions.eq("parent", parent));
		}
		return criteria.list();
	}
}
