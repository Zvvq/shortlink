SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

CREATE DATABASE `short_link`
CHARACTER SET utf8mb4
COLLATE utf8mb4_bin;

USE `short_link`;

CREATE TABLE `short_link`.`scheduled` (
    `cron_id` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '定时器 ID（主键）',
    `cron_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '定时器名称',
    `cron` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'cron 表达式',
    PRIMARY KEY (`cron_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic COMMENT = '定时任务配置表';

CREATE TABLE `short_link`.`t_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gid` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '分组标识，62 进制编码',
    `name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '分组名称',
    `username` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建分组的用户名',
    `sort_order` INT NULL DEFAULT 0 COMMENT '分组排序序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识 0：未删除 1：已删除',
    INDEX `idx_username` (`username` ASC) USING BTREE,
    UNIQUE INDEX `uk_gid` (`gid` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin AUTO_INCREMENT = 2 ROW_FORMAT = Dynamic COMMENT = '短链接分组表';

CREATE TABLE `short_link`.`t_link` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `domain` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '域名',
    `short_uri` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '短链接标识',
    `full_short_url` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '完整短链接',
    `origin_url` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '原始链接',
    `click_num` BIGINT NULL DEFAULT 0 COMMENT '点击量',
    `gid` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '分组标识',
    `enable_status` TINYINT NULL DEFAULT 1 COMMENT '启用标识 0：未启用 1：已启用',
    `created_type` TINYINT NULL DEFAULT 0 COMMENT '创建类型 0：控制台 1：接口',
    `valid_date_type` TINYINT NULL DEFAULT 0 COMMENT '有效期类型 0：永久有效 1：用户自定义',
    `valid_date` DATETIME NULL COMMENT '有效期',
    `describe` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识 0：未删除 1：已删除',
    INDEX `idx_domain_short_uri` (`domain` ASC, `short_uri` ASC) USING BTREE,
    INDEX `idx_gid` (`gid` ASC) USING BTREE,
    UNIQUE INDEX `uk_full_short_url` (`full_short_url` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin AUTO_INCREMENT = 2 ROW_FORMAT = Dynamic COMMENT = '短链接表';

CREATE TABLE `short_link`.`t_link_access_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gid` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '分组标识',
    `full_short_url` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '完整短链接',
    `date` DATE NOT NULL COMMENT '日期',
    `pv` INT NULL DEFAULT 0 COMMENT '访问量',
    `uv` INT NULL DEFAULT 0 COMMENT '独立访问数',
    `uip` INT NULL DEFAULT 0 COMMENT '独立 IP 数',
    `hour` TINYINT NULL COMMENT '小时 0-23',
    `weekday` TINYINT NULL COMMENT '星期 1-7',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识 0：未删除 1：已删除',
    INDEX `idx_date` (`date` ASC) USING BTREE,
    INDEX `idx_gid` (`gid` ASC) USING BTREE,
    UNIQUE INDEX `uk_stats` (`full_short_url` ASC, `date` ASC, `hour` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin AUTO_INCREMENT = 11102 ROW_FORMAT = Dynamic COMMENT = '短链接访问统计表';

CREATE TABLE `short_link`.`t_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `username` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '密码',
    `real_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '手机号',
    `mail` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '邮箱',
    `deletion_time` BIGINT NULL COMMENT '注销时间戳',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识 0：未删除 1：已删除',
    INDEX `idx_mail` (`mail` ASC) USING BTREE,
    INDEX `idx_phone` (`phone` ASC) USING BTREE,
    UNIQUE INDEX `uk_username` (`username` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin AUTO_INCREMENT = 2 ROW_FORMAT = Dynamic COMMENT = '用户表';

INSERT INTO `short_link`.`scheduled` (`cron_id`, `cron_name`, `cron`)
VALUES ('1', '测试', '0/5 * * * * ?'),
       ('2', 'pv/uv 同步写入 mysql', '0 0/5 * * * ?');

-- 用户注册成功后会自动创建一条“默认分组”记录
INSERT INTO `short_link`.`t_group` (`id`, `gid`, `name`, `username`, `sort_order`, `create_time`, `update_time`, `del_flag`)
VALUES (1, 'DEFAULT', '默认分组', '1', 0, '2026-04-19 22:24:06', '2026-04-19 22:24:06', 0);

INSERT INTO `short_link`.`t_link`
(`id`, `domain`, `short_uri`, `full_short_url`, `origin_url`, `click_num`, `gid`, `enable_status`, `created_type`, `valid_date_type`, `valid_date`, `describe`, `create_time`, `update_time`, `del_flag`)
VALUES (1, 'localhost:8080', 'PemwYn', 'localhost:8080/PemwYn', 'http://localhost:8080/test2', 0, 'DEFAULT', 1, 0, 0, '2065-05-01 15:59:12', '修改', '2026-04-19 12:02:46', '2026-04-19 15:59:24', 0);

INSERT INTO `short_link`.`t_link_access_stats`
(`id`, `gid`, `full_short_url`, `date`, `pv`, `uv`, `uip`, `hour`, `weekday`, `create_time`, `update_time`, `del_flag`)
VALUES (1, NULL, 'PemwYn', '2026-04-18', 5911, 5917, 0, -1, 6, '2026-04-19 12:05:00', '2026-04-19 12:05:00', 0),
       (2, NULL, 'PemwYn', '2026-04-18', 2, 1, 0, 9, 6, '2026-04-19 12:05:00', '2026-04-19 12:05:00', 0),
       (3, NULL, 'PemwYn', '2026-04-18', 5909, 5917, 0, 10, 6, '2026-04-19 12:05:00', '2026-04-19 12:05:00', 0),
       (7, NULL, 'PemwYn', '2026-04-19', 465837, 475400, 0, -1, 7, '2026-04-19 12:10:00', '2026-04-19 22:57:33', 0),
       (8, NULL, 'PemwYn', '2026-04-19', 2, 1, 0, 12, 7, '2026-04-19 12:10:00', '2026-04-19 12:10:00', 0),
       (187, NULL, 'PemwYn', '2026-04-19', 20000, 19993, 0, 13, 7, '2026-04-19 13:36:35', '2026-04-19 13:58:14', 0),
       (3018, NULL, 'PemwYn', '2026-04-19', 630000, 625333, 0, 14, 7, '2026-04-19 14:00:34', '2026-04-19 14:23:27', 0),
       (4524, NULL, 'PemwYn', '2026-04-19', 1680695, 1680880, 0, 15, 7, '2026-04-19 15:43:28', '2026-04-19 16:10:52', 0),
       (5420, NULL, 'PemwYn', '2026-04-19', 12000, 12010, 0, 16, 7, '2026-04-19 16:10:52', '2026-04-19 16:10:52', 0),
       (5584, NULL, 'PemwYn', '2026-04-19', 465837, 475400, 0, 22, 7, '2026-04-19 22:42:05', '2026-04-19 22:57:33', 0),
       (5984, NULL, 'PemwYn', '2026-04-20', 133873, 132549, 0, -1, 1, '2026-04-20 09:05:56', '2026-04-20 12:53:03', 0),
       (5985, NULL, 'PemwYn', '2026-04-20', 43906, 43818, 0, 9, 1, '2026-04-20 09:05:56', '2026-04-20 09:30:32', 0),
       (7708, NULL, 'PemwYn', '2026-04-20', 89979, 89154, 0, 12, 1, '2026-04-20 12:51:05', '2026-04-20 12:53:03', 0),
       (11008, NULL, 'PemwYn', '2026-04-21', 280002, 278513, 0, -1, 2, '2026-04-21 14:50:00', '2026-04-21 15:30:00', 0),
       (11009, NULL, 'PemwYn', '2026-04-21', 160007, 160230, 0, 14, 2, '2026-04-21 14:50:00', '2026-04-21 14:55:00', 0),
       (11032, NULL, 'PemwYn', '2026-04-21', 120000, 119415, 0, 15, 2, '2026-04-21 15:20:00', '2026-04-21 15:30:00', 0),
       (11092, NULL, 'PemwYn', '2026-04-22', 80002, 80560, 0, -1, 3, '2026-04-22 09:50:06', '2026-04-22 09:50:06', 0),
       (11093, NULL, 'PemwYn', '2026-04-22', 80002, 80560, 0, 9, 3, '2026-04-22 09:50:06', '2026-04-22 09:50:06', 0);

INSERT INTO `short_link`.`t_user`
(`id`, `username`, `password`, `real_name`, `phone`, `mail`, `deletion_time`, `create_time`, `update_time`, `del_flag`)
VALUES (1, '1', '$2a$10$jjDuxyJrbNNgKD/mHevLeOugFpUNQH1bThAuTFapFvh9bZ4wfYA5q', '1', '1', '1', NULL, '2026-04-19 22:16:39', '2026-04-19 22:16:39', 0);

SET FOREIGN_KEY_CHECKS = 1;
