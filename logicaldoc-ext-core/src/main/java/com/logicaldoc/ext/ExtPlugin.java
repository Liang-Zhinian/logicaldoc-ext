package com.logicaldoc.ext;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.ext.tenant.TenantServiceImpl;
import com.logicaldoc.ext.tenant.TenantsDataServlet;
import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.config.WebConfigurator;
import com.logicaldoc.util.plugin.LogicalDOCPlugin;


/**
 * This class provides initializations needed by Enterprise-Plugin
 * 
 * @author Marco Meschieri - Logical Objects
 * @since 6.5.1
 */
public class ExtPlugin extends LogicalDOCPlugin {
	protected static Logger log = LoggerFactory.getLogger(ExtPlugin.class);

	@Override
	protected void install() throws Exception {
		super.install();

		// Register the needed servlets
		File dest = new File(getPluginPath());
		dest = dest.getParentFile().getParentFile();
		WebConfigurator config = new WebConfigurator(dest.getPath() + "/web.xml");
		
		config.addServlet("TenantService", TenantServiceImpl.class.getName());
		config.addServletMapping("TenantService", "/frontend/tenant");
		config.writeXMLDoc();
		
		config.addServlet("TenantsData", TenantsDataServlet.class.getName());
		config.addServletMapping("TenantsData", "/data/tenants.xml");
		config.writeXMLDoc();
		
		setRestartRequired();
	}
}
