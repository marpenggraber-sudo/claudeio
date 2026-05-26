-- 添加账号和密码字段到 user_account 表
ALTER TABLE user_account
    ADD COLUMN account VARCHAR(64) NULL UNIQUE COMMENT '用户账号',
    ADD COLUMN password VARCHAR(128) NULL COMMENT '用户密码';
