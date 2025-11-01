import React from 'react';

/**
 * Component to display active sessions by user (last 5 minutes)
 */
const ActiveSessions = ({ sessionsByUser, loading }) => {
  const totalSessions = sessionsByUser 
    ? Object.values(sessionsByUser).reduce((sum, count) => sum + count, 0)
    : 0;
  const userCount = sessionsByUser ? Object.keys(sessionsByUser).length : 0;

  return (
    <div className="bg-white rounded-lg shadow-md p-6 border border-gray-200">
      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold text-gray-700">Active Sessions</h3>
          {loading ? (
            <div className="animate-pulse">
              <div className="h-6 w-20 bg-gray-200 rounded"></div>
            </div>
          ) : (
            <div className="text-2xl font-bold text-green-600">{totalSessions}</div>
          )}
        </div>
        <p className="text-sm text-gray-500 mb-4">Last 5 minutes • {userCount} active user{userCount !== 1 ? 's' : ''}</p>
      </div>

      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="animate-pulse flex items-center justify-between">
              <div className="h-4 w-24 bg-gray-200 rounded"></div>
              <div className="h-4 w-12 bg-gray-200 rounded"></div>
            </div>
          ))}
        </div>
      ) : sessionsByUser && Object.keys(sessionsByUser).length > 0 ? (
        <div className="space-y-3 max-h-64 overflow-y-auto">
          {Object.entries(sessionsByUser)
            .sort((a, b) => b[1] - a[1])
            .map(([userId, sessionCount]) => (
              <div key={userId} className="flex items-center justify-between py-2 border-b border-gray-100 last:border-b-0">
                <div className="flex items-center">
                  <div className="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center mr-3">
                    <svg className="w-4 h-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                  </div>
                  <span className="text-sm font-medium text-gray-700">{userId}</span>
                </div>
                <div className="flex items-center">
                  <span className="text-sm text-gray-600 mr-2">{sessionCount}</span>
                  <span className="text-xs text-gray-500">session{sessionCount !== 1 ? 's' : ''}</span>
                </div>
              </div>
            ))}
        </div>
      ) : (
        <div className="text-center py-8 text-gray-500">
          <svg className="w-16 h-16 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p>No active sessions in the last 5 minutes</p>
        </div>
      )}
    </div>
  );
};

export default ActiveSessions;

