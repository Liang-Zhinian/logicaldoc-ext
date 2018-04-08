package com.logicaldoc.ext.branding;

import com.logicaldoc.core.PersistentObjectDAO;

public abstract interface BrandingDAO extends PersistentObjectDAO<Branding> {
	public abstract Branding findByTenantId(long tenantId);
}
