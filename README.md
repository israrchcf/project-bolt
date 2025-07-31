# Remote Device Manager (RDM) System

A comprehensive system for managing and monitoring Android devices remotely.

## Project Structure

```
rdm-system/
├── android-client/                 # Android Studio Project
│   ├── app/
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/com/rdm/client/
│   │   │       │   ├── MainActivity.java
│   │   │       │   ├── models/
│   │   │       │   │   ├── DeviceInfo.java
│   │   │       │   │   └── ApiResponse.java
│   │   │       │   ├── network/
│   │   │       │   │   ├── ApiService.java
│   │   │       │   │   └── RetrofitClient.java
│   │   │       │   ├── services/
│   │   │       │   │   └── DataSyncService.java
│   │   │       │   └── utils/
│   │   │       │       ├── LocationHelper.java
│   │   │       │       ├── NetworkHelper.java
│   │   │       │       └── DeviceHelper.java
│   │   │       ├── res/
│   │   │       │   ├── layout/
│   │   │       │   │   └── activity_main.xml
│   │   │       │   └── values/
│   │   │       │       └── strings.xml
│   │   │       └── AndroidManifest.xml
│   │   └── build.gradle
│   ├── build.gradle
│   └── settings.gradle
├── web-admin/                      # Node.js Backend & Dashboard
│   ├── server/
│   │   ├── config/
│   │   │   └── database.js
│   │   ├── controllers/
│   │   │   ├── deviceController.js
│   │   │   └── dashboardController.js
│   │   ├── middleware/
│   │   │   └── auth.js
│   │   ├── models/
│   │   │   ├── Device.js
│   │   │   └── DeviceLog.js
│   │   ├── routes/
│   │   │   ├── api.js
│   │   │   └── dashboard.js
│   │   └── app.js
│   ├── public/
│   │   ├── css/
│   │   │   └── dashboard.css
│   │   ├── js/
│   │   │   └── dashboard.js
│   │   └── index.html
│   ├── views/
│   │   ├── dashboard.ejs
│   │   └── device-details.ejs
│   ├── package.json
│   └── .env.example
└── database/
    └── schema.sql
```

## Setup Instructions

### 1. GitHub Repository Setup
1. Create a new GitHub repository named `rdm-system`
2. Initialize GitHub Codespace with Node.js environment
3. Clone this repository structure

### 2. Android Client Setup
1. Open Android Studio
2. Import the `android-client` folder as an existing project
3. Sync Gradle files
4. Update API endpoint in `RetrofitClient.java`

### 3. Web Admin Setup
1. Navigate to `web-admin` directory
2. Run `npm install`
3. Copy `.env.example` to `.env` and configure
4. Run `npm start`

### 4. Database Setup
1. Create MySQL database or use SQLite
2. Execute `database/schema.sql`
3. Update database configuration in `.env`

## Features

### Android Client
- Device registration with backend
- Automatic data sync every 5 minutes
- Location tracking with GPS
- Network status monitoring
- Battery level reporting
- Background service with WorkManager

### Web Admin Panel
- Real-time device monitoring
- Device logs and history
- Search and filter capabilities
- Secure API authentication
- Responsive dashboard interface

## API Endpoints

- `POST /api/register` - Register new device
- `POST /api/sync` - Sync device data
- `GET /api/devices` - Get all devices
- `GET /api/device/:id/logs` - Get device logs

## Security

- API key authentication
- Encrypted data transmission
- Input validation and sanitization
- Rate limiting on API endpoints