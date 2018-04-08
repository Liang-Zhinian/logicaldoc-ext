package com.logicaldoc.ext.branding;

import java.util.List;

import com.logicaldoc.core.HibernatePersistentObjectDAO;

public class HibernateBrandingDAO extends HibernatePersistentObjectDAO<Branding> implements BrandingDAO {

	protected HibernateBrandingDAO() {
		super(Branding.class);
	}

	@Override
	public Branding findByTenantId(long tenantId) {
		List list = findByWhere("tenantId="+tenantId, null, null);
		if (list != null && !list.isEmpty()) {
			return (Branding)list.get(0);
		}
		return null;
	}

}
