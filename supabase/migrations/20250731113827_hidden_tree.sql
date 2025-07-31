-- Remote Device Manager Database Schema
-- Compatible with both MySQL and SQLite

-- Create devices table
CREATE TABLE IF NOT EXISTS devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT UNIQUE NOT NULL,
    model TEXT NOT NULL,
    manufacturer TEXT NOT NULL,
    os_version TEXT,
    app_version TEXT,
    first_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create device_logs table
CREATE TABLE IF NOT EXISTS device_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    battery_level INTEGER,
    network_status TEXT,
    latitude REAL,
    longitude REAL,
    local_ip TEXT,
    public_ip TEXT,
    timestamp INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
);

-- Create admin_users table
CREATE TABLE IF NOT EXISTS admin_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    email TEXT,
    is_active BOOLEAN DEFAULT 1,
    last_login DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices(device_id);
CREATE INDEX IF NOT EXISTS idx_devices_last_seen ON devices(last_seen);
CREATE INDEX IF NOT EXISTS idx_devices_is_active ON devices(is_active);

CREATE INDEX IF NOT EXISTS idx_device_logs_device_id ON device_logs(device_id);
CREATE INDEX IF NOT EXISTS idx_device_logs_timestamp ON device_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_device_logs_created_at ON device_logs(created_at);

CREATE INDEX IF NOT EXISTS idx_admin_users_username ON admin_users(username);

-- Insert default admin user (password: admin123)
-- Note: In production, this should be changed immediately
INSERT OR IGNORE INTO admin_users (username, password_hash) 
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi');

-- Sample data for testing (optional)
-- Uncomment the following lines to insert sample data

/*
-- Sample devices
INSERT OR IGNORE INTO devices (device_id, model, manufacturer, os_version, app_version) VALUES
('sample_device_001', 'Galaxy S21', 'Samsung', 'Android 12 (API 31)', '1.0 (1)'),
('sample_device_002', 'Pixel 6', 'Google', 'Android 13 (API 33)', '1.0 (1)'),
('sample_device_003', 'iPhone 13', 'Apple', 'iOS 15.0', '1.0 (1)');

-- Sample device logs
INSERT OR IGNORE INTO device_logs (device_id, battery_level, network_status, latitude, longitude, local_ip, public_ip, timestamp) VALUES
('sample_device_001', 85, 'WiFi Connected', 37.7749, -122.4194, '192.168.1.100', '203.0.113.1', 1640995200000),
('sample_device_002', 92, 'Mobile Data', 40.7128, -74.0060, '192.168.1.101', '203.0.113.2', 1640995260000),
('sample_device_003', 78, 'WiFi Connected', 34.0522, -118.2437, '192.168.1.102', '203.0.113.3', 1640995320000);
*/