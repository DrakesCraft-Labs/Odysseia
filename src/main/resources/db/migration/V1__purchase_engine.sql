CREATE TABLE IF NOT EXISTS schema_migrations(version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS purchase_deliveries (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  provider TEXT NOT NULL,
  transaction_id TEXT NOT NULL,
  player_name TEXT NOT NULL,
  player_uuid TEXT,
  product_id TEXT NOT NULL,
  product_version INTEGER NOT NULL,
  state TEXT NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  last_error TEXT,
  announcement_sent INTEGER NOT NULL DEFAULT 0,
  received_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  completed_at TEXT,
  UNIQUE(provider, transaction_id, product_id)
);
CREATE INDEX IF NOT EXISTS idx_purchase_player ON purchase_deliveries(player_name, state);
CREATE INDEX IF NOT EXISTS idx_purchase_state ON purchase_deliveries(state, updated_at);
CREATE TABLE IF NOT EXISTS purchase_actions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  delivery_id INTEGER NOT NULL REFERENCES purchase_deliveries(id) ON DELETE CASCADE,
  action_key TEXT NOT NULL,
  position INTEGER NOT NULL,
  action_type TEXT NOT NULL,
  state TEXT NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  requires_online INTEGER NOT NULL,
  refund_policy TEXT NOT NULL,
  required INTEGER NOT NULL,
  last_error TEXT,
  result TEXT,
  started_at TEXT,
  updated_at TEXT NOT NULL,
  completed_at TEXT,
  UNIQUE(delivery_id, action_key),
  UNIQUE(delivery_id, position)
);
CREATE INDEX IF NOT EXISTS idx_action_delivery_state ON purchase_actions(delivery_id, state, position);
CREATE TABLE IF NOT EXISTS purchase_audit (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  delivery_id INTEGER REFERENCES purchase_deliveries(id) ON DELETE SET NULL,
  actor TEXT NOT NULL,
  event TEXT NOT NULL,
  detail TEXT,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_delivery ON purchase_audit(delivery_id, created_at);
