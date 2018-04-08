package com.logicaldoc.ext.tenant;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.core.security.Tenant;
import com.logicaldoc.core.security.dao.TenantDAO;
import com.logicaldoc.util.Context;

/**
 * This servlet is responsible for tenants data.
 * 
 * @author Matteo Caruso - Logical Objects
 * @since 6.0
 */
public class TenantsDataServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger log = LoggerFactory.getLogger(TenantsDataServlet.class);

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		try {
			response.setContentType("text/xml");
			response.setCharacterEncoding("UTF-8");

			// Avoid resource caching
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Cache-Control", "no-store");
			response.setDateHeader("Expires", 0);

			PrintWriter writer = response.getWriter();
			writer.print("<list>");

			//if (!required)
			//	writer.print("<user><id></id><username></username><name></name></user>");

			List<Tenant> tenants = new ArrayList<Tenant>();

			TenantDAO tenantDao = (TenantDAO) Context.get().getBean(TenantDAO.class);
			
			tenants = tenantDao.findAll();
			
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			df.setTimeZone(TimeZone.getTimeZone("UTC"));

			/*
			 * Iterate over records composing the response XML document
			 */
			for (Tenant tenant : tenants) {
				/*if (user.getType() != User.TYPE_DEFAULT)
					continue;*/

				tenantDao.initialize(tenant);

				// id, enabled, name, displayName, expire, 
				// email, telephone, country, city, address
				// id, name, enabledIcon, enabled, expire, displayName, email, city, country, telephone, postalCode,
				// state, address
				writer.print("<tenant>");
				writer.print("<id>" + tenant.getId() + "</id>");
				writer.print("<displayName><![CDATA[" + tenant.getDisplayName() + "]]></displayName>");
				if (tenant.getEnabled() == 1){
					writer.print("<eenabled>true</eenabled>");
					writer.print("<enabledIcon>bullet_green</enabledIcon>");
				}
				else if (tenant.getEnabled() == 0){
					writer.print("<eenabled>false</eenabled>");
					writer.print("<enabledIcon>bullet_red</enabledIcon>");
				}
				
				writer.print("<name><![CDATA[" + (tenant.getName() == null ? "" : tenant.getName()) + "]]></name>");
				writer.print("<email><![CDATA[" + (tenant.getEmail() == null ? "" : tenant.getEmail())
						+ "]]></email>");
				writer.print("<country><![CDATA[" + (tenant.getCountry() == null ? "" : tenant.getCountry())
						+ "]]></country>");
				writer.print("<email><![CDATA[" + (tenant.getEmail() == null ? "" : tenant.getEmail()) + "]]></email>");
				writer.print("<telephone><![CDATA[" + (tenant.getTelephone() == null ? "" : tenant.getTelephone())
						+ "]]></telephone>");
				writer.print("<city><![CDATA[" + (tenant.getCity() == null ? "" : tenant.getCity())
						+ "]]></city>");
				writer.print("<address><![CDATA[" + (tenant.getStreet() == null ? "" : tenant.getStreet())
						+ "]]></address>");
				writer.print("<expire>" + (tenant.getExpire() == null ? "" : df.format(tenant.getExpire()))
						+ "</expire>");

				
				writer.print("<postalCode><![CDATA[" + (tenant.getPostalCode() == null ? "" : tenant.getPostalCode())
						+ "]]></postalCode>");
				writer.print("<state><![CDATA[" + (tenant.getState() == null ? "" : tenant.getState())
						+ "]]></state>");
				
				writer.print("</tenant>");
			}

			writer.print("</list>");

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			if (e instanceof ServletException)
				throw (ServletException) e;
			else if (e instanceof IOException)
				throw (IOException) e;
			else
				throw new ServletException(e.getMessage(), e);
		}
	}
}