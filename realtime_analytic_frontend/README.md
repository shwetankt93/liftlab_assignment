# Real-Time Analytics Frontend

A single-page React application displaying real-time analytics metrics.

## Features

- **Active Users**: Shows count of unique users with events in the last 5 minutes
- **Top Pages**: Displays a bar chart of top pages by view count (last 15 minutes)
- **Active Sessions**: Shows active sessions per user (last 5 minutes)
- **Auto-refresh**: Automatically fetches new metrics every 30 seconds
- **Responsive Design**: Modern, clean UI using TailwindCSS

## Tech Stack

- React 19
- Vite
- TailwindCSS
- Axios
- Chart.js / react-chartjs-2

## Getting Started

### Prerequisites

- Node.js 16+ and npm
- Backend API running on `http://localhost:8080` (or configure via environment variable)

### Installation

```bash
npm install
```

### Development

```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## Configuration

The API base URL can be configured via environment variable:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

Create a `.env` file in the root directory or export the variable before running the app.

## Project Structure

```
src/
├── components/          # React components
│   ├── ActiveUsers.jsx
│   ├── TopPages.jsx
│   └── ActiveSessions.jsx
├── hooks/              # Custom React hooks
│   └── useMetrics.js
├── services/           # API service layer
│   └── api.js
├── App.jsx            # Main application component
├── main.jsx           # Application entry point
└── index.css          # Global styles with TailwindCSS
```

## Metrics Displayed

1. **Active Users Count**: Number of unique users with events in the last 5 minutes
2. **Top Pages**: Bar chart showing top pages by view count (last 15 minutes)
3. **Active Sessions by User**: List of users with their active session counts (last 5 minutes)

The dashboard automatically refreshes every 30 seconds to show the latest metrics.
