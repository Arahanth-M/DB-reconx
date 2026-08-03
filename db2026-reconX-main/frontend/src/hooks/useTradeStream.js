// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.
import { useEffect, useState } from 'react';

const MAX_BUFFER = 200;

export function useTradeStream(url = '/api/v1/trades/stream') {
  const [trades, setTrades] = useState([]);
  const [isConnected, setConnected] = useState(false);

  useEffect(() => {
    const eventSource = new EventSource(url);

    eventSource.onopen = () => {
      setConnected(true);
    };

    eventSource.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);

        setTrades((prev) => [trade, ...prev].slice(0, MAX_BUFFER));
      } catch {
        // Ignore malformed payloads
      }
    };

    eventSource.onerror = () => {
      setConnected(false);
    };

    return () => {
      eventSource.close();
    };
  }, [url]);

  return {
    trades,
    isConnected,
  };
}