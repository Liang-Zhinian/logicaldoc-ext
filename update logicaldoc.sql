
-- tenant
INSERT INTO `ld_menu`
(`ld_id`,
`ld_lastmodified`,
`ld_recordversion`,
`ld_deleted`,
`ld_tenantid`,
`ld_name`,
`ld_parentid`,
`ld_securityref`,
`ld_icon`,
`ld_type`,
`ld_description`,
`ld_position`)
VALUES
(
'-1140', '2018-01-08 18:12:38', '1', '0', '1', 'tenant', '2', NULL, 'menu.png', '1', NULL, '1'
);

-- branding
INSERT INTO `ld_menu`
(`ld_id`,
`ld_lastmodified`,
`ld_recordversion`,
`ld_deleted`,
`ld_tenantid`,
`ld_name`,
`ld_parentid`,
`ld_securityref`,
`ld_icon`,
`ld_type`,
`ld_description`,
`ld_position`)
VALUES (
'-1150', '2018-01-08 18:12:38', '1', '0', '1', 'branding', '2', NULL, 'menu.png', '1', NULL, '1'

);

-- quota
insert into ld_menu (ld_id,ld_lastmodified,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position)
values (1700,CURRENT_TIMESTAMP,0,'quota',7,'quota.png',1,1,1,1);

-- branding table
create table ld_branding(ld_id bigint not null, ld_lastmodified datetime not null, ld_recordversion bigint not null,
ld_deleted int not null, ld_tenantid bigint not null, ld_logo text, ld_logohead text, 
ld_logooem text, ld_logoheadoem text, ld_banner text, ld_favicon text, 
ld_product varchar(255), ld_productname varchar(255), ld_vendor varchar(255), ld_vendorcap varchar(255),  
ld_vendorcountry varchar(255), ld_vendorcity varchar(255), ld_vendoraddress varchar(1000),
ld_support varchar(1000), ld_help varchar(1000), ld_bugs varchar(1000),   
ld_url varchar(1000), ld_forum varchar(1000), ld_sales varchar(1000), ld_skin varchar(255),
ld_css text, primary key (ld_id));

