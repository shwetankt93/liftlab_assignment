import React from 'react';
import { useMetrics } from './hooks/useMetrics';
import ActiveUsers from './components/ActiveUsers';
import TopPages from './components/TopPages';
import ActiveSessions from './components/ActiveSessions';

function App() {
  const { metrics, loading, error, lastUpdated } = useMetrics();

  const formatLastUpdated = (date) => {
    if (!date) return '';
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit', 
      second: '2-digit' 
    });
  };

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Real-Time Analytics Dashboard</h1>
              <p className="text-sm text-gray-500 mt-1">Live metrics from your application</p>
            </div>
            <div className="text-right">
              {lastUpdated && (
                <div className="text-sm text-gray-600">
                  <span className="inline-flex items-center">
                    <span className="w-2 h-2 bg-green-500 rounded-full mr-2 animate-pulse"></span>
                    Last updated: {formatLastUpdated(lastUpdated)}
                  </span>
                </div>
              )}
              <p className="text-xs text-gray-500 mt-1">Auto-refreshes every 30 seconds</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center">
              <svg className="w-5 h-5 text-red-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-red-800 font-medium">Error loading metrics: {error}</span>
            </div>
          </div>
        )}

        {/* Metrics Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
          {/* Active Users */}
          <div className="lg:col-span-1">
            <ActiveUsers 
              count={metrics?.activeUsersCount} 
              loading={loading}
            />
          </div>

          {/* Top Pages - Takes 2 columns on large screens */}
          <div className="lg:col-span-2">
            <TopPages 
              topPages={metrics?.topPages} 
              loading={loading}
            />
          </div>
        </div>

        {/* Active Sessions */}
        <div className="grid grid-cols-1">
          <ActiveSessions 
            sessionsByUser={metrics?.activeSessionsByUser} 
            loading={loading}
          />
        </div>

        {/* Footer Info */}
        <div className="mt-8 text-center text-sm text-gray-500">
          <p>Metrics are automatically refreshed every 30 seconds</p>
          {metrics?.timestamp && (
            <p className="mt-1">
              Data timestamp: {new Date(metrics.timestamp).toLocaleString()}
            </p>
          )}
        </div>
      </main>
    </div>
  );
}

export default App;
