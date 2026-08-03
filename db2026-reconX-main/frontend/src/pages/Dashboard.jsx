// TICKET-ADV120 — useMemo for portfolio-value calc.
// TICKET-ADV116 — useTradeStream live feed.
import React, { useMemo } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';

function StatCard({ label, value }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
    </article>
  );
}

function Dashboard() {
  const { trades, isConnected } = useTradeStream();

  // TODO(TICKET-ADV120): use useMemo to compute `portfolioValue` =
  //                     sum(trades[i].quantity * trades[i].price).
  //                     Memoise on `trades` so it doesn't recompute every render.

  // TODO(TICKET-ADV120): derive `matched` (status === 'MATCHED') and
  //                     `breaks` (status in ['UNMATCHED','DISPUTED']) counts.
  const portfolioValue = useMemo(
  () =>
    trades.reduce(
      (sum, trade) => sum + ((trade.quantity || 0) * (trade.price || 0)),
      0
    ),
  [trades]
);

const matched = useMemo(
  () => trades.filter((trade) => trade.status === 'MATCHED').length,
  [trades]
);

const breaks = useMemo(
  () =>
    trades.filter((trade) =>
      ['UNMATCHED', 'DISPUTED'].includes(trade.status)
    ).length,
  [trades]
);

  return (
    <section>
      <h2>Dashboard</h2>
      <div className="stat-grid">
  <StatCard
    label="Portfolio Value"
    value={portfolioValue.toLocaleString()}
  />

  <StatCard
    label="Trades Streamed"
    value={trades.length}
  />

  <StatCard
    label="Matched"
    value={matched}
  />

  <StatCard
    label="Open Breaks"
    value={breaks}
  />
</div>
      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
    </section>
  );
}

export default withAuth(Dashboard);
