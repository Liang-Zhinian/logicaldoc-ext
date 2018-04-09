package com.logicaldoc.ext.tenant;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.logicaldoc.core.security.Session;
import com.logicaldoc.core.security.SessionManager;
import com.logicaldoc.core.security.Tenant;
import com.logicaldoc.core.security.User;
import com.logicaldoc.core.security.dao.TenantDAO;
import com.logicaldoc.core.security.dao.UserDAO;
import com.logicaldoc.core.sequence.SequenceDAO;
import com.logicaldoc.ext.branding.Branding;
import com.logicaldoc.ext.branding.BrandingDAO;
import com.logicaldoc.gui.common.client.AccessDeniedException;
import com.logicaldoc.gui.common.client.InvalidSessionException;
import com.logicaldoc.gui.common.client.ServerException;
import com.logicaldoc.gui.common.client.beans.GUIBranding;
import com.logicaldoc.gui.common.client.beans.GUITenant;
import com.logicaldoc.gui.frontend.client.services.TenantService;
import com.logicaldoc.util.Context;
import com.logicaldoc.web.UploadServlet;
import com.logicaldoc.web.service.SecurityServiceImpl;
import com.logicaldoc.web.util.ServiceUtil;

public class TenantServiceImpl extends RemoteServiceServlet implements TenantService {
	private static final long serialVersionUID = 1L;
	protected static Logger log = LoggerFactory.getLogger(TenantServiceImpl.class);
	public static long MENU_TENANTS = -1140L;
	public static long MENU_BRANDING = -1150L;
	public static long MENU_QUOTA = 1700L;

	private boolean isBrandingStandardEnabled = true; // LicenseManager.getInstance().isEnabled("Feature_6");
	private boolean isBrandingFullEnabled = true; // LicenseManager.getInstance().isEnabled("Feature_37");
	private boolean isBrandingLogoEnabled = true; // LicenseManager.getInstance().isEnabled("Feature_38");

	@Override
	public void delete(long paramLong) throws ServerException {
		ServiceUtil.checkMenu(getThreadLocalRequest(), MENU_TENANTS);
		// if (!LicenseManager.getInstance().isEnabled("Feature_79"))
		// {
		// log.info("Multi tenant feature not enabled");
		// return;
		// }
		try {
			TenantDAO localTenantDAO = (TenantDAO) Context.get().getBean(TenantDAO.class);
			localTenantDAO.delete(paramLong);
		} catch (Throwable localThrowable) {
			log.error(localThrowable.getMessage(), localThrowable);
			throw new RuntimeException(localThrowable.getMessage(), localThrowable);
		}
	}

