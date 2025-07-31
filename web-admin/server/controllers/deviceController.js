const database = require('../config/database');
const { validationResult } = require('express-validator');

/**
 * Device Controller
 * Handles device registration, data sync, and device management
 */

class DeviceController {
    
    /**
     * Register a new device
     */
    async registerDevice(req, res) {
        try {
            // Validate request
            const errors = validationResult(req);
            if (!errors.isEmpty()) {
                return res.status(400).json({
                    success: false,
                    message: 'Validation failed',
                    errors: errors.array()
                });
            }

            const {
                device_id,
                model,
                manufacturer,
                os_version,
                app_version
            } = req.body;

            // Check if device already exists
            const existingDevice = await database.get(
                'SELECT id FROM devices WHERE device_id = ?',
                [device_id]
            );

            if (existingDevice) {
                // Update existing device
                await database.execute(`
                    UPDATE devices 
                    SET model = ?, manufacturer = ?, os_version = ?, app_version = ?, 
                        last_seen = ${database.dbType === 'mysql' ? 'CURRENT_TIMESTAMP' : 'CURRENT_TIMESTAMP'}, 
                        is_active = ${database.dbType === 'mysql' ? 'TRUE' : '1'}
                    WHERE device_id = ?
                `, [model, manufacturer, os_version, app_version, device_id]);

                console.log(`📱 Device updated: ${device_id}`);
                return res.json({
                    success: true,
                    message: 'Device updated successfully',
                    data: { device_id, status: 'updated' }
                });
            } else {
                // Register new device
                await database.execute(`
                    INSERT INTO devices (device_id, model, manufacturer, os_version, app_version)
                    VALUES (?, ?, ?, ?, ?)
                `, [device_id, model, manufacturer, os_version, app_version]);

                console.log(`📱 New device registered: ${device_id}`);
                return res.status(201).json({
                    success: true,
                    message: 'Device registered successfully',
                    data: { device_id, status: 'registered' }
                });
            }

        } catch (error) {
            console.error('❌ Device registration error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error',
                error_code: 'REGISTRATION_FAILED'
            });
        }
    }

    /**
     * Sync device data
     */
    async syncDeviceData(req, res) {
        try {
            // Validate request
            const errors = validationResult(req);
            if (!errors.isEmpty()) {
                return res.status(400).json({
                    success: false,
                    message: 'Validation failed',
                    errors: errors.array()
                });
            }

            const {
                device_id,
                battery_level,
                network_status,
                latitude,
                longitude,
                local_ip,
                public_ip,
                timestamp
            } = req.body;

            // Verify device exists
            const device = await database.get(
                'SELECT id FROM devices WHERE device_id = ?',
                [device_id]
            );

            if (!device) {
                return res.status(404).json({
                    success: false,
                    message: 'Device not found. Please register first.',
                    error_code: 'DEVICE_NOT_FOUND'
                });
            }

            // Insert device log
            await database.execute(`
                INSERT INTO device_logs 
                (device_id, battery_level, network_status, latitude, longitude, local_ip, public_ip, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            `, [device_id, battery_level, network_status, latitude, longitude, local_ip, public_ip, timestamp]);

            // Update device last_seen
            await database.execute(`
                UPDATE devices 
                SET last_seen = ${database.dbType === 'mysql' ? 'CURRENT_TIMESTAMP' : 'CURRENT_TIMESTAMP'}, 
                    is_active = ${database.dbType === 'mysql' ? 'TRUE' : '1'}
                WHERE device_id = ?
            `, [device_id]);

            console.log(`🔄 Data synced for device: ${device_id}`);
            return res.json({
                success: true,
                message: 'Data synced successfully',
                data: {
                    device_id,
                    timestamp: Date.now(),
                    sync_status: 'completed'
                }
            });

        } catch (error) {
            console.error('❌ Data sync error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error',
                error_code: 'SYNC_FAILED'
            });
        }
    }

    /**
     * Get all devices
     */
    async getAllDevices(req, res) {
        try {
            const { page = 1, limit = 50, search = '', status = 'all' } = req.query;
            const offset = (page - 1) * limit;

            let whereClause = '';
            let params = [];

            // Build search conditions
            if (search) {
                whereClause += ' WHERE (device_id LIKE ? OR model LIKE ? OR manufacturer LIKE ?)';
                params.push(`%${search}%`, `%${search}%`, `%${search}%`);
            }

            // Build status filter
            if (status !== 'all') {
                const statusCondition = status === 'active' ? 
                    (database.dbType === 'mysql' ? 'is_active = TRUE' : 'is_active = 1') :
                    (database.dbType === 'mysql' ? 'is_active = FALSE' : 'is_active = 0');
                
                whereClause += whereClause ? ` AND ${statusCondition}` : ` WHERE ${statusCondition}`;
            }

            // Get total count
            const countQuery = `SELECT COUNT(*) as total FROM devices${whereClause}`;
            const countResult = await database.get(countQuery, params);
            const total = countResult.total;

            // Get devices with pagination
            const devicesQuery = `
                SELECT 
                    id, device_id, model, manufacturer, os_version, app_version,
                    first_seen, last_seen, is_active, created_at, updated_at
                FROM devices
                ${whereClause}
                ORDER BY last_seen DESC
                LIMIT ? OFFSET ?
            `;
            
            const devices = await database.all(devicesQuery, [...params, parseInt(limit), parseInt(offset)]);

            // Get latest log data for each device
            const devicesWithLogs = await Promise.all(devices.map(async (device) => {
                const latestLog = await database.get(`
                    SELECT battery_level, network_status, latitude, longitude, 
                           local_ip, public_ip, timestamp
                    FROM device_logs 
                    WHERE device_id = ? 
                    ORDER BY timestamp DESC 
                    LIMIT 1
                `, [device.device_id]);

                return {
                    ...device,
                    latest_log: latestLog
                };
            }));

            return res.json({
                success: true,
                data: {
                    devices: devicesWithLogs,
                    pagination: {
                        page: parseInt(page),
                        limit: parseInt(limit),
                        total,
                        pages: Math.ceil(total / limit)
                    }
                }
            });

        } catch (error) {
            console.error('❌ Get devices error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }

    /**
     * Get device details
     */
    async getDeviceDetails(req, res) {
        try {
            const { deviceId } = req.params;

            // Get device info
            const device = await database.get(
                'SELECT * FROM devices WHERE device_id = ?',
                [deviceId]
            );

            if (!device) {
                return res.status(404).json({
                    success: false,
                    message: 'Device not found'
                });
            }

            // Get device logs with pagination
            const { page = 1, limit = 100 } = req.query;
            const offset = (page - 1) * limit;

            const logs = await database.all(`
                SELECT * FROM device_logs 
                WHERE device_id = ? 
                ORDER BY timestamp DESC 
                LIMIT ? OFFSET ?
            `, [deviceId, parseInt(limit), parseInt(offset)]);

            // Get total log count
            const logCount = await database.get(
                'SELECT COUNT(*) as total FROM device_logs WHERE device_id = ?',
                [deviceId]
            );

            return res.json({
                success: true,
                data: {
                    device,
                    logs,
                    pagination: {
                        page: parseInt(page),
                        limit: parseInt(limit),
                        total: logCount.total,
                        pages: Math.ceil(logCount.total / limit)
                    }
                }
            });

        } catch (error) {
            console.error('❌ Get device details error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }

    /**
     * Delete device
     */
    async deleteDevice(req, res) {
        try {
            const { deviceId } = req.params;

            // Check if device exists
            const device = await database.get(
                'SELECT id FROM devices WHERE device_id = ?',
                [deviceId]
            );

            if (!device) {
                return res.status(404).json({
                    success: false,
                    message: 'Device not found'
                });
            }

            // Delete device (logs will be deleted by CASCADE)
            await database.execute(
                'DELETE FROM devices WHERE device_id = ?',
                [deviceId]
            );

            console.log(`🗑️ Device deleted: ${deviceId}`);
            return res.json({
                success: true,
                message: 'Device deleted successfully'
            });

        } catch (error) {
            console.error('❌ Delete device error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }

    /**
     * Get device statistics
     */
    async getDeviceStats(req, res) {
        try {
            // Total devices
            const totalDevices = await database.get('SELECT COUNT(*) as count FROM devices');
            
            // Active devices (seen in last 24 hours)
            const activeDevices = await database.get(`
                SELECT COUNT(*) as count FROM devices 
                WHERE ${database.dbType === 'mysql' ? 
                    'last_seen >= DATE_SUB(NOW(), INTERVAL 24 HOUR)' : 
                    'last_seen >= datetime("now", "-24 hours")'
                }
            `);

            // Total logs
            const totalLogs = await database.get('SELECT COUNT(*) as count FROM device_logs');

            // Recent logs (last 24 hours)
            const recentLogs = await database.get(`
                SELECT COUNT(*) as count FROM device_logs 
                WHERE ${database.dbType === 'mysql' ? 
                    'created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)' : 
                    'created_at >= datetime("now", "-24 hours")'
                }
            `);

            // Device by manufacturer
            const devicesByManufacturer = await database.all(`
                SELECT manufacturer, COUNT(*) as count 
                FROM devices 
                GROUP BY manufacturer 
                ORDER BY count DESC 
                LIMIT 10
            `);

            return res.json({
                success: true,
                data: {
                    total_devices: totalDevices.count,
                    active_devices: activeDevices.count,
                    total_logs: totalLogs.count,
                    recent_logs: recentLogs.count,
                    devices_by_manufacturer: devicesByManufacturer
                }
            });

        } catch (error) {
            console.error('❌ Get device stats error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }
}

module.exports = new DeviceController();