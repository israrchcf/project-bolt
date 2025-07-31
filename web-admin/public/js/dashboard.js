/**
 * Dashboard JavaScript functionality
 * Handles interactive features and real-time updates
 */

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Dashboard loaded');
    
    // Initialize dashboard
    initializeDashboard();
    
    // Set up auto-refresh
    setupAutoRefresh();
    
    // Set up event listeners
    setupEventListeners();
});

/**
 * Initialize dashboard components
 */
function initializeDashboard() {
    // Update timestamps to relative time
    updateTimestamps();
    
    // Add loading states
    addLoadingStates();
    
    // Initialize tooltips
    initializeTooltips();
}

/**
 * Set up auto-refresh for real-time data
 */
function setupAutoRefresh() {
    // Refresh every 30 seconds
    setInterval(() => {
        refreshStats();
        updateTimestamps();
    }, 30000);
    
    console.log('⏰ Auto-refresh enabled (30s interval)');
}

/**
 * Set up event listeners
 */
function setupEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        let searchTimeout;
        searchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                performSearch(this.value);
            }, 500);
        });
    }
    
    // Filter buttons
    const filterButtons = document.querySelectorAll('.filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const filter = this.dataset.filter;
            applyFilter(filter);
            
            // Update active state
            filterButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
        });
    });
    
    // Refresh button
    const refreshBtn = document.getElementById('refreshBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            refreshData();
        });
    }
    
    // Export button
    const exportBtn = document.getElementById('exportBtn');
    if (exportBtn) {
        exportBtn.addEventListener('click', function() {
            exportData();
        });
    }
}

/**
 * Update timestamps to relative time
 */
function updateTimestamps() {
    const timestamps = document.querySelectorAll('.timestamp');
    timestamps.forEach(element => {
        const dateStr = element.textContent.trim();
        if (dateStr && dateStr !== 'Never') {
            try {
                const date = new Date(dateStr);
                const relativeTime = getRelativeTime(date);
                element.setAttribute('title', dateStr);
                element.textContent = relativeTime;
            } catch (error) {
                console.warn('Invalid timestamp:', dateStr);
            }
        }
    });
}

/**
 * Get relative time string
 */
function getRelativeTime(date) {
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffMins < 1) {
        return 'Just now';
    } else if (diffMins < 60) {
        return `${diffMins}m ago`;
    } else if (diffHours < 24) {
        return `${diffHours}h ago`;
    } else if (diffDays < 7) {
        return `${diffDays}d ago`;
    } else {
        return date.toLocaleDateString();
    }
}

/**
 * Refresh dashboard statistics
 */
