package com.logicaldoc.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.ext.tenant.TenantServiceImpl;
import com.logicaldoc.gui.common.client.beans.GUIInfo;
import com.logicaldoc.gui.common.client.beans.GUITenant;
import com.logicaldoc.web.service.InfoServiceImpl;

/**
 * Implementation of the InfoService
 * 
 * @author Marco Meschieri - Logical Objects
 * @since 6.0
 */
public class InfoServiceExt extends InfoServiceImpl {

	private static Logger log = LoggerFactory.getLogger(InfoServiceExt.class);

	private static final long serialVersionUID = 1L;

	/* Called when Frontend initializes */
	@Override
	public GUIInfo getInfo(String locale, String tenantName) {
		GUIInfo guiInfo = super.getInfo(locale, tenantName);
				
		try {
			TenantServiceImpl tenantServiceImpl = new TenantServiceImpl();
			GUITenant guiTenant = tenantServiceImpl.load(guiInfo.getTenant().getId());
			guiInfo.setTenant(guiTenant);
			guiInfo.setBranding(guiTenant.getBranding());
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new RuntimeException(t.getMessage(), t);
		}
		return guiInfo;
	}
}