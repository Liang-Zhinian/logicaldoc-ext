package com.logicaldoc.ext;

import com.logicaldoc.core.RunLevel;
import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.DocumentEvent;
import com.logicaldoc.core.document.DocumentListener;
import com.logicaldoc.core.document.DocumentHistory;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.folder.Folder;
import com.logicaldoc.core.folder.FolderDAO;
import com.logicaldoc.core.security.Tenant;
import com.logicaldoc.core.security.User;
import com.logicaldoc.core.security.dao.UserDAO;
import com.logicaldoc.core.sequence.Sequence;
import com.logicaldoc.core.sequence.SequenceDAO;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.concurrency.NamedThreadFactory;
import com.logicaldoc.util.config.ContextProperties;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentListenerImpl implements DocumentListener {

	public static final String WSDOCS = "wsdocs";
	public static final String WSSIZE = "wssize";
	public static final String REPODOCS = "repodocs";
	public static final String REPOSIZE = "reposize";
	public static final String USERQUOTA = "userquota";
	private static final String COUNTER_NEWDOC = "counter.newdoc";
	protected static Logger log = LoggerFactory.getLogger(DocumentListenerImpl.class);
	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(5,
			new NamedThreadFactory("QuotaUpdate"));

	public static void validateMaxDocuments(long tenantId, long folderId) throws Exception {
		SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
		Tenant localTenant = QuotaUtil.retrieveTenant(tenantId);
		if ((localTenant != null) && (localTenant.getMaxRepoDocs() != null)
				&& (localTenant.getMaxRepoDocs().longValue() > 0L)) {
			checkTenantDocumentCountThreshold(localTenant.getId());
			long l1 = localSequenceDAO.getCurrentValue("repodocs", 0L, tenantId);
			if (l1 >= localTenant.getMaxRepoDocs().longValue()) {
				log.error("Reached the maximum allowed documents number in the tenant " + localTenant.getName());
				throw new Exception(
						"Reached the maximum allowed documents number in the tenant " + localTenant.getName());
			}
		}
		if ((folderId != 0L) && (isWorkspaceQuotaChecksEnabled())) {
			FolderDAO localFolderDAO = (FolderDAO) Context.get().getBean(FolderDAO.class);
			Folder localFolder = localFolderDAO.findWorkspace(folderId);
			if ((localFolder.getQuotaDocs() != null) && (localFolder.getQuotaDocs().longValue() > 0L)) {
				checkWorkspaceDocumentCountThreshold(localFolder);
				long l2 = localSequenceDAO.getCurrentValue("wsdocs", folderId, tenantId);
				if (l2 >= localFolder.getQuotaDocs().longValue()) {
					log.error("Reached the maximum number of documents in the workspace " + localFolder.getName());
					throw new Exception(
							"Reached the maximum number of documents in the workspace " + localFolder.getName());
				}
			}
		}
	}

	public static void validateMaxSize(long tenantId, long folderId, long userId) throws Exception {
		SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
		UserDAO localUserDAO = (UserDAO) Context.get().getBean(UserDAO.class);
		Tenant localTenant = QuotaUtil.retrieveTenant(tenantId);
		if ((localTenant != null) && (localTenant.getMaxRepoSize() != null)
				&& (localTenant.getMaxRepoSize().longValue() > 0L)) {
			checkTenantStorageThreshold(localTenant.getId());
			long l1 = localSequenceDAO.getCurrentValue("reposize", 0L, tenantId);
			long l3 = localTenant.getMaxRepoSize().longValue() * QuotaUtil.TOBYTES;
			if (l1 >= l3) {
				log.error("Reached the maximum size of the repository in the tenant " + localTenant.getName());
				throw new Exception(
						"Reached the maximum size of the repository in the tenant " + localTenant.getName());
			}
		}
		User localUser = (User) localUserDAO.findById(userId);
		long l2 = -1L;
		if (localUser != null) {
			l2 = localUser.getQuota();
		}
		if (l2 > 0L) {
			long l4 = localSequenceDAO.getCurrentValue("userquota", userId, -1L);
			if (l4 >= l2) {
				log.error("Reached the maximum allowed quota for the user " + localUser.getUsername());
				throw new Exception("Reached the maximum allowed quota for the user " + localUser.getUsername());
			}
		}
		if ((folderId != 0L) && (isWorkspaceQuotaChecksEnabled())) {
			FolderDAO localFolderDAO = (FolderDAO) Context.get().getBean(FolderDAO.class);
			Folder localFolder = localFolderDAO.findWorkspace(folderId);
			if ((localFolder.getQuotaSize() != null) && (localFolder.getQuotaSize().longValue() > 0L)) {
				checkWorkspaceStorageThreshold(localFolder);
				long l5 = localSequenceDAO.getCurrentValue("wssize", folderId, tenantId);
				long l6 = localFolder.getQuotaSize().longValue() * QuotaUtil.TOBYTES;
				if (l5 >= l6) {
					log.error("Reached the maximum size of the workspace " + localFolder.getName());
					throw new Exception("Reached the maximum size of the workspace " + localFolder.getName());
				}
			}
		}
	}

	public static void validateDocumentsQuota(long tenantId, long userId, long folderId) throws Exception {
		validateMaxDocuments(tenantId, folderId);
		validateMaxSize(tenantId, folderId, userId);
	}

	@Override
	public void afterCheckin(Document paramDocument, DocumentHistory paramHistory, Map<String, Object> paramMap)
			throws Exception {
		if (paramDocument.getFileVersion().equals(paramDocument.getVersion())) {
			increaseStorageSize(paramDocument, paramDocument.getFileSize());
			increaseUserQuotaCount(paramDocument.getPublisherId(), paramDocument.getTenantId(),
					paramDocument.getFileSize());
			executor.schedule(new FutureTask(new QuotaUpdate(paramDocument, 0L, paramDocument.getFileSize())), 5L,
					TimeUnit.SECONDS);
		}
	}

	@Override
	public void afterStore(Document paramDocument, DocumentHistory paramHistory, Map<String, Object> paramMap)
			throws Exception {
		QuotaUpdate localQuotaUpdate = null;
		if ("true".equals(paramMap.get("counter.newdoc"))) {
			localQuotaUpdate = new QuotaUpdate(paramDocument, 1L, paramDocument.getFileSize());
		} else {
			long l;
			if ((paramHistory != null) && (paramHistory.getEvent().equals(DocumentEvent.RESTORED.toString()))) {
				l = computeTotalDocSize(paramDocument.getId());
				localQuotaUpdate = new QuotaUpdate(paramDocument, 1L, l);
			} else if (paramDocument.getDeleted() > 0) {
				l = computeTotalDocSize(paramDocument.getId());
				localQuotaUpdate = new QuotaUpdate(paramDocument, -1L, -l);
			}
		}
		if (localQuotaUpdate != null) {
			executor.schedule(new FutureTask(localQuotaUpdate), 5L, TimeUnit.SECONDS);
		}
	}

	@Override
	public void beforeCheckin(Document paramDocument, DocumentHistory paramHistory, Map<String, Object> paramMap)
			throws Exception {
		validateDocumentsQuota(paramDocument.getTenantId(), paramDocument.getPublisherId(),
				getWorkspaceId(paramDocument.getFolder().getId()));
		executor.schedule(new FutureTask(new QuotaUpdate(paramDocument)), 5L, TimeUnit.SECONDS);
	}

	private long getWorkspaceId(long paramLong) {
		FolderDAO localFolderDAO = (FolderDAO) Context.get().getBean(FolderDAO.class);
		Folder localFolder = localFolderDAO.findWorkspace(paramLong);
		return localFolder != null ? localFolder.getId() : 0L;
	}

	@Override
	public void beforeStore(Document paramDocument, DocumentHistory paramHistory, Map<String, Object> paramMap)
			throws Exception {
		if (paramDocument.getId() == 0L) {
			paramMap.put("counter.newdoc", "true");
		}
		if (paramDocument.getDeleted() != 1) {
			validateDocumentsQuota(paramDocument.getTenantId(), paramDocument.getPublisherId(),
					getWorkspaceId(paramDocument.getFolder().getId()));
		}
		executor.schedule(new FutureTask(new QuotaUpdate(paramDocument)), 5L, TimeUnit.SECONDS);
	}

	public static synchronized void updateStatistics(long paramLong, Long paramLong1, Long paramLong2,
			boolean paramBoolean) {
		try {
			ContextProperties localContextProperties = Context.get().getProperties();
			SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
			DocumentDAO localDocumentDAO = (DocumentDAO) Context.get().getBean(DocumentDAO.class);
			FolderDAO localFolderDAO = (FolderDAO) Context.get().getBean(FolderDAO.class);
			int i = 12;
			try {
				i = localContextProperties.getInt("count.ttl");
			} catch (Throwable localThrowable2) {
			}
			Calendar localCalendar = Calendar.getInstance();
			localCalendar.setTime(new Date());
			localCalendar.add(10, -i);
			Date localDate = localCalendar.getTime();
			Sequence localSequence = localSequenceDAO.findByAlternateKey("repodocs", 0L, paramLong);
			long l;
			if ((paramBoolean) || (localSequence == null)
					|| ((localSequence.getLastReset() != null) && (localSequence.getLastReset().before(localDate)))) {
				l = 0L;
				if (paramLong != -1L) {
					l = localDocumentDAO.count(Long.valueOf(paramLong), true, true);
				} else {
					l = localDocumentDAO.count(null, true, true);
				}
				localSequenceDAO.reset("repodocs", 0L, paramLong, l);
			}
			localSequence = localSequenceDAO.findByAlternateKey("reposize", 0L, paramLong);
			if ((paramBoolean) || (localSequence == null)
					|| ((localSequence.getLastReset() != null) && (localSequence.getLastReset().before(localDate)))) {
				l = localDocumentDAO.queryForLong(
						"SELECT SUM(A.ld_filesize) from ld_version A where A.ld_deleted = 0 and A.ld_version = A.ld_fileversion "
								+ (paramLong != -1L ? " and A.ld_tenantid=" + paramLong : ""));
				localSequenceDAO.reset("reposize", 0L, paramLong, l);
			}
			if (paramLong != -1L) {
				if ((paramLong2 != null) && (paramLong2.longValue() != 0L)) {
					localSequence = localSequenceDAO.findByAlternateKey("userquota", paramLong2.longValue(), paramLong);
					if ((paramBoolean) || (localSequence == null) || ((localSequence.getLastReset() != null)
							&& (localSequence.getLastReset().before(localDate)))) {
						l = localDocumentDAO.queryForLong(
								"SELECT SUM(A.ld_filesize) from ld_version A where A.ld_deleted = 0 and A.ld_version = A.ld_fileversion  and A.ld_tenantid="
										+ paramLong + " and ld_publisherid=" + paramLong2);
						localSequenceDAO.reset("userquota", paramLong2.longValue(), paramLong, l);
					}
				}
				if ((paramLong1 != null) && (paramLong1.longValue() != 0L) && (isWorkspaceQuotaChecksEnabled())) {
					localSequence = localSequenceDAO.findByAlternateKey("wsdocs", paramLong1.longValue(), paramLong);
					if ((paramBoolean) || (localSequence == null) || ((localSequence.getLastReset() != null)
							&& (localSequence.getLastReset().before(localDate)))) {
						localSequenceDAO.reset("wsdocs", paramLong1.longValue(), paramLong,
								localFolderDAO.countDocsInTree(paramLong1.longValue()));
					}
					localSequence = localSequenceDAO.findByAlternateKey("wssize", paramLong1.longValue(), paramLong);
					if ((paramBoolean) || (localSequence == null) || ((localSequence.getLastReset() != null)
							&& (localSequence.getLastReset().before(localDate)))) {
						localSequenceDAO.reset("wssize", paramLong1.longValue(), paramLong,
								localFolderDAO.computeTreeSize(paramLong1.longValue()));
					}
				}
			}
		} catch (Throwable localThrowable1) {
			log.warn(localThrowable1.getMessage(), localThrowable1);
		}
	}

	private static boolean isWorkspaceQuotaChecksEnabled() {
		return RunLevel.current().aspectEnabled("workspaceQuotaCheck");
	}

	private synchronized void increaseStorageSize(Document paramDocument, long paramLong) {
		try {
			SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
			localSequenceDAO.next("reposize", 0L, paramDocument.getTenantId(), paramLong);
			checkTenantStorageThreshold(paramDocument.getTenantId());
			localSequenceDAO.next("reposize", 0L, -1L, paramLong);
			checkTenantStorageThreshold(-1L);
			if (isWorkspaceQuotaChecksEnabled()) {
				FolderDAO localFolderDAO = (FolderDAO) Context.get().getBean(FolderDAO.class);
				Folder localFolder = localFolderDAO.findWorkspace(paramDocument.getFolder().getId());
				if (localFolder != null) {
					localSequenceDAO.next("wssize", localFolder.getId(), paramDocument.getTenantId(), paramLong);
					checkWorkspaceStorageThreshold(localFolder);
				}
			}
		} catch (Throwable localThrowable) {
			log.warn(localThrowable.getMessage(), localThrowable);
		}
	}

	private static void checkTenantStorageThreshold(long paramLong) {
		Tenant localTenant = QuotaUtil.retrieveTenant(paramLong);
		if (localTenant == null) {
			return;
		}
		SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
		long l1 = localSequenceDAO.getCurrentValue("reposize", 0L, paramLong);
		if ((localTenant.getMaxRepoSize() != null) && (localTenant.getMaxRepoSize().longValue() > 0L)
				&& (localTenant.getQuotaThreshold() != null)) {
			long l2 = localTenant.getMaxRepoSize().longValue() * QuotaUtil.TOBYTES;
			long l3 = Math.round(l2 * localTenant.getQuotaThreshold().intValue() / 100.0D);
			if (l1 >= l3) {
				QuotaUtil.notifyQuotaAlert(localTenant, null, null, Long.valueOf(l1), null);
			}
		}
	}

	private static void checkWorkspaceStorageThreshold(Folder paramFolder) {
		if ((paramFolder == null) || (!isWorkspaceQuotaChecksEnabled())) {
			return;
		}
		SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
		long l1 = localSequenceDAO.getCurrentValue("wssize", paramFolder.getId(), paramFolder.getTenantId());
		if ((paramFolder.getQuotaSize() != null) && (paramFolder.getQuotaSize().longValue() > 0L)
				&& (paramFolder.getQuotaThreshold() != null)) {
			long l2 = paramFolder.getQuotaSize().longValue() * QuotaUtil.TOBYTES;
			long l3 = Math.round(l2 * paramFolder.getQuotaThreshold().intValue() / 100.0D);
			if (l1 >= l3) {
				QuotaUtil.notifyQuotaAlert(null, paramFolder, null, Long.valueOf(l1), null);
				QuotaUtil.registerWorkspaceThresholdOvercame(paramFolder, Long.valueOf(l1), null);
			}
		}
	}

	private synchronized void increaseDocumentCount(Document paramDocument, long paramLong) {
		try {
			SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
			localSequenceDAO.next("repodocs", 0L, paramDocument.getTenantId(), paramLong);
			checkTenantDocumentCountThreshold(paramDocument.getTenantId());
			localSequenceDAO.next("repodocs", 0L, -1L, paramLong);
			checkTenantDocumentCountThreshold(-1L);
			FolderDAO localFolderDAO = (FolderDAO) Context.get().getBean(FolderDAO.class);
			Folder localFolder = localFolderDAO.findWorkspace(paramDocument.getFolder().getId());
			if ((localFolder != null) && (isWorkspaceQuotaChecksEnabled())) {
				localSequenceDAO.next("wsdocs", localFolder.getId(), paramDocument.getTenantId(), paramLong);
				checkWorkspaceDocumentCountThreshold(localFolder);
			}
		} catch (Throwable localThrowable) {
			log.warn(localThrowable.getMessage(), localThrowable);
		}
	}

	private static void checkTenantDocumentCountThreshold(long paramLong) {
		Tenant localTenant = QuotaUtil.retrieveTenant(paramLong);
		if (localTenant == null) {
			return;
		}
		SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
		long l1 = localSequenceDAO.getCurrentValue("repodocs", 0L, paramLong);
		if ((localTenant.getMaxRepoDocs() != null) && (localTenant.getMaxRepoDocs().longValue() > 0L)
				&& (localTenant.getQuotaThreshold() != null)) {
			long l2 = Math.round(
					localTenant.getMaxRepoDocs().longValue() * localTenant.getQuotaThreshold().intValue() / 100.0D);
			if (l1 >= l2) {
				QuotaUtil.notifyQuotaAlert(localTenant, null, Long.valueOf(l1), null, null);
			}
		}
	}

	private static void checkWorkspaceDocumentCountThreshold(Folder paramFolder) {
		if ((paramFolder == null) || (!isWorkspaceQuotaChecksEnabled())) {
			return;
		}
		SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
		long l1 = localSequenceDAO.getCurrentValue("wsdocs", paramFolder.getId(), paramFolder.getTenantId());
		if ((paramFolder.getQuotaDocs() != null) && (paramFolder.getQuotaDocs().longValue() > 0L)
				&& (paramFolder.getQuotaThreshold() != null)) {
			long l2 = Math.round(
					paramFolder.getQuotaDocs().longValue() * paramFolder.getQuotaThreshold().intValue() / 100.0D);
			if (l1 >= l2) {
				QuotaUtil.notifyQuotaAlert(null, paramFolder, Long.valueOf(l1), null, null);
				QuotaUtil.registerWorkspaceThresholdOvercame(paramFolder, null, Long.valueOf(l1));
			}
		}
	}

	private synchronized void increaseUserQuotaCount(long paramLong1, long paramLong2, long paramLong3) {
		try {
			SequenceDAO localSequenceDAO = (SequenceDAO) Context.get().getBean(SequenceDAO.class);
			localSequenceDAO.next("userquota", paramLong1, paramLong2, paramLong3);
		} catch (Throwable localThrowable) {
			log.warn(localThrowable.getMessage(), localThrowable);
		}
	}

	private long computeTotalDocSize(long paramLong) {
		DocumentDAO localDocumentDAO = (DocumentDAO) Context.get().getBean(DocumentDAO.class);
		long l = localDocumentDAO.queryForLong(
				"SELECT SUM(ld_filesize) from ld_version where ld_version = ld_fileversion  and ld_documentid="
						+ paramLong);
		return l;
	}

	static void killQuotaThreads() {
		log.info("Killing all timeout threads");
		executor.shutdownNow();
		try {
			executor.awaitTermination(3L, TimeUnit.SECONDS);
		} catch (InterruptedException localInterruptedException) {
		}
	}

	class QuotaUpdate implements Callable<String> {
		private Document document;
		private long count = 0L;
		private long size = 0L;

		public QuotaUpdate(Document paramDocument) {
			this.document = paramDocument;
		}

		public QuotaUpdate(Document paramDocument, long paramLong1, long paramLong2) {
			this.document = paramDocument;
			this.count = paramLong1;
			this.size = paramLong2;
		}

		public String call() throws Exception {
			long l = DocumentListenerImpl.this.getWorkspaceId(this.document.getFolder().getId());
			DocumentListenerImpl.updateStatistics(this.document.getTenantId(), Long.valueOf(l),
					Long.valueOf(this.document.getPublisherId()), false);
			if (this.document.getTenantId() != -1L) {
				DocumentListenerImpl.updateStatistics(-1L, Long.valueOf(l), Long.valueOf(this.document.getPublisherId()),
						false);
			}
			if (this.count != 0L) {
				DocumentListenerImpl.this.increaseDocumentCount(this.document, this.count);
			}
			if (this.size != 0L) {
				DocumentListenerImpl.this.increaseStorageSize(this.document, this.size);
				DocumentListenerImpl.this.increaseUserQuotaCount(this.document.getPublisherId(), this.document.getTenantId(),
						this.size);
			}
			return "done";
		}
	}
}
