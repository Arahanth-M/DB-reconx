// Compound DataTable + useDebouncedSearch driving a paginated trades list.
import React, { useCallback, useEffect, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';

function Trades() {
  const [search, setSearch] = useState('');
  const debounced = useDebouncedSearch(search, 300);
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ items: [], totalPages: 0 });
  const [busyId, setBusyId] = useState(null);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    const params = new URLSearchParams();
    params.set('page', String(page));
    if (debounced) params.set('status', debounced);

    api.listTrades(params.toString())
      .then((res) => {
        if (cancelled) return;
        if (res && Array.isArray(res.items)) {
          setData({ items: res.items, totalPages: res.totalPages ?? 0 });
        } else if (Array.isArray(res)) {
          setData({ items: res, totalPages: 1 });
        } else {
          setData({ items: [], totalPages: 0 });
        }
      })
      .catch(() => {
        if (!cancelled) setData({ items: [], totalPages: 0 });
      });

    return () => { cancelled = true; };
  }, [page, debounced]);

  const markMatched = useCallback(async (id) => {
    setActionError(null);
    setBusyId(id);
    try {
      const updated = await api.updateStatus(id, 'MATCHED');
      setData((prev) => ({
        ...prev,
        items: prev.items.map((t) =>
          t.id === id ? { ...t, ...updated, status: updated?.status ?? 'MATCHED' } : t
        ),
      }));
    } catch (err) {
      setActionError(err.message || 'Failed to update status');
    } finally {
      setBusyId(null);
    }
  }, []);

  return (
    <section>
      <h2>Trades</h2>
      <input
        aria-label="Filter by status"
        placeholder="status filter (PENDING/MATCHED/…)"
        value={search}
        onChange={(e) => setSearch(e.target.value.toUpperCase())}
      />
      {actionError && <p role="alert" className="form-error">{actionError}</p>}
      <DataTable>
        <DataTable.Header columns={[
          { key: 'tradeRef', label: 'Ref' },
          { key: 'symbol',   label: 'Symbol' },
          { key: 'qty',      label: 'Qty' },
          { key: 'price',    label: 'Price' },
          { key: 'status',   label: 'Status' },
        ]} />
        <DataTable.Body
          rows={data.items}
          render={(t) => (
            <>
              <span>{t.tradeRef}</span>
              <span>{t.instrumentSymbol ?? t.symbol ?? t.instrument}</span>
              <span>{t.quantity ?? t.qty}</span>
              <span>{t.price}</span>
              <span className="trade-status-cell">
                {t.status}
                {t.status !== 'MATCHED' && (
                  <button
                    type="button"
                    className="match-btn"
                    disabled={busyId === t.id}
                    onClick={() => markMatched(t.id)}
                  >
                    {busyId === t.id ? 'Matching…' : 'Match'}
                  </button>
                )}
              </span>
            </>
          )}
        />
        <DataTable.Pagination
          page={page}
          totalPages={Math.max(1, data.totalPages)}
          onChange={setPage}
        />
      </DataTable>
    </section>
  );
}

export default withAuth(Trades);
