package com.logicaldoc.ext.branding;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.logicaldoc.core.HibernatePersistentObjectDAO;
import com.logicaldoc.core.document.dao.HibernateDocumentDAO;

public class HibernateBrandingDAO extends HibernatePersistentObjectDAO<Branding> implements BrandingDAO {

	protected HibernateBrandingDAO() {
		super(Branding.class);
		super.log = LoggerFactory.getLogger(HibernateBrandingDAO.class);
	}

	@Override
	public Branding findByTenantId(long tenantId) {
		List<Branding> list = findByWhere("tenantId="+tenantId, null, null);
		if (list != null && !list.isEmpty()) {
			return (Branding)list.get(0);
		}
		return null;
	}

}
