use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerStatusReport {
    pub tps_1m: f64,
    pub tps_5m: f64,
    pub tps_15m: f64,
    pub used_ram_mb: u64,
    pub max_ram_mb: u64,
    pub online_players: Vec<String>,
    pub max_players: u32,
}

impl ServerStatusReport {
    pub fn build_discord_status_message(&self) -> String {
        let status_indicator = if self.tps_1m >= 19.5 {
            "🟢 Excelente"
        } else if self.tps_1m >= 17.0 {
            "🟡 Aceptable"
        } else {
            "🔴 Carga Alta"
        };

        let mut msg = format!(
            "📊 **Estado del Servidor DrakesCraft (Motor Rust)**\n\
             • **Rendimiento:** {}\n\
             • **TPS (1m / 5m / 15m):** `{:.2}` | `{:.2}` | `{:.2}`\n\
             • **Memoria RAM:** `{} MB / {} MB`\n\
             • **Jugadores Conectados:** `{} / {}` \n",
            status_indicator,
            self.tps_1m,
            self.tps_5m,
            self.tps_15m,
            self.used_ram_mb,
            self.max_ram_mb,
            self.online_players.len(),
            self.max_players
        );

        if self.online_players.is_empty() {
            msg.push_str("👥 **Lista:** *No hay jugadores conectados en este momento.*");
        } else {
            msg.push_str(&format!("👥 **Lista:** {}", self.online_players.join(", ")));
        }

        msg
    }
}
