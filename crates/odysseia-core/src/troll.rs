use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum StaffTrollType {
    FakeOp,
    FakeCrash,
    VoidFall,
    Anvil,
    Creeper,
    Spiders,
    Lightning,
    Screamer,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TrollActionPayload {
    pub troll_type: StaffTrollType,
    pub target_player: String,
    pub staff_executor: String,
}

impl StaffTrollType {
    pub fn get_feedback_message(&self, target: &str) -> String {
        match self {
            Self::FakeOp => format!("§a[Troll] Mensaje de Fake OP enviado a {}", target),
            Self::FakeCrash => format!("§c[Troll] Pantalla de Fake Crash simulada para {}", target),
            Self::VoidFall => format!("§e[Troll] Caída de vacío inofensiva aplicada a {}", target),
            Self::Anvil => format!("§7[Troll] Yunque cayendo invocado sobre {}", target),
            Self::Creeper => format!("§a[Troll] Siseo de Creeper reproducido cerca de {}", target),
            Self::Spiders => format!("§8[Troll] Enjambre de arañas fantasmas sobre {}", target),
            Self::Lightning => format!("§e[Troll] Rayo estético descargado sobre {}", target),
            Self::Screamer => format!("§4[Troll] Screamer de terror enviado a {}", target),
        }
    }
}
