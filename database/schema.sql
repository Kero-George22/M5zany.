-- SmartStock ERP Database Schema
-- Part 1: Core Infrastructure & Admin Dashboard

CREATE DATABASE IF NOT EXISTS smartstock_erp;
USE smartstock_erp;

-- Users table
CREATE TABLE users (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL DEFAULT '',
    email       VARCHAR(100) DEFAULT '',
    phone       VARCHAR(20) DEFAULT '',
    role        ENUM('ADMIN', 'MANAGER', 'CASHIER') NOT NULL,
    branch_id   INT,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Branches table
CREATE TABLE branches (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    location    VARCHAR(255),
    phone       VARCHAR(20) DEFAULT '',
    email       VARCHAR(100) DEFAULT '',
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Products table
CREATE TABLE products (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    barcode         VARCHAR(50) UNIQUE,
    qr_code         TEXT,
    category        VARCHAR(50),
    unit_cost       DECIMAL(10,2) DEFAULT 0.00,
    selling_price   DECIMAL(10,2) DEFAULT 0.00,
    expiry_date     DATE,
    branch_id       INT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);

-- Inventory table (per branch)
CREATE TABLE inventory (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    product_id  INT NOT NULL,
    branch_id   INT NOT NULL,
    quantity    INT DEFAULT 0,
    min_stock   INT DEFAULT 10,
    max_stock   INT DEFAULT 1000,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);

-- Stock movements table
CREATE TABLE stock_movements (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    product_id      INT NOT NULL,
    branch_id       INT NOT NULL,
    movement_type   ENUM('IN', 'OUT', 'TRANSFER', 'SALE') NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(10,2),
    cashier_id      INT,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE,
    FOREIGN KEY (cashier_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Cycle counts table
CREATE TABLE cycle_counts (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    product_id      INT NOT NULL,
    branch_id       INT NOT NULL,
    counted_by      INT NOT NULL,
    expected_qty    INT DEFAULT 0,
    actual_qty      INT DEFAULT 0,
    variance        INT DEFAULT 0,
    count_date      DATE NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE,
    FOREIGN KEY (counted_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Alerts table
CREATE TABLE alerts (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    type        ENUM('EXPIRY', 'LOW_STOCK', 'FAST_MOVING', 'TRANSFER', 'SYSTEM', 'CUSTOM') NOT NULL,
    product_id  INT,
    branch_id   INT,
    sender_id   INT,
    message     TEXT,
    severity    ENUM('INFO', 'WARNING', 'CRITICAL') DEFAULT 'INFO',
    is_read     BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Weekly AI summaries table
CREATE TABLE weekly_summaries (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    branch_id       INT,
    summary_text    TEXT NOT NULL,
    generated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    report_period   VARCHAR(50) NOT NULL,
    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);

-- Insert default admin user (password: admin123, BCrypt hashed)
INSERT INTO users (username, password, full_name, role, branch_id, is_active)
VALUES ('admin', '$2a$10$9mxubONxmlnpRp3jQ/hgyOL.UWL/ZgynxEWg99VyF6UA717XTev.G', 'System Admin', 'ADMIN', NULL, TRUE);

-- Insert default branch
INSERT INTO branches (name, location, phone, email) VALUES ('Main Branch', 'Cairo Downtown', '01000000000', 'main@smartstock.com');
