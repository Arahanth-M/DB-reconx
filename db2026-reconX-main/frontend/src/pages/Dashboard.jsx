// useMemo for portfolio-value calc + useTradeStream live feed.
import React, { useEffect, useMemo, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';
import { api } from '@services/apiService.js';

function StatCard({ label, value }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
    </article>
  );
}

function Dashboard({ trades: tradesProp }) {
  const stream = useTradeStream();
  const [listed, setListed] = useState([]);

  useEffect(() => {
    if (tradesProp) return undefined;
    let cancelled = false;
    api.listTrades('page=0&size=200')
      .then((res) => {
        if (cancelled) return;
        const items = Array.isArray(res?.items) ? res.items : Array.isArray(res) ? res : [];
        setListed(items);
      })
      .catch(() => {
        if (!cancelled) setListed([]);
      });
    return () => { cancelled = true; };
  }, [tradesProp]);

  // Prefer explicit prop (tests); otherwise merge REST snapshot + live SSE.
  const trades = useMemo(() => {
    if (tradesProp) return tradesProp;
    if (stream.trades.length === 0) return listed;
    const seen = new Set(stream.trades.map((t) => t.id ?? t.tradeRef));
    const older = listed.filter((t) => !seen.has(t.id ?? t.tradeRef));
    return [...stream.trades, ...older];
  }, [tradesProp, stream.trades, listed]);
  const isConnected = tradesProp ? true : stream.isConnected;

  const portfolioValue = useMemo(
    () => trades.reduce((sum, t) => sum + (Number(t.quantity) * Number(t.price) || 0), 0),
    [trades]
  );

  const { matched, unmatched, breaks } = useMemo(() => {
    let m = 0;
    let u = 0;
    let b = 0;
    for (const t of trades) {
      if (t.status === 'MATCHED') m++;
      else if (t.status === 'UNMATCHED') { u++; b++; }
      else if (t.status === 'DISPUTED') b++;
    }
    return { matched: m, unmatched: u, breaks: b };
  }, [trades]);

  return (
    <section>
      <h2>Dashboard</h2>
      <div className="stat-grid">
        <StatCard label="Portfolio value (USD)" value={portfolioValue.toLocaleString()} />
        <StatCard label="Trades streamed" value={trades.length} />
        <StatCard label="Matched trades" value={matched} />
        <StatCard label="Unmatched trades" value={unmatched} />
        <StatCard label="Open breaks" value={breaks} />
      </div>
      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
    </section>
  );
}

export default withAuth(Dashboard);
