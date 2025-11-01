import { useState, useEffect } from 'react';
import { metricsApi } from '../services/api';

const REFRESH_INTERVAL = 30000; // 30 seconds

/**
 * Custom hook to fetch and auto-refresh metrics
 * @returns {Object} { metrics, loading, error, lastUpdated }
 */
export const useMetrics = () => {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  const fetchMetrics = async () => {
    try {
      setError(null);
      const data = await metricsApi.getMetrics();
      setMetrics(data);
      setLastUpdated(new Date());
      setLoading(false);
    } catch (err) {
      setError(err.message || 'Failed to fetch metrics');
      setLoading(false);
    }
  };

  useEffect(() => {
    // Initial fetch
    fetchMetrics();

    // Set up auto-refresh interval
    const intervalId = setInterval(() => {
      fetchMetrics();
    }, REFRESH_INTERVAL);

    // Cleanup interval on unmount
    return () => clearInterval(intervalId);
  }, []);

  return { metrics, loading, error, lastUpdated, refresh: fetchMetrics };
};

