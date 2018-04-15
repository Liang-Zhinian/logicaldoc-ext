package com.logicaldoc.ext.tenant;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import com.logicaldoc.core.HibernatePersistentObjectDAO;
import com.logicaldoc.core.PersistentObject;
import com.logicaldoc.core.communication.MessageTemplate;
import com.logicaldoc.core.communication.MessageTemplateDAO;
import com.logicaldoc.core.folder.Folder;
import com.logicaldoc.core.folder.FolderDAO;
import com.logicaldoc.core.folder.FolderGroup;
import com.logicaldoc.core.generic.Generic;
import com.logicaldoc.core.generic.GenericDAO;
import com.logicaldoc.core.metadata.Attribute;
import com.logicaldoc.core.metadata.AttributeSet;
import com.logicaldoc.core.metadata.AttributeSetDAO;
import com.logicaldoc.core.metadata.Template;
import com.logicaldoc.core.metadata.TemplateDAO;
import com.logicaldoc.core.security.Group;
import com.logicaldoc.core.security.Menu;
import com.logicaldoc.core.security.SecurityManager;
import com.logicaldoc.core.security.Tenant;
import com.logicaldoc.core.security.User;
import com.logicaldoc.core.security.UserHistory;
import com.logicaldoc.core.security.dao.GroupDAO;
import com.logicaldoc.core.security.dao.TenantDAO;
import com.logicaldoc.core.security.dao.UserDAO;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.sql.SqlUtil;

@SuppressWarnings("unchecked")
public class HibernateTenantDAO extends HibernatePersistentObjectDAO<Tenant> implements TenantDAO {

	private ContextProperties conf;
	private FolderDAO folderDao;
	private GroupDAO groupDao;
	private UserDAO userDao;
	private GenericDAO genericDao;
	private TemplateDAO templateDao;
	private AttributeSetDAO attributeSetDao;
	private MessageTemplateDAO messageTemplateDao;

	long adminGroupId = 0L;

	protected HibernateTenantDAO() {
		super(Tenant.class);
	}

	@Override
	public boolean delete(long paramLong, int paramInt) {
		assert (paramInt != 0);
		boolean bool = true;
		String str = null;
		try {
			Tenant localTenant = (Tenant) findById(paramLong);
			str = localTenant.getName();
			refresh(localTenant);
			if (localTenant != null) {
				localTenant.setName(localTenant.getName() + "." + localTenant.getId());
				localTenant.setDeleted(paramInt);
				saveOrUpdate(localTenant);
			}
		} catch (Exception localException) {
			if (this.log.isErrorEnabled()) {
				this.log.error(localException.getMessage(), localException);
			}
			bool = false;
		}
		if (str != null) {
			this.conf.removeTenantProperties(str);
			try {
				this.conf.write();
			} catch (IOException localIOException) {
				this.log.warn("Unable to remove configuration settings", localIOException);
			}
		}
		return bool;
	}

	@Override
	public Tenant findByName(String name) {
		Tenant tenant = null;
		Collection<Tenant> coll = findByWhere("_entity.name = '" + SqlUtil.doubleQuotes(name) + "'", null, null);
		if (coll.size() > 0) {
			tenant = coll.iterator().next();
			if (tenant.getDeleted() == 1)
				tenant = null;
		}

		return tenant;
	}

	@Override
	public int count() {
		String query = "select count(*) from ld_tenant where ld_deleted=0";
		return queryForInt(query);
	}

	public void setFolderDao(FolderDAO folderDao) {
		this.folderDao = folderDao;
	}

	public void setGroupDao(GroupDAO groupDao) {
		this.groupDao = groupDao;
	}

	public void setUserDao(UserDAO userDao) {
		this.userDao = userDao;
	}

	public void setConf(ContextProperties conf) {
		this.conf = conf;
	}

	public void setGenericDao(GenericDAO genericDao) {
		this.genericDao = genericDao;
	}

	public void setTemplateDao(TemplateDAO templateDao) {
		this.templateDao = templateDao;
	}

	@Override
	public Set<String> findAllNames() {
		Set<String> names = new HashSet<String>();
		List<Tenant> tenants = findAll();
		for (Tenant tenant : tenants) {
			names.add(tenant.getName());
		}
		return names;
	}

	public void setMessageTemplateDao(MessageTemplateDAO messageTemplateDao) {
		this.messageTemplateDao = messageTemplateDao;
	}

