/*
 Navicat MySQL Data Transfer
 Source Server Type    : MySQL
 Target Server Type    : MySQL
 File Encoding         : 65001
 
 Date: 2025-12-21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 打印机设备表
-- ----------------------------
CREATE TABLE IF NOT EXISTS printers
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT                             NOT NULL,
    name        VARCHAR(100)                       NOT NULL COMMENT '打印机名称',
    device_id   VARCHAR(100)                       NOT NULL COMMENT '拓竹设备ID（dev_id）',
    model       VARCHAR(50)                        NULL COMMENT '设备型号（如P1P、P1S、A1等）',
    access_code VARCHAR(20)                        NULL COMMENT '设备访问码',
    ip_address  VARCHAR(45)                        NULL COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_device (device_id)
) COMMENT ='打印机设备' CHARSET = utf8mb4;

-- ----------------------------
-- 2. AMS 模块表
-- ----------------------------
CREATE TABLE IF NOT EXISTS ams
(
    id          bigint auto_increment primary key,
    printer_id  bigint                             not null,
    user_id     bigint                             not null,
    ams_no      int                                not null comment 'AMS编号，从0开始',
    is_delete   tinyint  default 0                 null comment '是否删除(0:未删除,1:已删除)',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_printer_ams unique (printer_id, ams_no)
) comment 'AMS 模块表' charset = utf8mb4;

-- ----------------------------
-- 3. AMS 槽位绑定表
-- ----------------------------
CREATE TABLE IF NOT EXISTS ams_slots
(
    id          bigint auto_increment primary key,
    ams_id      bigint                             not null,
    user_id     bigint                             not null,
    slot_no     int                                not null comment '槽位编号（0~3）',
    spool_id    bigint                             null comment '绑定的耗材ID',
    is_delete   tinyint  default 0                 null comment '是否删除(0:未删除,1:已删除)',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_ams_slot unique (ams_id, slot_no)
) comment 'AMS 槽位绑定表' charset = utf8mb4;

-- ----------------------------
-- 4. 耗材料盘表
-- ----------------------------
CREATE TABLE IF NOT EXISTS spools
(
    id              bigint auto_increment primary key,
    user_id         bigint                             not null comment '用户id',
    name            varchar(100)                       not null comment '名称',
    material_type   varchar(50)                        null comment '耗材类型',
    color           varchar(50)                        null comment '颜色',
    brand           varchar(100)                       null comment '品牌',
    appearance      int                                null comment '外观/质感',
    net_weight      float                              null comment '耗材净重（g）',
    spool_weight    float                              null comment '料盘重量（g）',
    used_weight     float    default 0                 null comment '累计使用重量（g）',
    first_used_time datetime                           null comment '首次使用时间',
    last_used_time  datetime                           null comment '最后使用时间',
    price           float                              null comment '价格',
    remark          varchar(500)                       null comment '备注',
    is_delete       tinyint  default 0                 null comment '是否删除(0:未删除,1:已删除)',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
) COMMENT ='耗材料盘表' CHARSET = utf8mb4;

-- ----------------------------
-- 5. 耗材使用记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS spool_used_records
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id        BIGINT                               NOT NULL COMMENT '用户id',
    spool_id       BIGINT                               NOT NULL COMMENT '耗材id',
    title          VARCHAR(200)                         NULL COMMENT '任务标题',
    cover_url      VARCHAR(255)                         NULL COMMENT '封面图片',
    status         TINYINT                              NOT NULL COMMENT '任务状态',
    print_progress TINYINT UNSIGNED                     NULL COMMENT '打印进度',
    task_weight    DECIMAL(10, 2)                       NULL COMMENT '任务重量(克)',
    actual_weight  DECIMAL(10, 2)                       NULL COMMENT '实际使用重量(克)',
    start_time     DATETIME                             NULL COMMENT '开始时间',
    end_time       DATETIME                             NULL COMMENT '结束时间',
    order_id       BIGINT     DEFAULT NULL COMMENT '所属订单 ID',
    is_delete      TINYINT(1) DEFAULT 0                 NOT NULL COMMENT '是否删除(0:未删除,1:已删除)',
    create_time    DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time    DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_spr_user_id (user_id),
    KEY idx_spr_spool_id (spool_id),
    KEY idx_spr_start_time (start_time),
    KEY idx_spr_status (status)
) COMMENT ='耗材使用记录' CHARSET = utf8mb4
                          ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 6. 耗材使用快照
-- ----------------------------
CREATE TABLE IF NOT EXISTS spool_used_snapshots
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    record_id   BIGINT                             NOT NULL COMMENT '关联打印记录ID',
    percent     TINYINT                            NOT NULL COMMENT '进度百分比',
    image_url   VARCHAR(500)                       NOT NULL COMMENT '快照图片URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_record_percent (record_id, percent)
) COMMENT ='耗材使用快照' CHARSET = utf8mb4
                          ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 7. 订单信息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `order_info`
(
    `id`              bigint   NOT NULL AUTO_INCREMENT,
    `user_id`         bigint   NOT NULL COMMENT '所属用户ID',
    `order_name`      varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '订单名称',
    `customer_name`   varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户名称',
    `customer_source` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户来源',
    `status`          tinyint                                 DEFAULT '0' COMMENT '订单状态 0-未开始 1-进行中 2-已完成 3-已取消',
    `start_time`      datetime                                DEFAULT NULL COMMENT '订单开始时间',
    `end_time`        datetime                                DEFAULT NULL COMMENT '订单结束时间',
    `material_cost`   decimal(10, 2)                          DEFAULT '0.00' COMMENT '耗材成本',
    `other_cost`      decimal(10, 2)                          DEFAULT '0.00' COMMENT '其他成本',
    `total_income`    decimal(10, 2)                          DEFAULT '0.00' COMMENT '收入',
    `remark`          text COLLATE utf8mb4_general_ci COMMENT '备注',
    `is_delete`       tinyint(1)                              DEFAULT '0' COMMENT '是否删除(0:未删除,1:已删除)',
    `create_time`     datetime NOT NULL                       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime NOT NULL                       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='订单信息表';

-- ----------------------------
-- 8. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS user
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username                VARCHAR(50)                        NOT NULL COMMENT '用户名',
    password                VARCHAR(100)                       NOT NULL COMMENT '密码',
    nickname                VARCHAR(50)                        NULL COMMENT '昵称',
    avatar                  VARCHAR(255)                       NULL COMMENT '头像URL',
    email                   VARCHAR(100)                       NULL COMMENT '邮箱',
    phone                   VARCHAR(20)                        NULL COMMENT '手机号',
    role                    VARCHAR(10)                        NULL COMMENT '角色',
    bambu_username          VARCHAR(100)                       NULL COMMENT '拓竹云用户名',
    bambu_user_id           VARCHAR(50)                        NULL COMMENT '拓竹云用户ID（纯数字）',
    bambu_token             TEXT                               NULL COMMENT '拓竹云访问令牌',
    bambu_token_expire_time DATETIME                           NULL COMMENT '拓竹云令牌过期时间',
    is_delete               TINYINT  DEFAULT 0                 NULL COMMENT '是否删除(0:未删除,1:已删除)',
    create_time             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_user_bambu_user_id (bambu_user_id),
    KEY idx_user_email (email),
    KEY idx_user_phone (phone)
) COMMENT ='用户表' CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;