-- ContentGenius 建库 + 建表 + 种子（精简，可直接整文件执行）
-- 密码 123456 的 BCrypt：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi

SET NAMES utf8mb4;

-- ======================== user 库 ========================
CREATE DATABASE IF NOT EXISTS contentgenius_user DEFAULT CHARSET utf8mb4;
USE contentgenius_user;

CREATE TABLE IF NOT EXISTS `user` (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(128) NOT NULL,
    email        VARCHAR(128) DEFAULT NULL,
    phone        VARCHAR(20)  DEFAULT NULL,
    member_level TINYINT      DEFAULT 0,
    status       TINYINT      DEFAULT 1,
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS permission (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_permission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_permission (user_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS member_level_permission (
    member_level  TINYINT NOT NULL,
    permission_id BIGINT  NOT NULL,
    PRIMARY KEY (member_level, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO permission (id, code, name) VALUES
(1, 'content:read', '查看内容'),
(2, 'content:write', '编辑内容'),
(3, 'agent:chat', 'Agent对话'),
(4, 'admin:manage', '管理'),
(5, 'user:read', '用户信息')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO member_level_permission (member_level, permission_id) VALUES
(0,1),(0,3),(0,5),
(1,1),(1,2),(1,3),(1,5),
(2,1),(2,2),(2,3),(2,4),(2,5)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO `user` (id, username, password, email, phone, member_level, status) VALUES
(1, 'test01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'test01@test.com', '13800000000', 2, 1),
(2, 'admin01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin01@test.com', '13800000001', 2, 1)
ON DUPLICATE KEY UPDATE password = VALUES(password), status = VALUES(status);

-- ======================== content 库 ========================
CREATE DATABASE IF NOT EXISTS contentgenius_content DEFAULT CHARSET utf8mb4;
USE contentgenius_content;

CREATE TABLE IF NOT EXISTS project (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS content_version (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id  BIGINT       NOT NULL,
    version_no  INT          NOT NULL,
    title       VARCHAR(256) DEFAULT NULL,
    content     MEDIUMTEXT,
    platform    VARCHAR(32)  DEFAULT NULL,
    source      VARCHAR(32)  NOT NULL DEFAULT 'manual',
    status      TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_version (project_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    platform    VARCHAR(32)  NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    prompt_hint VARCHAR(512) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO template (id, code, name, platform, description, prompt_hint, status) VALUES
(1, 'tpl_xhs_default', '小红书默认', 'xiaohongshu', '种草笔记',
 '口语化、分段、适量 emoji，种草笔记风格', 1),
(2, 'tpl_wechat_default', '公众号默认', 'wechat', '长文',
 '标题吸引人、小标题清晰、结尾引导互动，公众号长文风格', 1)
ON DUPLICATE KEY UPDATE prompt_hint = VALUES(prompt_hint);

INSERT INTO project (id, user_id, title, description, status) VALUES
(1, 1, '618防晒种草', '演示项目', 1)
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO content_version (id, project_id, version_no, title, content, platform, source, status) VALUES
(1, 1, 1, '防晒示例稿', '这是示例正文。', 'xiaohongshu', 'manual', 0)
ON DUPLICATE KEY UPDATE content = VALUES(content);