	@Override
	public GUITenant save(GUITenant paramGUITenant) throws ServerException {
		Session localSession = checkMenuPermissions(paramGUITenant.getId());
		// if ((paramGUITenant.getId() != 1L) &&
		// (!LicenseManager.getInstance().isEnabled("Feature_79")))
		// {
		// log.info("Multi tenant feature not enabled");
		// return paramGUITenant;
		// }
		try {
			TenantDAO localTenantDAO = (TenantDAO) Context.get().getBean(TenantDAO.class);
			Tenant localTenant = (Tenant) localTenantDAO.findById(paramGUITenant.getId());
			if (localTenant == null) {
				localTenant = new Tenant();
			} else {
				localTenantDAO.initialize(localTenant);
			}
			localTenant.setName(paramGUITenant.getName());
			localTenant.setCity(paramGUITenant.getCity());
			localTenant.setDisplayName(paramGUITenant.getDisplayName());
			localTenant.setEmail(paramGUITenant.getEmail());
			localTenant.setPostalCode(paramGUITenant.getPostalCode());
			localTenant.setState(paramGUITenant.getState());
			localTenant.setStreet(paramGUITenant.getStreet());
			localTenant.setTelephone(paramGUITenant.getTelephone());
			localTenant.setType(paramGUITenant.getType());
			localTenant.setCountry(paramGUITenant.getCountry());
			localTenant.setTenantId(paramGUITenant.getTenantId());
			localTenant.setMaxRepoDocs(paramGUITenant.getMaxRepoDocs());
			localTenant.setMaxRepoSize(paramGUITenant.getMaxRepoSize());
			localTenant.setMaxSessions(paramGUITenant.getMaxSessions());
			localTenant.setQuotaThreshold(paramGUITenant.getQuotaThreshold());
			localTenant.setQuotaAlertRecipients(paramGUITenant.getQuotaAlertRecipientsAsString());
			localTenant.setMaxUsers(paramGUITenant.getMaxUsers());
			localTenant.setEnabled(paramGUITenant.isEnabled() ? 1 : 0);
			localTenant.setExpire(paramGUITenant.getExpire());
			paramGUITenant.setId(localTenant.getId());
			localTenantDAO.store(localTenant);
			UserDAO localUserDAO = (UserDAO) Context.get().getBean(UserDAO.class);
			User localUser = localUserDAO.findAdminUser(paramGUITenant.getName());
			paramGUITenant.setAdminUsername(localUser.getUsername());
			if (paramGUITenant.getBranding() != null) {
				try {
					saveBranding(paramGUITenant.getId(), paramGUITenant.getBranding());
				} catch (Throwable localThrowable2) {
					log.error(localThrowable2.getMessage(), localThrowable2);
				}
			}
			return load(localTenant.getId());
		} catch (Throwable localThrowable1) {
			return (GUITenant) ServiceUtil.throwServerException(localSession, log, localThrowable1);
		}
	}

	@Override
	public GUITenant load(long paramLong) throws ServerException {

		log.info("load tenant: " + paramLong);
		try {
			GUITenant localGUITenant = SecurityServiceImpl.getTenant(paramLong);
			SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
			UserDAO localUserDAO = (UserDAO) Context.get().getBean(UserDAO.class);
			try {
				localGUITenant.setUsers(localUserDAO.count(Long.valueOf(paramLong)));
				localGUITenant.setDocuments(localSequenceDAO.getCurrentValue("repodocs", 0L, paramLong));
				localGUITenant.setSize(localSequenceDAO.getCurrentValue("reposize", 0L, paramLong));
				localGUITenant.setSessions(SessionManager.get().countOpened(paramLong));
				User localUser = localUserDAO.findAdminUser(localGUITenant.getName());
				localGUITenant.setAdminUsername(localUser.getUsername());
			} catch (NullPointerException localNullPointerException) {
			}
			log.info("load branding: " + paramLong);
			localGUITenant.setBranding(loadBranding(paramLong));
			return localGUITenant;
		} catch (Throwable localThrowable) {
			return (GUITenant) ServiceUtil.throwServerException(null, log, localThrowable);
		}
	}

