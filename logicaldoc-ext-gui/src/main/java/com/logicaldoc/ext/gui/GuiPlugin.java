package com.logicaldoc.ext.gui;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.logicaldoc.ext.gui.license.server.LicenseServiceImpl;
import com.logicaldoc.util.config.WebConfigurator;
import com.logicaldoc.util.plugin.LogicalDOCPlugin;

/**
 * Provides some initializations
 * 
 * @author Marco Meschieri - Logical Objects
 * @since 4.0
 */
public class GuiPlugin extends LogicalDOCPlugin {

	protected static Logger log = LoggerFactory.getLogger(GuiPlugin.class);

	@Override
	protected void install() throws Exception {
		super.install();

		File dest = new File(getPluginPath());
		dest = dest.getParentFile().getParentFile();

		WebConfigurator config = new WebConfigurator(dest.getPath() + "/web.xml");

//		config.addServlet("LicenseService", LicenseServiceImpl.class.getName());
//		config.writeXMLDoc();
//		config.addServletMapping("LicenseService", "/license/license");
//		config.writeXMLDoc();
//
//		config.addServletMapping("SettingService", "/license/setting");
//		config.writeXMLDoc();
//
//		config.addServletMapping("SecurityService", "/sessions/security");
//		config.writeXMLDoc();
//		
//		config.addServletMapping("InfoService", "/sessions/info");
//		config.writeXMLDoc();
//		config.addServletMapping("InfoService", "/mobile/info");
//		config.writeXMLDoc();
//		config.addServletMapping("InfoService", "/license/info");
//		config.writeXMLDoc();
//
//		config.addServletMapping("SecurityService", "/mobile/security");
//		config.writeXMLDoc();
//		
//		config.addServletMapping("SystemService", "/mobile/system");
//		config.writeXMLDoc();
//		
//		config.addServletMapping("DocumentService", "/mobile/document");
//		config.writeXMLDoc();
//		
//		config.addServletMapping("FolderService", "/mobile/folder");
//		config.writeXMLDoc();
//		
//		config.addServletMapping("SearchService", "/mobile/search");
//		config.writeXMLDoc();
		
		setRestartRequired();
	}
}