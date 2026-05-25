-- Inventory Management Phase 2 SQL Updates
-- Project M5zany - Inventory Orchestration, Cycle Counting, and Resource Tracking

USE smartstock_erp;

-- ============================================
-- 1. Update inventory table to add expiry_date
-- ============================================
ALTER TABLE inventory 
ADD COLUMN expiry_date DATE DEFAULT NULL AFTER quantity;

-- Add last_updated timestamp to match CSV structure
ALTER TABLE inventory 
ADD COLUMN last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER expiry_date;

-- ============================================
-- 2. Update stock_movements table to match CSV structure
-- ============================================
-- Add reference_id column
ALTER TABLE stock_movements 
ADD COLUMN reference_id INT DEFAULT NULL AFTER quantity;

-- Rename cashier_id to moved_by to match CSV structure
ALTER TABLE stock_movements 
CHANGE COLUMN cashier_id moved_by INT DEFAULT NULL;

-- Rename created_at to moved_at to match CSV structure
ALTER TABLE stock_movements 
CHANGE COLUMN created_at moved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ============================================
-- 3. Update cycle_counts table to match cycle_count_log.csv
-- ============================================
-- Rename table to cycle_count_log to match CSV
RENAME TABLE cycle_counts TO cycle_count_log;

-- Rename columns to match CSV structure
ALTER TABLE cycle_count_log 
CHANGE COLUMN id count_id INT AUTO_INCREMENT;

ALTER TABLE cycle_count_log 
CHANGE COLUMN expected_qty expected_qty INT DEFAULT 0;

ALTER TABLE cycle_count_log 
CHANGE COLUMN actual_qty counted_qty INT DEFAULT 0;

ALTER TABLE cycle_count_log 
CHANGE COLUMN variance discrepancy INT DEFAULT 0;

-- Add notes column if not exists (it should exist but checking)
ALTER TABLE cycle_count_log 
ADD COLUMN notes TEXT DEFAULT NULL AFTER discrepancy;

-- ============================================
-- 4. Create indexes for better performance
-- ============================================
CREATE INDEX idx_inventory_product_branch ON inventory(product_id, branch_id);
CREATE INDEX idx_inventory_expiry ON inventory(expiry_date);
CREATE INDEX idx_stock_movements_branch_type ON stock_movements(branch_id, movement_type);
CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX idx_cycle_count_log_branch_date ON cycle_count_log(branch_id, count_date);
CREATE INDEX idx_cycle_count_log_discrepancy ON cycle_count_log(discrepancy);

-- ============================================
-- 5. Add low_stock_threshold configuration table
-- ============================================
CREATE TABLE IF NOT EXISTS inventory_config (
    id INT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(50) UNIQUE NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default configuration values
INSERT INTO inventory_config (config_key, config_value, description) VALUES
('low_stock_threshold', '10', 'Minimum quantity threshold for low stock alerts'),
('expiry_alert_days', '7', 'Days before expiry to trigger alert'),
('orchestration_enabled', 'true', 'Enable inventory orchestration between branches'),
('cycle_count_daily_products', '20', 'Number of products to suggest for daily cycle count')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ============================================
-- 6. Add audit_status column to cycle_count_log for Phase 2
-- ============================================
ALTER TABLE cycle_count_log
ADD COLUMN audit_status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'DISCREPANCY') DEFAULT 'PENDING' AFTER notes;

-- ============================================
-- 7. Add qr_code_path column to products for Phase 2 QR system
-- ============================================
ALTER TABLE products
ADD COLUMN qr_code_path VARCHAR(255) DEFAULT NULL AFTER qr_code;

-- ============================================
-- 8. Verify table structures
-- ============================================
-- Display updated inventory structure
DESCRIBE inventory;

-- Display updated stock_movements structure
DESCRIBE stock_movements;

-- Display updated cycle_count_log structure
DESCRIBE cycle_count_log;

-- Display updated products structure
DESCRIBE products;