	public GUIBranding loadBranding(long paramLong) {
		log.info("loadBranding: " + paramLong);
		GUIBranding localGUIBranding = new GUIBranding();
		/*
		 * if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("Product")))
		 * {
		 * localGUIBranding.setProduct(LicenseManager.getInstance().getFeature("Product"
		 * )); } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("ProductName"
		 * ))) {
		 * localGUIBranding.setProductName(LicenseManager.getInstance().getFeature(
		 * "ProductName")); } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("Vendor"))) {
		 * localGUIBranding.setVendor(LicenseManager.getInstance().getFeature("Vendor"))
		 * ; } if (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature(
		 * "VendorAddress"))) {
		 * localGUIBranding.setVendorAddress(LicenseManager.getInstance().getFeature(
		 * "VendorAddress")); } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("VendorCap"))
		 * ) { localGUIBranding.setVendorCap(LicenseManager.getInstance().getFeature(
		 * "VendorCap")); } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature(
		 * "VendorAddress"))) {
		 * localGUIBranding.setVendorAddress(LicenseManager.getInstance().getFeature(
		 * "VendorAddress")); } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("VendorCity")
		 * )) { localGUIBranding.setVendorCity(LicenseManager.getInstance().getFeature(
		 * "VendorCity")); } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature(
		 * "VendorCountry"))) {
		 * localGUIBranding.setVendorCountry(LicenseManager.getInstance().getFeature(
		 * "VendorCountry")); } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("Support")))
		 * {
		 * localGUIBranding.setSupport(LicenseManager.getInstance().getFeature("Support"
		 * )); } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("Sales"))) {
		 * localGUIBranding.setSales(LicenseManager.getInstance().getFeature("Sales"));
		 * } if (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("Url")))
		 * { localGUIBranding.setUrl(LicenseManager.getInstance().getFeature("Url")); }
		 * if (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("Bugs")))
		 * { localGUIBranding.setBugs(LicenseManager.getInstance().getFeature("Bugs"));
		 * } if
		 * (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("Help"))) {
		 * localGUIBranding.setHelp(LicenseManager.getInstance().getFeature("Help")); }
		 * if (StringUtils.isNotEmpty(LicenseManager.getInstance().getFeature("Forum")))
		 * {
		 * localGUIBranding.setForum(LicenseManager.getInstance().getFeature("Forum"));
		 * }
		 */
		if (!isBrandingStandardEnabled) {
			log.info("isBrandingStandardEnabled is false.");
			return localGUIBranding;
		}
		BrandingDAO localBrandingDAO = (BrandingDAO) Context.get().getBean(BrandingDAO.class);
		Branding localBranding = localBrandingDAO.findByTenantId(paramLong);
		if (localBranding != null) {
			if (isBrandingLogoEnabled) {
				log.info("branding data is valid.");
				if (StringUtils.isNotEmpty(localBranding.getLogo())) {
					localGUIBranding.setLogoSrc(localBranding.getLogo());
					log.info("logo data is valid.");
				}
				if (StringUtils.isNotEmpty(localBranding.getLogoHead())) {
					localGUIBranding.setLogoHeadSrc(localBranding.getLogoHead());
					log.info("logo head data is valid.");
				}
				if (StringUtils.isNotEmpty(localBranding.getBanner())) {
					localGUIBranding.setBannerSrc(localBranding.getBanner());
				}
				if (StringUtils.isNotEmpty(localBranding.getFavicon())) {
					localGUIBranding.setFaviconSrc(localBranding.getFavicon());
				}
			}
			if (StringUtils.isNotEmpty(localBranding.getLogoOem())) {
				localGUIBranding.setLogoOemSrc(localBranding.getLogoOem());
			}
			if (StringUtils.isNotEmpty(localBranding.getLogoHeadOem())) {
				localGUIBranding.setLogoHeadOemSrc(localBranding.getLogoHeadOem());
			}
			if (isBrandingFullEnabled) {
				if (StringUtils.isNotEmpty(localBranding.getProduct())) {
					localGUIBranding.setProduct(localBranding.getProduct());
				}
				if (StringUtils.isNotEmpty(localBranding.getProductName())) {
					localGUIBranding.setProductName(localBranding.getProductName());
				}
				if (StringUtils.isNotEmpty(localBranding.getVendor())) {
					localGUIBranding.setVendor(localBranding.getVendor());
				}
				if (StringUtils.isNotEmpty(localBranding.getVendorAddress())) {
					localGUIBranding.setVendorAddress(localBranding.getVendorAddress());
				}
				if (StringUtils.isNotEmpty(localBranding.getVendorCap())) {
					localGUIBranding.setVendorCap(localBranding.getVendorCap());
				}
				if (StringUtils.isNotEmpty(localBranding.getVendorCity())) {
					localGUIBranding.setVendorCity(localBranding.getVendorCity());
				}
				if (StringUtils.isNotEmpty(localBranding.getVendorCountry())) {
					localGUIBranding.setVendorCountry(localBranding.getVendorCountry());
				}
				if (StringUtils.isNotEmpty(localBranding.getSupport())) {
					localGUIBranding.setSupport(localBranding.getSupport());
				}
				if (StringUtils.isNotEmpty(localBranding.getSales())) {
					localGUIBranding.setSales(localBranding.getSales());
				}
				if (StringUtils.isNotEmpty(localBranding.getUrl())) {
					localGUIBranding.setUrl(localBranding.getUrl());
				}
				if (StringUtils.isNotEmpty(localBranding.getForum())) {
					localGUIBranding.setForum(localBranding.getForum());
				}
				if (StringUtils.isNotEmpty(localBranding.getBugs())) {
					localGUIBranding.setBugs(localBranding.getBugs());
				}
				if (StringUtils.isNotEmpty(localBranding.getHelp())) {
					localGUIBranding.setHelp(localBranding.getHelp());
				}
				if (StringUtils.isNotEmpty(localBranding.getSkin())) {
					localGUIBranding.setSkin(localBranding.getSkin());
				}
				if (StringUtils.isNotEmpty(localBranding.getCss())) {
					localGUIBranding.setCss(localBranding.getCss());
				}
			}
		}
		return localGUIBranding;
	}

