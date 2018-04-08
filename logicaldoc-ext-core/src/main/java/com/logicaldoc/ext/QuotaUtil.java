package com.logicaldoc.ext;

import com.logicaldoc.core.communication.EMail;
import com.logicaldoc.core.communication.EMailSender;
import com.logicaldoc.core.communication.MessageTemplate;
import com.logicaldoc.core.communication.MessageTemplateDAO;
import com.logicaldoc.core.communication.Recipient;
import com.logicaldoc.core.communication.SystemMessage;
import com.logicaldoc.core.communication.SystemMessageDAO;
import com.logicaldoc.core.folder.Folder;
import com.logicaldoc.core.folder.FolderDAO;
import com.logicaldoc.core.folder.FolderEvent;
import com.logicaldoc.core.folder.FolderHistory;
import com.logicaldoc.core.folder.FolderHistoryDAO;
import com.logicaldoc.core.generic.Generic;
import com.logicaldoc.core.generic.GenericDAO;
import com.logicaldoc.core.security.Tenant;
import com.logicaldoc.core.security.User;
import com.logicaldoc.core.security.dao.TenantDAO;
import com.logicaldoc.core.security.dao.UserDAO;
import com.logicaldoc.i18n.I18N;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.io.FileUtil;
//import com.logicalobjects.jlicense.license.LicenseManager;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuotaUtil {
	public static long TOBYTES = 1048576L;
	protected static Logger log = LoggerFactory.getLogger(QuotaUtil.class);

	public static void notifyQuotaAlert(Tenant paramTenant, Folder paramFolder, Long paramLong1, Long paramLong2,
			Integer paramInteger) {
		Iterator<String> quotaAlertRecipient;
		String quotaAlertRecipientUserName;
		User quotaAlertRecipientUser;
		Iterator<String> localObject4;
		ArrayList localArrayList = new ArrayList();
		UserDAO localUserDAO = (UserDAO) Context.get().getBean(UserDAO.class);
		if ((paramTenant != null) && (!paramTenant.getQuotaAlertRecipientsAsList().isEmpty())) {
			quotaAlertRecipient = paramTenant.getQuotaAlertRecipientsAsList().iterator();
			while (((Iterator) quotaAlertRecipient).hasNext()) {
				quotaAlertRecipientUserName = (String) ((Iterator) quotaAlertRecipient).next();
				quotaAlertRecipientUser = localUserDAO.findByUsername((String) quotaAlertRecipientUserName);
				if (quotaAlertRecipientUser != null) {
					localArrayList.add(quotaAlertRecipientUser);
				}
			}
		}
		if ((paramFolder != null) && (!paramFolder.getQuotaAlertRecipientsAsList().isEmpty())) {
			quotaAlertRecipient = paramFolder.getQuotaAlertRecipientsAsList().iterator();
			while (((Iterator) quotaAlertRecipient).hasNext()) {
				quotaAlertRecipientUserName = (String) ((Iterator) quotaAlertRecipient).next();
				quotaAlertRecipientUser = localUserDAO.findByUsername((String) quotaAlertRecipientUserName);
				if (quotaAlertRecipientUser != null) {
					localArrayList.add(quotaAlertRecipientUser);
				}
			}
		}
		log.debug("Notify quota alert to " + localArrayList);
		Object localObject1 = (GenericDAO) Context.get().getBean(GenericDAO.class);
		Object localObject2 = new DecimalFormat("###,###,###");
		Object localObject3 = Calendar.getInstance();
		((Calendar) localObject3).add(11, -getAlertFreq());
		Date localDate = ((Calendar) localObject3).getTime();
		Iterator localIterator = localArrayList.iterator();
		while (localIterator.hasNext()) {
			User localUser = (User) localIterator.next();
			String str1 = "workspace-" + paramFolder.getId();
			long l = paramTenant != null ? paramTenant.getId() : paramFolder.getTenantId();
			String str2 = "quota-alert";
			Generic localGeneric = ((GenericDAO) localObject1).findByAlternateKey(str2, str1,
					Long.valueOf(localUser.getId()), l);
			if (localGeneric == null) {
				localGeneric = new Generic(str2, str1, Long.valueOf(localUser.getId()), l);
			}
			if ((localGeneric.getDate1() == null) || (!localDate.before(localGeneric.getDate1()))) {
				log.warn("Prepare Quota Alert for user " + localUser);
				HashMap localHashMap = new HashMap();
				if (paramTenant != null) {
					localHashMap.put("object",
							(paramTenant.getId() != -1L ? I18N.message("tenant", localUser.getLocale()) + " " : "")
									+ paramTenant.getDisplayName());
				} else {
					localHashMap.put("object",
							I18N.message("workspace", localUser.getLocale()) + " " + paramFolder.getName());
				}
				localHashMap.put("tenant", paramTenant);
				localHashMap.put("workspace", paramFolder);
				localHashMap.put("locale", localUser.getLocale());
				if (paramLong1 != null) {
					localHashMap.put("documents", ((DecimalFormat) localObject2).format(paramLong1));
				}
				if (paramLong2 != null) {
					localHashMap.put("size", FileUtil.getDisplaySize(paramLong2.longValue(), localUser.getLanguage()));
				}
				if (paramInteger != null) {
					localHashMap.put("users", ((DecimalFormat) localObject2).format(paramInteger));
				}
				MessageTemplateDAO localMessageTemplateDAO = (MessageTemplateDAO) Context.get()
						.getBean(MessageTemplateDAO.class);
				MessageTemplate localMessageTemplate = localMessageTemplateDAO.findByNameAndLanguage("quota.alert",
						localUser.getLanguage(), paramTenant != null ? paramTenant.getId()
								: paramTenant.getId() == -1L ? 1L : paramFolder.getTenantId());
				if (localMessageTemplate == null) {
					log.warn("Template 'quota.alert' not found");
				} else {
					String str3 = localMessageTemplate.getFormattedSubject(localHashMap);
					String str4 = localMessageTemplate.getFormattedBody(localHashMap);
					SystemMessage localSystemMessage = new SystemMessage();
					localSystemMessage.setTenantId(l);
					localSystemMessage.setType(0);
					localSystemMessage.setHtml(1);
					localSystemMessage.setAuthor("SYSTEM");
					localSystemMessage.setLocale(localUser.getLocale());
					localSystemMessage.setMessageText(str4);
					localSystemMessage.setSubject(str3);
					Recipient localRecipient = new Recipient();
					localRecipient.setAddress(localUser.getEmail());
					localRecipient.setName(localUser.getUsername());
					localRecipient.setType(0);
					localSystemMessage.getRecipients().add(localRecipient);
					SystemMessageDAO localSystemMessageDAO = (SystemMessageDAO) Context.get()
							.getBean(SystemMessageDAO.class);
					localSystemMessageDAO.store(localSystemMessage);
					EMail localEMail = new EMail();
					localEMail.setTenantId(localSystemMessage.getTenantId());
					localEMail.setHtml(localSystemMessage.getHtml());
					localEMail.setLocale(localSystemMessage.getLocale());
					localEMail.setAuthor(localSystemMessage.getAuthor());
					localEMail.setSubject(localSystemMessage.getSubject());
					localEMail.setMessageText(localSystemMessage.getMessageText());
					localRecipient = new Recipient();
					localRecipient.setAddress(localUser.getEmail());
					localRecipient.setName(localUser.getFullName());
					localRecipient.setMode("TO");
					localEMail.getRecipients().add(localRecipient);
					EMailSender localEMailSender = (EMailSender) Context.get().getBean(EMailSender.class);
					try {
						localEMailSender.sendAsync(localEMail);
					} catch (Exception localException) {
						log.error(localException.getMessage(), localException);
					}
					localGeneric.setDate1(new Date());
					((GenericDAO) localObject1).store(localGeneric);
				}
			}
		}
	}

	private static int getAlertFreq() {
		return Context.get().getProperties().getInt("quota.alert.freq", 24);
	}

	public static void registerWorkspaceThresholdOvercame(Folder paramFolder, Long paramLong1, Long paramLong2) {
		Object localObject;

		FolderDAO localFolderDAO = (FolderDAO) Context.get().getBean(FolderDAO.class);
		Calendar localCalendar = Calendar.getInstance();
		localCalendar.add(11, -getAlertFreq());
		FolderHistoryDAO localFolderHistoryDAO = (FolderHistoryDAO) Context.get().getBean(FolderHistoryDAO.class);
		List localList = localFolderHistoryDAO.findByFolderIdAndEvent(paramFolder.getId(),
				FolderEvent.QUOTA_OVERTHRESHOLD.toString(), localCalendar.getTime());
		if (localList.isEmpty()) {
			FolderHistory localFolderHistory = new FolderHistory();
			localFolderHistory.setEvent(FolderEvent.QUOTA_OVERTHRESHOLD.toString());
			String str = "";
			if (paramLong1 != null) {
				str = FileUtil.getDisplaySize(paramLong1.longValue(), null);
			}
			if (paramLong2 != null) {
				if (!str.isEmpty()) {
					str = str + " / ";
				}
				localObject = new DecimalFormat("###,###,###");
				str = str + ((DecimalFormat) localObject).format(paramLong2) + " docs";
			}
			localFolderHistory.setComment(str);
			localObject = (UserDAO) Context.get().getBean(UserDAO.class);
			localFolderHistory.setUser(((UserDAO) localObject).findByUsername("_system"));
			localFolderDAO.saveFolderHistory(paramFolder, localFolderHistory);
		}
	}

	private static final long getWholeSystemMaxSize() {
		return 1000000L;
		
//		long l = -1L;
//		try {
//			l = Long.parseLong(LicenseManager.getInstance().getFeature("Quota")) * TOBYTES;
//		} catch (Throwable localThrowable) {
//		}
//		return l;
	}

	private static final long getWholeSystemMaxDocs() {
		return 1000000L;
//		long l = -1L;
//		try {
//			l = Long.parseLong(LicenseManager.getInstance().getFeature("Documents"));
//		} catch (Throwable localThrowable) {
//		}
//		return l;
	}

	public static Tenant retrieveTenant(long paramLong) {
		TenantDAO localTenantDAO = (TenantDAO) Context.get().getBean(TenantDAO.class);
		Tenant localTenant1 = null;
		if (paramLong == -1L) {
			localTenant1 = new Tenant();
			localTenant1.setId(-1L);
			localTenant1.setName("system");
			localTenant1.setDisplayName("Whole System");
			localTenant1.setMaxRepoDocs(Long.valueOf(getWholeSystemMaxDocs()));
			localTenant1.setMaxRepoSize(Long.valueOf(getWholeSystemMaxSize()));
			localTenant1.setMaxUsers(10000/*Integer.valueOf(LicenseManager.getInstance().getDatabaseUsersNumber())*/);
			Tenant localTenant2 = (Tenant) localTenantDAO.findById(1L);
			localTenant1.setQuotaThreshold(localTenant2.getQuotaThreshold());
			localTenant1.setQuotaAlertRecipients(localTenant2.getQuotaAlertRecipients());
		} else {
			localTenant1 = (Tenant) localTenantDAO.findById(paramLong);
		}
		return localTenant1;
	}
}
