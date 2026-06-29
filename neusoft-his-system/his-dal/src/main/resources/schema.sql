CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    name VARCHAR(64) NOT NULL,
    enabled CHAR(1) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS patient (
    id BIGINT PRIMARY KEY,
    patient_no VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    gender VARCHAR(16),
    birthday DATE,
    phone VARCHAR(32),
    id_card VARCHAR(32),
    balance DECIMAL(12,2) DEFAULT 0.00,
    current_status VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS department (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    code VARCHAR(64),
    location VARCHAR(128),
    description VARCHAR(255),
    sort_no INT,
    enabled INT,
    created_at DATETIME,
    updated_at DATETIME
);

INSERT IGNORE INTO department (id, name, code, location, description, sort_no, enabled, created_at)
VALUES
    (910000000000000001, '心血管内科', 'CARDIOLOGY', '门诊二楼', '心血管疾病门诊诊疗科室', 10, 1, NOW()),
    (910000000000000002, '儿科', 'PEDIATRICS', '门诊一楼', '儿童常见病与专科门诊', 20, 1, NOW()),
    (910000000000000003, '消化内科', 'GASTROENTEROLOGY', '门诊二楼', '消化系统疾病门诊诊疗科室', 30, 1, NOW()),
    (910000000000000004, '普外科', 'GENERAL_SURGERY', '门诊三楼', '普通外科门诊诊疗科室', 40, 1, NOW());

CREATE TABLE IF NOT EXISTS outpatient_registration (
    id BIGINT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT,
    department VARCHAR(64),
    schedule_date VARCHAR(32),
    status VARCHAR(32),
    fee DECIMAL(12,2),
    paid CHAR(1),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS doctor_profile (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(64) NOT NULL,
    department VARCHAR(64),
    title VARCHAR(64),
    specialty VARCHAR(255),
    attendance_status VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS doctor_schedule (
    id BIGINT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    schedule_date VARCHAR(32) NOT NULL,
    shift VARCHAR(32) NOT NULL,
    level VARCHAR(32),
    registration_limit INT,
    status INT,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS doctor_leave_application (
    id BIGINT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    user_id BIGINT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(32),
    previous_status VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS medical_record (
    id BIGINT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    diagnosis TEXT,
    treatment_plan TEXT,
    archive_flag CHAR(1),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS prescription (
    id BIGINT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    type VARCHAR(32),
    drug_items TEXT,
    check_items TEXT,
    total_amount DECIMAL(12,2),
    paid CHAR(1),
    audit_status VARCHAR(32),
    dispense_status VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS drug_catalog (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    unit VARCHAR(32),
    price DECIMAL(12,2),
    stock INT,
    warning_threshold INT,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    contact VARCHAR(64),
    phone VARCHAR(32),
    qualification VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS billing_record (
    id BIGINT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    billing_type VARCHAR(32),
    amount DECIMAL(12,2) NOT NULL,
    pay_channel VARCHAR(32),
    settlement_type VARCHAR(32),
    status VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS financial_transaction (
    id BIGINT PRIMARY KEY,
    biz_type VARCHAR(64),
    amount DECIMAL(12,2),
    direction VARCHAR(8),
    status VARCHAR(32),
    remark VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME
);
-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role_code)
);

-- 角色权限矩阵配置表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_code VARCHAR(32) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_code, permission_code)
);

-- 系统审计日志持久化表
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT PRIMARY KEY,
    time DATETIME,
    username VARCHAR(64),
    operation VARCHAR(64),
    detail TEXT
);