	public void saveBranding(long tenantId, GUIBranding paramGUIBranding) throws ServerException {
		if (paramGUIBranding == null) {
			return;
		}
		checkMenuPermissions(tenantId);
		// boolean bool1 = true; // LicenseManager.getInstance().isEnabled("Feature_6");
		// boolean bool2 = false; //
		// LicenseManager.getInstance().isEnabled("Feature_37");
		// boolean bool3 = true; //
		// LicenseManager.getInstance().isEnabled("Feature_38");
		if (!isBrandingStandardEnabled) {
			return;
		}
		BrandingDAO localBrandingDAO = (BrandingDAO) Context.get().getBean(BrandingDAO.class);
		Branding localBranding = localBrandingDAO.findByTenantId(tenantId);
		if (localBranding == null) {
			localBranding = new Branding();
			localBranding.setTenantId(tenantId);
		}
		localBranding.setLogoOem(paramGUIBranding.getLogoOem());
		localBranding.setLogoHeadOem(paramGUIBranding.getLogoHeadOem());
		if (isBrandingLogoEnabled) {
			localBranding.setLogo(paramGUIBranding.getLogo());
			localBranding.setLogoHead(paramGUIBranding.getLogoHead());
			localBranding.setBanner(paramGUIBranding.getBanner());
			localBranding.setFavicon(paramGUIBranding.getFavicon());
		}
		if (isBrandingFullEnabled) {
			localBranding.setProduct(paramGUIBranding.getProduct());
			localBranding.setProductName(paramGUIBranding.getProductName());
			localBranding.setVendor(paramGUIBranding.getVendor());
			localBranding.setVendorAddress(paramGUIBranding.getVendorAddress());
			localBranding.setVendorCap(paramGUIBranding.getVendorCap());
			localBranding.setVendorCity(paramGUIBranding.getVendorCity());
			localBranding.setVendorCountry(paramGUIBranding.getVendorCountry());
			localBranding.setBugs(paramGUIBranding.getBugs());
			localBranding.setForum(paramGUIBranding.getForum());
			localBranding.setHelp(paramGUIBranding.getHelp());
			localBranding.setUrl(paramGUIBranding.getUrl());
			localBranding.setSupport(paramGUIBranding.getSupport());
			localBranding.setSales(paramGUIBranding.getSales());
			localBranding.setSkin(paramGUIBranding.getSkin());
			localBranding.setCss(paramGUIBranding.getCss());
		}
		localBrandingDAO.store(localBranding);
	}

	private Session checkMenuPermissions(long paramLong) throws InvalidSessionException, AccessDeniedException {
		Session localSession = ServiceUtil.validateSession(getThreadLocalRequest());
		if (localSession.getTenantId() != paramLong) {
			ServiceUtil.checkMenu(getThreadLocalRequest(), MENU_TENANTS);
		} else {
			ServiceUtil.checkEvenOneMenu(getThreadLocalRequest(),
					new long[] { MENU_TENANTS, MENU_BRANDING, MENU_QUOTA });
		}
		return localSession;
	}

