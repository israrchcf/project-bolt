const express = require('express');
const authMiddleware = require('../middleware/auth');
const deviceController = require('../controllers/deviceController');

const router = express.Router();

/**
 * Dashboard routes for web interface
 */

/**
 * @route GET /
 * @desc Dashboard home page
 */
router.get('/', authMiddleware.optionalAuth, async (req, res) => {
    try {
        if (!req.user) {
            return res.render('login', {
                title: 'RDM Admin - Login',
                error: null
            });
        }
        
        // Get dashboard statistics
        const stats = await getStats();
        const recentDevices = await getRecentDevices();
        
        res.render('dashboard', {
            title: 'RDM Admin Dashboard',
            user: req.user,
            stats,
            recentDevices
        });
    } catch (error) {
        console.error('❌ Dashboard error:', error);
        res.status(500).render('error', {
            title: 'Error',
            message: 'Internal server error'
        });
    }
});

/**
 * @route GET /login
 * @desc Login page
 */
router.get('/login', (req, res) => {
    res.render('login', {
        title: 'RDM Admin - Login',
        error: null
    });
});

/**
 * @route POST /login
 * @desc Process login
 */
router.post('/login', async (req, res) => {
    try {
        const { username, password } = req.body;
        
        // Use the auth middleware login function
        const mockReq = { body: { username, password } };
        const mockRes = {
            status: (code) => mockRes,
            json: (data) => {
                if (data.success) {
                    // Set cookie and redirect
                    res.cookie('token', data.data.token, {
                        httpOnly: true,
                        secure: process.env.NODE_ENV === 'production',
                        maxAge: 24 * 60 * 60 * 1000 // 24 hours
                    });
                    res.redirect('/');
                } else {
                    res.render('login', {
                        title: 'RDM Admin - Login',
                        error: data.message
                    });
                }
            }
        };
        
        await authMiddleware.login(mockReq, mockRes);
    } catch (error) {
        console.error('❌ Login error:', error);
        res.render('login', {
            title: 'RDM Admin - Login',
            error: 'Login failed. Please try again.'
        });
    }
});

/**
 * @route GET /logout
 * @desc Logout
 */
router.get('/logout', (req, res) => {
    res.clearCookie('token');
    res.redirect('/login');
});

/**
 * @route GET /devices
 * @desc Devices list page
 */
router.get('/devices', authMiddleware.requireAuth, async (req, res) => {
    try {
        const { page = 1, search = '', status = 'all' } = req.query;
        
        // Get devices data
        const mockReq = { query: { page, search, status, limit: 20 } };
        const mockRes = {
            json: (data) => {
                res.render('devices', {
                    title: 'Devices - RDM Admin',
                    user: req.user,
                    devices: data.data.devices,
                    pagination: data.data.pagination,
                    search,
                    status
                });
            },
            status: (code) => mockRes
        };
        
        await deviceController.getAllDevices(mockReq, mockRes);
    } catch (error) {
        console.error('❌ Devices page error:', error);
        res.status(500).render('error', {
            title: 'Error',
            message: 'Failed to load devices'
        });
    }
});

/**
 * @route GET /devices/:deviceId
 * @desc Device details page
 */
router.get('/devices/:deviceId', authMiddleware.requireAuth, async (req, res) => {
    try {
        const { deviceId } = req.params;
        const { page = 1 } = req.query;
        
        // Get device details
        const mockReq = { params: { deviceId }, query: { page, limit: 50 } };
        const mockRes = {
            json: (data) => {
                res.render('device-details', {
                    title: `Device ${deviceId} - RDM Admin`,
                    user: req.user,
                    device: data.data.device,
                    logs: data.data.logs,
                    pagination: data.data.pagination
                });
            },
            status: (code) => {
                if (code === 404) {
                    res.status(404).render('error', {
                        title: 'Device Not Found',
                        message: 'The requested device was not found'
                    });
                } else {
                    mockRes;
                }
            }
        };
        
        await deviceController.getDeviceDetails(mockReq, mockRes);
    } catch (error) {
        console.error('❌ Device details error:', error);
        res.status(500).render('error', {
            title: 'Error',
            message: 'Failed to load device details'
        });
    }
});

/**
 * Helper function to get dashboard statistics
 */
async function getStats() {
    try {
        const database = require('../config/database');
        
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
        
        // Total logs today
        const logsToday = await database.get(`
            SELECT COUNT(*) as count FROM device_logs 
            WHERE ${database.dbType === 'mysql' ? 
                'DATE(created_at) = CURDATE()' : 
                'DATE(created_at) = DATE("now")'
            }
        `);
        
        // New devices today
        const newDevicesToday = await database.get(`
            SELECT COUNT(*) as count FROM devices 
            WHERE ${database.dbType === 'mysql' ? 
                'DATE(created_at) = CURDATE()' : 
                'DATE(created_at) = DATE("now")'
            }
        `);
        
        return {
            totalDevices: totalDevices.count,
            activeDevices: activeDevices.count,
            logsToday: logsToday.count,
            newDevicesToday: newDevicesToday.count
        };
    } catch (error) {
        console.error('❌ Get stats error:', error);
        return {
            totalDevices: 0,
            activeDevices: 0,
            logsToday: 0,
            newDevicesToday: 0
        };
    }
}

/**
 * Helper function to get recent devices
 */
async function getRecentDevices() {
    try {
        const database = require('../config/database');
        
        const devices = await database.all(`
            SELECT device_id, model, manufacturer, last_seen, is_active
            FROM devices 
            ORDER BY last_seen DESC 
            LIMIT 10
        `);
        
        return devices;
    } catch (error) {
        console.error('❌ Get recent devices error:', error);
        return [];
    }
}

module.exports = router;