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
    current_status VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME
);

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

-- 系统审计日志持久化表
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT PRIMARY KEY,
    time DATETIME,
    username VARCHAR(64),
    operation VARCHAR(64),
    detail TEXT
);