	@Override
	public void changeAdminPassword(String paramString1, String paramString2) throws ServerException {
		// Session localSession = ServiceUtil.checkMenu(getThreadLocalRequest(),
		// MENU_TENANTS);
		// try
		// {
		// User localUser1 = localSession.getUser();
		// if ((paramString2.equals("default")) || (localSession.getTenantId() != 1L) ||
		// (!localUser1.isMemberOf("admin")) || (localUser1.getTenantId() != 1L)) {
		// throw new ServerException("Cannot operate on tenants");
		// }
		// UserDAO localUserDAO = (UserDAO)Context.get().getBean(UserDAO.class);
		// User localUser2 = localUserDAO.findAdminUser(paramString2);
		// if (localUser2 == null) {
		// throw new Exception("Administrator of " + paramString2 + " not found");
		// }
		// localUserDAO.initialize(localUser2);
		// UserHistory localUserHistory = null;
		// localUser2.setDecodedPassword(paramString1);
		// localUser2.setPasswordChanged(new Date());
		// localUser2.setPasswordExpired(0);
		// localUserHistory = new UserHistory();
		// localUserHistory.setSessionId(localSession.getSid());
		// localUserHistory.setUser(localUser2);
		// localUserHistory.setEvent("event.user.passwordchanged");
		// localUserHistory.setComment("Changed password of tenant administrator");
		// localUser2.setRepass("");
		// localUserDAO.store(localUser2, localUserHistory);
		// }
		// catch (Throwable localThrowable)
		// {
		// log.error(localThrowable.getMessage(), localThrowable);
		// }
	}

	@Override
	public GUITenant changeSessionTenant(long paramLong) throws ServerException {
		Session localSession = ServiceUtil.validateSession(getThreadLocalRequest());
		try {
			GUITenant localGUITenant = load(paramLong);
			localSession.setTenantId(localGUITenant.getId());
			localSession.setTenantName(localGUITenant.getName());
			return localGUITenant;
		} catch (Throwable localThrowable) {
			log.error(localThrowable.getMessage(), localThrowable);
		}
		return null;
	}

	@Override
	public String encodeBrandingImage() throws ServerException {
		Session localSession = ServiceUtil.validateSession(getThreadLocalRequest());
		try {
			Map localMap = UploadServlet.getReceivedFiles(getThreadLocalRequest(), localSession.getSid());
			File localFile = (File) localMap.values().iterator().next();
			byte[] arrayOfByte = IOUtils.toByteArray(new FileInputStream(localFile));
			return Base64.encodeBase64String(arrayOfByte);
		} catch (Throwable localThrowable) {
			log.error(localThrowable.getMessage(), localThrowable);
		}
		return null;
	}

