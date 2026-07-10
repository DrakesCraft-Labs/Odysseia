CREATE TABLE IF NOT EXISTS purchase_financial_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  provider TEXT NOT NULL,
  transaction_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  event_type TEXT NOT NULL,
  actor TEXT NOT NULL,
  result TEXT,
  created_at TEXT NOT NULL,
  UNIQUE(provider, transaction_id, product_id, event_type)
);
