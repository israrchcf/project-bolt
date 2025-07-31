const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { validationResult } = require('express-validator');
const database = require('../config/database');

/**
 * Authentication middleware for API and admin access
 */

class AuthMiddleware {
    
    /**
     * Validate API key for device requests
     */
    async validateApiKey(req, res, next) {
        try {
            const apiKey = req.headers['x-api-key'] || req.query.api_key;
            const expectedApiKey = process.env.API_KEY || 'rdm-client-key-2024';
            
            if (!apiKey) {
                return res.status(401).json({
                    success: false,
                    message: 'API key is required',
                    error_code: 'MISSING_API_KEY'
                });
            }
            
            if (apiKey !== expectedApiKey) {
                console.warn(`❌ Invalid API key attempt: ${apiKey}`);
                return res.status(401).json({
                    success: false,
                    message: 'Invalid API key',
                    error_code: 'INVALID_API_KEY'
                });
            }
            
            next();
        } catch (error) {
            console.error('❌ API key validation error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }
    
    /**
     * Admin login
     */
    async login(req, res) {
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
            
            const { username, password } = req.body;
            
            // Find user
            const user = await database.get(
                'SELECT id, username, password_hash, is_active FROM admin_users WHERE username = ?',
                [username]
            );
            
            if (!user) {
                return res.status(401).json({
                    success: false,
                    message: 'Invalid credentials'
                });
            }
            
            if (!user.is_active) {
                return res.status(401).json({
                    success: false,
                    message: 'Account is disabled'
                });
            }
            
            // Verify password
            const isValidPassword = await bcrypt.compare(password, user.password_hash);
            if (!isValidPassword) {
                return res.status(401).json({
                    success: false,
                    message: 'Invalid credentials'
                });
            }
            
            // Generate JWT token
            const token = jwt.sign(
                { 
                    id: user.id, 
                    username: user.username 
                },
                process.env.JWT_SECRET || 'your-secret-key',
                { 
                    expiresIn: '24h' 
                }
            );
            
            // Update last login
            await database.execute(
                `UPDATE admin_users SET last_login = ${database.dbType === 'mysql' ? 'CURRENT_TIMESTAMP' : 'CURRENT_TIMESTAMP'} WHERE id = ?`,
                [user.id]
            );
            
            console.log(`✅ Admin login: ${username}`);
            
            res.json({
                success: true,
                message: 'Login successful',
                data: {
                    token,
                    user: {
                        id: user.id,
                        username: user.username
                    }
                }
            });
            
        } catch (error) {
            console.error('❌ Login error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }
    
    /**
     * Require authentication for admin routes
     */
    async requireAuth(req, res, next) {
        try {
            const token = req.headers.authorization?.replace('Bearer ', '') || 
                         req.cookies?.token ||
                         req.query.token;
            
            if (!token) {
                return res.status(401).json({
                    success: false,
                    message: 'Authentication required'
                });
            }
            
            // Verify JWT token
            const decoded = jwt.verify(token, process.env.JWT_SECRET || 'your-secret-key');
            
            // Get user from database
            const user = await database.get(
                'SELECT id, username, is_active FROM admin_users WHERE id = ?',
                [decoded.id]
            );
            
            if (!user || !user.is_active) {
                return res.status(401).json({
                    success: false,
                    message: 'Invalid or expired token'
                });
            }
            
            // Add user to request
            req.user = user;
            next();
            
        } catch (error) {
            if (error.name === 'JsonWebTokenError') {
                return res.status(401).json({
                    success: false,
                    message: 'Invalid token'
                });
            }
            
            if (error.name === 'TokenExpiredError') {
                return res.status(401).json({
                    success: false,
                    message: 'Token expired'
                });
            }
            
            console.error('❌ Auth middleware error:', error);
            return res.status(500).json({
                success: false,
                message: 'Internal server error'
            });
        }
    }
    
    /**
     * Optional authentication (for routes that work with or without auth)
     */
    async optionalAuth(req, res, next) {
        try {
            const token = req.headers.authorization?.replace('Bearer ', '') || 
                         req.cookies?.token;
            
            if (token) {
                const decoded = jwt.verify(token, process.env.JWT_SECRET || 'your-secret-key');
                const user = await database.get(
                    'SELECT id, username, is_active FROM admin_users WHERE id = ?',
                    [decoded.id]
                );
                
                if (user && user.is_active) {
                    req.user = user;
                }
            }
            
            next();
        } catch (error) {
            // Ignore auth errors for optional auth
            next();
        }
    }
    
    /**
     * Rate limiting for sensitive endpoints
     */
    createRateLimit(windowMs = 15 * 60 * 1000, max = 5) {
        const rateLimit = require('express-rate-limit');
        
        return rateLimit({
            windowMs,
            max,
            message: {
                success: false,
                message: 'Too many requests, please try again later'
            },
            standardHeaders: true,
            legacyHeaders: false,
        });
    }
}

module.exports = new AuthMiddleware();