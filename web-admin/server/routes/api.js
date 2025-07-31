const express = require('express');
const { body } = require('express-validator');
const deviceController = require('../controllers/deviceController');
const authMiddleware = require('../middleware/auth');

const router = express.Router();

/**
 * API Routes for Remote Device Manager
 * Handles device registration, data sync, and management
 */

// Validation middleware
const deviceRegistrationValidation = [
    body('device_id').notEmpty().withMessage('Device ID is required'),
    body('model').notEmpty().withMessage('Device model is required'),
    body('manufacturer').notEmpty().withMessage('Device manufacturer is required'),
    body('os_version').optional().isString(),
    body('app_version').optional().isString()
];

const deviceSyncValidation = [
    body('device_id').notEmpty().withMessage('Device ID is required'),
    body('battery_level').optional().isInt({ min: 0, max: 100 }),
    body('network_status').optional().isString(),
    body('latitude').optional().isFloat({ min: -90, max: 90 }),
    body('longitude').optional().isFloat({ min: -180, max: 180 }),
    body('local_ip').optional().isString(),
    body('public_ip').optional().isString(),
    body('timestamp').isInt({ min: 0 }).withMessage('Valid timestamp is required')
];

// Public API endpoints (for Android clients)

/**
 * @route POST /api/register
 * @desc Register a new device
 * @access Public (with API key)
 */
router.post('/register', 
    authMiddleware.validateApiKey,
    deviceRegistrationValidation,
    deviceController.registerDevice
);

/**
 * @route POST /api/sync
 * @desc Sync device data
 * @access Public (with API key)
 */
router.post('/sync',
    authMiddleware.validateApiKey,
    deviceSyncValidation,
    deviceController.syncDeviceData
);

/**
 * @route POST /api/heartbeat
 * @desc Send device heartbeat
 * @access Public (with API key)
 */
router.post('/heartbeat',
    authMiddleware.validateApiKey,
    [body('device_id').notEmpty().withMessage('Device ID is required')],
    async (req, res) => {
        try {
            const { device_id } = req.body;
            
            // Update device last_seen
            const database = require('../config/database');
            await database.execute(`
                UPDATE devices 
                SET last_seen = ${database.dbType === 'mysql' ? 'CURRENT_TIMESTAMP' : 'CURRENT_TIMESTAMP'}
                WHERE device_id = ?
            `, [device_id]);

            res.json({
                success: true,
                message: 'Heartbeat received',
                timestamp: Date.now()
            });
        } catch (error) {
            console.error('❌ Heartbeat error:', error);
            res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }
);

// Admin API endpoints (require authentication)

/**
 * @route GET /api/devices
 * @desc Get all devices with pagination and filtering
 * @access Private (Admin only)
 */
router.get('/devices',
    authMiddleware.requireAuth,
    deviceController.getAllDevices
);

/**
 * @route GET /api/devices/:deviceId
 * @desc Get device details and logs
 * @access Private (Admin only)
 */
router.get('/devices/:deviceId',
    authMiddleware.requireAuth,
    deviceController.getDeviceDetails
);

/**
 * @route DELETE /api/devices/:deviceId
 * @desc Delete a device
 * @access Private (Admin only)
 */
router.delete('/devices/:deviceId',
    authMiddleware.requireAuth,
    deviceController.deleteDevice
);

/**
 * @route GET /api/stats
 * @desc Get device statistics
 * @access Private (Admin only)
 */
router.get('/stats',
    authMiddleware.requireAuth,
    deviceController.getDeviceStats
);

/**
 * @route POST /api/login
 * @desc Admin login
 * @access Public
 */
router.post('/login',
    [
        body('username').notEmpty().withMessage('Username is required'),
        body('password').notEmpty().withMessage('Password is required')
    ],
    authMiddleware.login
);

/**
 * @route POST /api/logout
 * @desc Admin logout
 * @access Private
 */
router.post('/logout',
    authMiddleware.requireAuth,
    (req, res) => {
        res.json({
            success: true,
            message: 'Logged out successfully'
        });
    }
);

/**
 * @route GET /api/profile
 * @desc Get admin profile
 * @access Private
 */
router.get('/profile',
    authMiddleware.requireAuth,
    async (req, res) => {
        try {
            const database = require('../config/database');
            const user = await database.get(
                'SELECT id, username, email, created_at FROM admin_users WHERE id = ?',
                [req.user.id]
            );

            res.json({
                success: true,
                data: user
            });
        } catch (error) {
            console.error('❌ Get profile error:', error);
            res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }
);

// Error handling for API routes
router.use((error, req, res, next) => {
    console.error('API Error:', error);
    
    if (error.type === 'entity.parse.failed') {
        return res.status(400).json({
            success: false,
            message: 'Invalid JSON payload'
        });
    }
    
    res.status(500).json({
        success: false,
        message: 'Internal server error',
        error_code: 'API_ERROR'
    });
});

module.exports = router;