	@Override
	public boolean store(Tenant paramTenant) {
		int i = paramTenant.getId() == 0L ? 1 : 0;
		// if ((i != 0) && (TenantCounter.getMaxTenants() == TenantCounter.getCount()))
		// {
		// this.log.error("Reached the maximum number of tenants");
		// throw new RuntimeException("You cannot add more tenants");
		// }
		if (paramTenant.getExpire() != null) {
			paramTenant.setExpire(DateUtils.truncate(paramTenant.getExpire(), 5));
			paramTenant.setExpire(DateUtils.addHours(paramTenant.getExpire(), 23));
			paramTenant.setExpire(DateUtils.addMinutes(paramTenant.getExpire(), 59));
			paramTenant.setExpire(DateUtils.addSeconds(paramTenant.getExpire(), 59));
		}
		boolean bool = super.store(paramTenant);
		if (!bool) {
			return bool;
		}
		if (i != 0) {
			paramTenant.setTenantId(paramTenant.getId());
			bool = super.store(paramTenant);
			Folder localFolder1 = new Folder("/");
			localFolder1.setType(1);
			localFolder1.setTenantId(paramTenant.getId());
			bool = this.folderDao.store(localFolder1);
			if (!bool) {
				return bool;
			}
			localFolder1.setParentId(localFolder1.getId());
			bool = this.folderDao.store(localFolder1);
			if (!bool) {
				return bool;
			}
			Folder localFolder2 = new Folder("Default");
			localFolder2.setTenantId(paramTenant.getId());
			localFolder2.setType(1);
			localFolder2.setParentId(localFolder1.getId());
			bool = this.folderDao.store(localFolder2);
			if (!bool) {
				return bool;
			}
			Group localGroup1 = replicateGroup("admin", paramTenant.getId());
			if (localGroup1 == null) {
				bool = false;
				return bool;
			}
			User localUser = new User();
			localUser.setUsername("admin" + StringUtils.capitalize(paramTenant.getName()));
			localUser.setDecodedPassword("admin");
			localUser.setTenantId(paramTenant.getId());
			localUser.setName(paramTenant.toString());
			localUser.setFirstName("Administrator");
			localUser.setEmail(paramTenant.getEmail());
			localUser.setLanguage("en");
			bool = this.userDao.store(localUser);
			if (!bool) {
				return bool;
			}
			flush();
			this.userDao.jdbcUpdate("insert into ld_usergroup(ld_groupid,ld_userid) values (?,?)",
					new Object[] { Long.valueOf(localGroup1.getId()), Long.valueOf(localUser.getId()) });
			replicateGroup("guest", paramTenant.getId());
			replicateGroup("author", paramTenant.getId());
			replicateGroup("poweruser", paramTenant.getId());
			replicateGroup("publisher", paramTenant.getId());
			Group localGroup2 = new Group();
			localGroup2.setName("guest");
			localGroup2.setDescription("Group of guests");
			localGroup2.setTenantId(paramTenant.getId());
			this.groupDao.store(localGroup1);
			if (!bool) {
				return bool;
			}
			long[] arrayOfLong1 = { Menu.DOCUMENTS, 5L, 1510L, 1520L, 1530L };
			for (long l1 : arrayOfLong1) {
				this.userDao.jdbcUpdate("insert into ld_menugroup(ld_groupid,ld_menuid, ld_write) values (?,?,0)",
						new Object[] { Long.valueOf(localGroup1.getId()), Long.valueOf(l1) });
			}
			long[] arrayOfLong2 = new long[] { localFolder2.getId(), localFolder2.getParentId() };
			for (long l2 : arrayOfLong2) {
				this.userDao.jdbcUpdate(
						"insert into ld_foldergroup(ld_folderid, ld_groupid, ld_write , ld_add, ld_security, ld_immutable, ld_delete, ld_rename, ld_import, ld_export, ld_sign, ld_archive, ld_workflow, ld_download, ld_calendar, ld_subscription, ld_print, ld_password) values (?,?,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)",
						new Object[] { Long.valueOf(l2), Long.valueOf(localGroup1.getId()) });
			}
			List<AttributeSet> attributes = this.attributeSetDao.findAll(1L);
			Object localObject2 = ((List) attributes).iterator();
			Object localObject3;
			Object localObject4;
			Object localObject5;
			Object localObject6;
			Object localObject7;
			Object localObject8;
			while (((Iterator) localObject2).hasNext()) {
				localObject3 = (AttributeSet) ((Iterator) localObject2).next();
				localObject4 = new AttributeSet();
				((AttributeSet) localObject4).setName(((AttributeSet) localObject3).getName());
				((AttributeSet) localObject4).setDescription(((AttributeSet) localObject3).getDescription());
				((AttributeSet) localObject4).setReadonly(((AttributeSet) localObject3).getReadonly());
				((AttributeSet) localObject4).setTenantId(paramTenant.getId());
				((AttributeSet) localObject4).setType(((AttributeSet) localObject3).getType());
				localObject5 = ((AttributeSet) localObject3).getAttributes();
				localObject6 = ((Map) localObject5).keySet().iterator();
				while (((Iterator) localObject6).hasNext()) {
					localObject7 = (String) ((Iterator) localObject6).next();
					localObject8 = (Attribute) ((Map) localObject5).get(localObject7);
					try {
						((AttributeSet) localObject4).getAttributes().put((String) localObject7,
								(Attribute) ((Attribute) localObject8).clone());
					} catch (CloneNotSupportedException localCloneNotSupportedException2) {
					}
				}
				this.attributeSetDao.store((AttributeSet) localObject4);
			}
			localObject2 = this.templateDao.findAll(1L);
			localObject3 = ((List) localObject2).iterator();
			while (((Iterator) localObject3).hasNext()) {
				localObject4 = (Template) ((Iterator) localObject3).next();
				localObject5 = new Template();
				((Template) localObject5).setName(((Template) localObject4).getName());
				((Template) localObject5).setDescription(((Template) localObject4).getDescription());
				((Template) localObject5).setReadonly(((Template) localObject4).getReadonly());
				((Template) localObject5).setTenantId(paramTenant.getId());
				((Template) localObject5).setType(((Template) localObject4).getType());
				localObject6 = ((Template) localObject4).getAttributes();
				if ((localObject6 != null) && (!((Map) localObject6).isEmpty())) {
					localObject7 = ((Map) localObject6).keySet().iterator();
					while (((Iterator) localObject7).hasNext()) {
						localObject8 = (String) ((Iterator) localObject7).next();
						Attribute localAttribute1 = (Attribute) ((Map) localObject6).get(localObject8);
						try {
							Attribute localAttribute2 = (Attribute) localAttribute1.clone();
							localAttribute2.setSetId(null);
							((Template) localObject5).getAttributes().put((String) localObject8, localAttribute2);
						} catch (CloneNotSupportedException localCloneNotSupportedException3) {
						}
					}
				}
				this.templateDao.store((Template) localObject5);
			}
			localObject3 = this.messageTemplateDao.findAll(1L);
			localObject4 = ((List) localObject3).iterator();
			while (((Iterator) localObject4).hasNext()) {
				localObject5 = (MessageTemplate) ((Iterator) localObject4).next();
				try {
					localObject6 = (MessageTemplate) ((MessageTemplate) localObject5).clone();
					((MessageTemplate) localObject6).setTenantId(paramTenant.getId());
					this.messageTemplateDao.store((MessageTemplate) localObject6);
				} catch (CloneNotSupportedException localCloneNotSupportedException1) {
					this.log.error(localCloneNotSupportedException1.getMessage());
				}
			}
			flush();
			localObject4 = new Generic("customid-scheme", "--", null, paramTenant.getId());
			((Generic) localObject4).setString1("<id>");
			((Generic) localObject4).setInteger1(Long.valueOf(0L));
			this.genericDao.store((Generic) localObject4);
			try {
				this.conf.replicateTenantSettings(paramTenant.getName());
				this.conf.write();
			} catch (IOException localIOException) {
				this.log.warn("Unable update the configuration settings", localIOException);
			}
		}
		try {
			flush();
		} catch (Throwable localThrowable) {
		}
		return bool;
	}

	public void setAttributeSetDao(AttributeSetDAO attributeSetDao) {
		this.attributeSetDao = attributeSetDao;
	}

	private Group replicateGroup(String paramString, long paramLong) {
		Group localGroup1 = this.groupDao.findByName(paramString, 1L);
		if (localGroup1 == null) {
			return null;
		}
		this.groupDao.initialize(localGroup1);
		Group localGroup2 = new Group();
		localGroup2.setName(localGroup1.getName());
		localGroup2.setDescription(localGroup1.getDescription());
		localGroup2.setTenantId(paramLong);
		boolean bool = this.groupDao.store(localGroup2);
		flush();
		if (!bool) {
			return null;
		}
		List localList = this.groupDao
				.queryForList("select ld_menuid from ld_menugroup where ld_groupid=" + localGroup1.getId(), Long.class);
		Iterator localIterator = localList.iterator();
		while (localIterator.hasNext()) {
			Long localLong = (Long) localIterator.next();
			this.groupDao.jdbcUpdate("insert into ld_menugroup(ld_menuid, ld_groupid, ld_write) values(?,?,0)",
					new Object[] { localLong, Long.valueOf(localGroup2.getId()) });
		}
		return localGroup2;
	}
}