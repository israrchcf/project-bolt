const mysql = require('mysql2/promise');
const sqlite3 = require('sqlite3').verbose();
const { promisify } = require('util');
const path = require('path');
const fs = require('fs').promises;

/**
 * Database configuration and connection management
 * Supports both MySQL and SQLite databases
 */

class Database {
    constructor() {
        this.connection = null;
        this.dbType = process.env.DB_TYPE || 'sqlite';
    }

    /**
     * Initialize database connection and create tables
     */
    async initialize() {
        try {
            if (this.dbType === 'mysql') {
                await this.initializeMySQL();
            } else {
                await this.initializeSQLite();
            }
            
            await this.createTables();
            console.log(`✅ ${this.dbType.toUpperCase()} database initialized successfully`);
        } catch (error) {
            console.error('❌ Database initialization failed:', error);
            throw error;
        }
    }

    /**
     * Initialize MySQL connection
     */
    async initializeMySQL() {
        this.connection = await mysql.createConnection({
            host: process.env.DB_HOST || 'localhost',
            port: process.env.DB_PORT || 3306,
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'rdm_database',
            charset: 'utf8mb4',
            timezone: '+00:00'
        });

        // Test connection
        await this.connection.ping();
        console.log('📡 MySQL connection established');
    }

    /**
     * Initialize SQLite connection
     */
    async initializeSQLite() {
        const dbPath = process.env.DB_PATH || path.join(__dirname, '../../database/rdm.db');
        const dbDir = path.dirname(dbPath);
        
        // Ensure database directory exists
        try {
            await fs.access(dbDir);
        } catch {
            await fs.mkdir(dbDir, { recursive: true });
        }

        return new Promise((resolve, reject) => {
            this.connection = new sqlite3.Database(dbPath, (err) => {
                if (err) {
                    reject(err);
                } else {
                    console.log('📡 SQLite connection established');
                    // Promisify SQLite methods
                    this.connection.run = promisify(this.connection.run.bind(this.connection));
                    this.connection.get = promisify(this.connection.get.bind(this.connection));
                    this.connection.all = promisify(this.connection.all.bind(this.connection));
                    resolve();
                }
            });
        });
    }

    /**
     * Create database tables if they don't exist
     */
    async createTables() {
        const tables = {
            devices: this.getDevicesTableSQL(),
            device_logs: this.getDeviceLogsTableSQL(),
            admin_users: this.getAdminUsersTableSQL()
        };

        for (const [tableName, sql] of Object.entries(tables)) {
            try {
                await this.execute(sql);
                console.log(`✅ Table '${tableName}' ready`);
            } catch (error) {
                console.error(`❌ Failed to create table '${tableName}':`, error);
                throw error;
            }
        }

        // Create default admin user
        await this.createDefaultAdmin();
    }

    /**
     * Get SQL for devices table
     */
    getDevicesTableSQL() {
        if (this.dbType === 'mysql') {
            return `
                CREATE TABLE IF NOT EXISTS devices (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    device_id VARCHAR(255) UNIQUE NOT NULL,
                    model VARCHAR(255) NOT NULL,
                    manufacturer VARCHAR(255) NOT NULL,
                    os_version VARCHAR(100),
                    app_version VARCHAR(100),
                    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_device_id (device_id),
                    INDEX idx_last_seen (last_seen),
                    INDEX idx_is_active (is_active)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            `;
        } else {
            return `
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
                )
            `;
        }
    }

    /**
     * Get SQL for device_logs table
     */
    getDeviceLogsTableSQL() {
        if (this.dbType === 'mysql') {
            return `
                CREATE TABLE IF NOT EXISTS device_logs (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    device_id VARCHAR(255) NOT NULL,
                    battery_level INT,
                    network_status VARCHAR(100),
                    latitude DECIMAL(10, 8),
                    longitude DECIMAL(11, 8),
                    local_ip VARCHAR(45),
                    public_ip VARCHAR(45),
                    timestamp BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_device_id (device_id),
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_created_at (created_at),
                    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            `;
        } else {
            return `
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
                )
            `;
        }
    }

    /**
     * Get SQL for admin_users table
     */
    getAdminUsersTableSQL() {
        if (this.dbType === 'mysql') {
            return `
                CREATE TABLE IF NOT EXISTS admin_users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    last_login TIMESTAMP NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            `;
        } else {
            return `
                CREATE TABLE IF NOT EXISTS admin_users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    email TEXT,
                    is_active BOOLEAN DEFAULT 1,
                    last_login DATETIME,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            `;
        }
    }

    /**
     * Create default admin user
     */
    async createDefaultAdmin() {
        try {
            const bcrypt = require('bcryptjs');
            const username = process.env.ADMIN_USERNAME || 'admin';
            const password = process.env.ADMIN_PASSWORD || 'admin123';
            
            // Check if admin user already exists
            const existingAdmin = await this.get(
                'SELECT id FROM admin_users WHERE username = ?',
                [username]
            );
            
            if (!existingAdmin) {
                const passwordHash = await bcrypt.hash(password, 10);
                await this.execute(
                    'INSERT INTO admin_users (username, password_hash) VALUES (?, ?)',
                    [username, passwordHash]
                );
                console.log(`✅ Default admin user created: ${username}`);
            }
        } catch (error) {
            console.error('❌ Failed to create default admin user:', error);
        }
    }

    /**
     * Execute SQL query
     */
    async execute(sql, params = []) {
        if (this.dbType === 'mysql') {
            const [result] = await this.connection.execute(sql, params);
            return result;
        } else {
            return await this.connection.run(sql, params);
        }
    }

    /**
     * Get single row
     */
    async get(sql, params = []) {
        if (this.dbType === 'mysql') {
            const [rows] = await this.connection.execute(sql, params);
            return rows[0] || null;
        } else {
            return await this.connection.get(sql, params);
        }
    }

    /**
     * Get multiple rows
     */
    async all(sql, params = []) {
        if (this.dbType === 'mysql') {
            const [rows] = await this.connection.execute(sql, params);
            return rows;
        } else {
            return await this.connection.all(sql, params);
        }
    }

    /**
     * Close database connection
     */
    async close() {
        if (this.connection) {
            if (this.dbType === 'mysql') {
                await this.connection.end();
            } else {
                await new Promise((resolve) => {
                    this.connection.close(resolve);
                });
            }
            console.log('📡 Database connection closed');
        }
    }
}

// Export singleton instance
module.exports = new Database();