async function refreshStats() {
    try {
        const response = await fetch('/api/stats', {
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            updateStatsDisplay(data.data);
        }
    } catch (error) {
        console.error('Failed to refresh stats:', error);
    }
}

/**
 * Update statistics display
 */
function updateStatsDisplay(stats) {
    const elements = {
        totalDevices: document.querySelector('[data-stat="total-devices"]'),
        activeDevices: document.querySelector('[data-stat="active-devices"]'),
        logsToday: document.querySelector('[data-stat="logs-today"]'),
        newDevicesToday: document.querySelector('[data-stat="new-devices-today"]')
    };
    
    if (elements.totalDevices) {
        elements.totalDevices.textContent = stats.total_devices || 0;
    }
    if (elements.activeDevices) {
        elements.activeDevices.textContent = stats.active_devices || 0;
    }
    if (elements.logsToday) {
        elements.logsToday.textContent = stats.recent_logs || 0;
    }
    if (elements.newDevicesToday) {
        elements.newDevicesToday.textContent = stats.new_devices_today || 0;
    }
}

/**
 * Refresh all data
 */
function refreshData() {
    const refreshBtn = document.getElementById('refreshBtn');
    if (refreshBtn) {
        refreshBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Refreshing...';
        refreshBtn.disabled = true;
    }
    
    // Refresh stats
    refreshStats().then(() => {
        // Refresh page after a short delay
        setTimeout(() => {
            window.location.reload();
        }, 1000);
    });
}

/**
 * Export data functionality
 */
function exportData() {
    const exportBtn = document.getElementById('exportBtn');
    if (exportBtn) {
        exportBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Exporting...';
        exportBtn.disabled = true;
    }
    
    // Create export request
    const exportUrl = '/api/export?' + new URLSearchParams({
        format: 'csv',
        type: 'devices'
    });
    
    // Create download link
    const link = document.createElement('a');
    link.href = exportUrl;
    link.download = `rdm-devices-${new Date().toISOString().split('T')[0]}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    // Reset button
    setTimeout(() => {
        if (exportBtn) {
            exportBtn.innerHTML = '<i class="fas fa-download"></i> Export Data';
            exportBtn.disabled = false;
        }
    }, 2000);
}

/**
 * Perform search
 */
function performSearch(query) {
    const currentUrl = new URL(window.location);
    if (query.trim()) {
        currentUrl.searchParams.set('search', query);
    } else {
        currentUrl.searchParams.delete('search');
    }
    currentUrl.searchParams.delete('page'); // Reset to first page
    window.location.href = currentUrl.toString();
}

/**
 * Apply filter
 */
function applyFilter(filter) {
    const currentUrl = new URL(window.location);
    if (filter && filter !== 'all') {
        currentUrl.searchParams.set('status', filter);
    } else {
        currentUrl.searchParams.delete('status');
    }
    currentUrl.searchParams.delete('page'); // Reset to first page
    window.location.href = currentUrl.toString();
}

/**
 * Add loading states to interactive elements
 */
function addLoadingStates() {
    const buttons = document.querySelectorAll('.btn');
    buttons.forEach(btn => {
        btn.addEventListener('click', function() {
            if (!this.disabled && !this.classList.contains('no-loading')) {
                const originalText = this.innerHTML;
                this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading...';
                this.disabled = true;
                
                // Reset after 5 seconds (fallback)
                setTimeout(() => {
                    this.innerHTML = originalText;
                    this.disabled = false;
                }, 5000);
            }
        });
    });
}

/**
 * Initialize tooltips
 */
function initializeTooltips() {
    const elementsWithTooltips = document.querySelectorAll('[title]');
    elementsWithTooltips.forEach(element => {
        element.addEventListener('mouseenter', showTooltip);
        element.addEventListener('mouseleave', hideTooltip);
    });
}

/**
 * Show tooltip
 */
function showTooltip(event) {
    const element = event.target;
    const title = element.getAttribute('title');
    
    if (title) {
        const tooltip = document.createElement('div');
        tooltip.className = 'tooltip';
        tooltip.textContent = title;
        tooltip.style.cssText = `
            position: absolute;
            background: #333;
            color: white;
            padding: 0.5rem;
            border-radius: 4px;
            font-size: 0.875rem;
            z-index: 1000;
            pointer-events: none;
            white-space: nowrap;
        `;
        
        document.body.appendChild(tooltip);
        
        const rect = element.getBoundingClientRect();
        tooltip.style.left = rect.left + (rect.width / 2) - (tooltip.offsetWidth / 2) + 'px';
        tooltip.style.top = rect.top - tooltip.offsetHeight - 5 + 'px';
        
        element._tooltip = tooltip;
        element.removeAttribute('title');
        element._originalTitle = title;
    }
}

/**
 * Hide tooltip
 */
function hideTooltip(event) {
    const element = event.target;
    if (element._tooltip) {
        document.body.removeChild(element._tooltip);
        element._tooltip = null;
        if (element._originalTitle) {
            element.setAttribute('title', element._originalTitle);
        }
    }
}

/**
 * Get authentication token
 */
function getAuthToken() {
    // Try to get token from localStorage or cookie
    return localStorage.getItem('authToken') || getCookie('token') || '';
}

/**
 * Get cookie value
 */
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

/**
 * Show notification
 */
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
        <span>${message}</span>
        <button class="notification-close">&times;</button>
    `;
    
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'success' ? '#d4edda' : type === 'error' ? '#f8d7da' : '#d1ecf1'};
        color: ${type === 'success' ? '#155724' : type === 'error' ? '#721c24' : '#0c5460'};
        padding: 1rem;
        border-radius: 5px;
        border-left: 4px solid ${type === 'success' ? '#28a745' : type === 'error' ? '#dc3545' : '#17a2b8'};
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        z-index: 1001;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        max-width: 400px;
    `;
    
    document.body.appendChild(notification);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 5000);
    
    // Close button
    const closeBtn = notification.querySelector('.notification-close');
    closeBtn.addEventListener('click', () => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    });
}

// Global functions for inline event handlers
window.refreshData = refreshData;
window.exportData = exportData;