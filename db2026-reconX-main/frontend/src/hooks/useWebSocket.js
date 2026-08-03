// TICKET-ADV115 — useWebSocket(url) with auto-reconnect (exp backoff up to 5 tries).
import { useCallback, useEffect, useRef, useState } from 'react';

export function useWebSocket(
  url,
  {
    reconnect = true,
    maxRetries = 5,
    baseDelay = 500,
    maxDelay = 30000,
  } = {}
) {
  const [data, setData] = useState(null);
  const [status, setStatus] = useState('connecting');

  const wsRef = useRef(null);
  const retriesRef = useRef(0);
  const timerRef = useRef(null);
  const stoppedRef = useRef(false);

  const connect = useCallback(() => {
    if (stoppedRef.current) return;

    setStatus('connecting');

    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => {
      setStatus('open');
      retriesRef.current = 0;
    };

    ws.onmessage = (event) => {
      try {
        setData(JSON.parse(event.data));
      } catch {
        setData(event.data);
      }
    };

    ws.onerror = () => {
      setStatus('error');
    };

    ws.onclose = () => {
      setStatus('closed');

      if (
        reconnect &&
        !stoppedRef.current &&
        retriesRef.current < maxRetries
      ) {
        const delay = Math.min(
          maxDelay,
          baseDelay * 2 ** retriesRef.current
        );

        retriesRef.current++;

        timerRef.current = setTimeout(connect, delay);
      }
    };
  }, [url, reconnect, maxRetries, baseDelay, maxDelay]);

  useEffect(() => {
    stoppedRef.current = false;

    connect();

    return () => {
      stoppedRef.current = true;

      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }

      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [connect]);

  const send = useCallback((payload) => {
    if (
      wsRef.current &&
      wsRef.current.readyState === WebSocket.OPEN
    ) {
      wsRef.current.send(
        typeof payload === 'string'
          ? payload
          : JSON.stringify(payload)
      );
    }
  }, []);

  return {
    data,
    status,
    send,
  };
}