	@Override
	public GUIBranding importBrandingPackage() throws ServerException {
		// Session localSession = ServiceUtil.checkEvenOneMenu(getThreadLocalRequest(),
		// new long[] { MENU_TENANTS, MENU_BRANDING });
		// boolean bool1 = LicenseManager.getInstance().isEnabled("Feature_6");
		// boolean bool2 = LicenseManager.getInstance().isEnabled("Feature_37");
		// boolean bool3 = LicenseManager.getInstance().isEnabled("Feature_38");
		// if (!bool1) {
		// throw new ServerException("Branding feature not enabled");
		// }
		// File localFile1 = null;
		// try
		// {
		// localFile1 = File.createTempFile("branding",
		// Long.toString(System.nanoTime()));
		// localFile1.delete();
		// localFile1.mkdir();
		// Map localMap = UploadServlet.getReceivedFiles(getThreadLocalRequest(),
		// localSession.getSid());
		// File localFile2 = (File)localMap.values().iterator().next();
		// new ZipUtil().unzip(localFile2.getPath(), localFile1.getPath());
		// GUIBranding localGUIBranding = new GUIBranding();
		// File localFile3 = new File(localFile1, "config.txt");
		// if ((localFile3.exists()) && (bool2))
		// {
		// localObject1 = new Properties();
		// ((Properties)localObject1).load(new FileInputStream(localFile3));
		// localGUIBranding.setProduct(((Properties)localObject1).getProperty("Product"));
		// localGUIBranding.setProductName(((Properties)localObject1).getProperty("ProductName"));
		// localGUIBranding.setVendor(((Properties)localObject1).getProperty("Vendor"));
		// localGUIBranding.setVendorAddress(((Properties)localObject1).getProperty("VendorAddress"));
		// localGUIBranding.setVendorCap(((Properties)localObject1).getProperty("VendorCap"));
		// localGUIBranding.setVendorCity(((Properties)localObject1).getProperty("VendorCity"));
		// localGUIBranding.setVendorCountry(((Properties)localObject1).getProperty("VendorCountry"));
		// localGUIBranding.setUrl(((Properties)localObject1).getProperty("Url"));
		// localGUIBranding.setBugs(((Properties)localObject1).getProperty("Bugs"));
		// localGUIBranding.setHelp(((Properties)localObject1).getProperty("Help"));
		// localGUIBranding.setForum(((Properties)localObject1).getProperty("Forum"));
		// localGUIBranding.setSupport(((Properties)localObject1).getProperty("Support"));
		// localGUIBranding.setSales(((Properties)localObject1).getProperty("Sales"));
		// localGUIBranding.setSkin(((Properties)localObject1).getProperty("Skin"));
		// }
		// localFile3 = new File(localFile1, "logo_oem.png");
		// if (localFile3.exists())
		// {
		// localObject1 = IOUtils.toByteArray(new FileInputStream(localFile3));
		// localGUIBranding.setLogoOemSrc(Base64.encodeBase64String((byte[])localObject1));
		// }
		// localFile3 = new File(localFile1, "logo_head_oem.png");
		// if (localFile3.exists())
		// {
		// localObject1 = IOUtils.toByteArray(new FileInputStream(localFile3));
		// localGUIBranding.setLogoHeadOemSrc(Base64.encodeBase64String((byte[])localObject1));
		// }
		// if (bool2)
		// {
		// localFile3 = new File(localFile1, "custom.css");
		// if (localFile3.exists()) {
		// localGUIBranding.setCss(FileUtil.readFile(localFile3));
		// }
		// }
		// if (bool3)
		// {
		// localFile3 = new File(localFile1, "logo.png");
		// if (localFile3.exists())
		// {
		// localObject1 = IOUtils.toByteArray(new FileInputStream(localFile3));
		// localGUIBranding.setLogoSrc(Base64.encodeBase64String((byte[])localObject1));
		// }
		// localFile3 = new File(localFile1, "logo_head.png");
		// if (localFile3.exists())
		// {
		// localObject1 = IOUtils.toByteArray(new FileInputStream(localFile3));
		// localGUIBranding.setLogoHeadSrc(Base64.encodeBase64String((byte[])localObject1));
		// }
		// localFile3 = new File(localFile1, "banner.png");
		// if (localFile3.exists())
		// {
		// localObject1 = IOUtils.toByteArray(new FileInputStream(localFile3));
		// localGUIBranding.setBannerSrc(Base64.encodeBase64String((byte[])localObject1));
		// }
		// localFile3 = new File(localFile1, "favicon.png");
		// if (localFile3.exists())
		// {
		// localObject1 = IOUtils.toByteArray(new FileInputStream(localFile3));
		// localGUIBranding.setFaviconSrc(Base64.encodeBase64String((byte[])localObject1));
		// }
		// }
		// Object localObject1 = localGUIBranding;
		// return (GUIBranding)localObject1;
		// }
		// catch (Throwable localThrowable)
		// {
		// log.error(localThrowable.getMessage(), localThrowable);
		// }
		// finally
		// {
		// try
		// {
		// FileUtils.deleteDirectory(localFile1);
		// }
		// catch (IOException localIOException3) {}
		// }
		return null;
	}
}