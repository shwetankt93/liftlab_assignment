import React from 'react';

/**
 * Component to display active users count (last 5 minutes)
 */
const ActiveUsers = ({ count, loading }) => {
  return (
    <div className="bg-white rounded-lg shadow-md p-6 border border-gray-200">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-gray-700 mb-1">Active Users</h3>
          <p className="text-sm text-gray-500">Last 5 minutes</p>
        </div>
        <div className="text-right">
          {loading ? (
            <div className="animate-pulse">
              <div className="h-8 w-16 bg-gray-200 rounded"></div>
            </div>
          ) : (
            <div className="text-3xl font-bold text-blue-600">{count ?? 0}</div>
          )}
        </div>
      </div>
      <div className="mt-4 pt-4 border-t border-gray-200">
        <div className="flex items-center text-sm text-gray-600">
          <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          Unique users with events in the last 5 minutes
        </div>
      </div>
    </div>
  );
};

export default ActiveUsers;

