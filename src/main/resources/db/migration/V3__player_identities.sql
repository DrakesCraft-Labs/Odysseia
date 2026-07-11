CREATE TABLE IF NOT EXISTS player_identities (
  player_uuid TEXT PRIMARY KEY,
  canonical_name TEXT NOT NULL COLLATE NOCASE UNIQUE,
  normalized_name TEXT NOT NULL COLLATE NOCASE,
  platform TEXT NOT NULL,
  bedrock_prefix INTEGER NOT NULL DEFAULT 0,
  first_seen_at TEXT NOT NULL,
  last_seen_at TEXT NOT NULL,
  verification_source TEXT NOT NULL,
  confidence TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS player_identity_aliases (
  player_uuid TEXT NOT NULL REFERENCES player_identities(player_uuid) ON DELETE CASCADE,
  alias TEXT NOT NULL COLLATE NOCASE,
  alias_type TEXT NOT NULL,
  confidence TEXT NOT NULL,
  first_seen_at TEXT NOT NULL,
  last_seen_at TEXT NOT NULL,
  PRIMARY KEY(player_uuid, alias)
);
CREATE INDEX IF NOT EXISTS idx_identity_canonical ON player_identities(canonical_name);
CREATE INDEX IF NOT EXISTS idx_identity_alias ON player_identity_aliases(alias, confidence);
