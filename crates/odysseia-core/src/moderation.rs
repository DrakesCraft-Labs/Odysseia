use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VanishState {
    pub is_vanished: bool,
    pub show_particle_trail: bool,
    pub target_player: String,
}

impl VanishState {
    pub fn new(target: String) -> Self {
        Self {
            is_vanished: false,
            show_particle_trail: true,
            target_player: target,
        }
    }

    pub fn toggle(&mut self) -> bool {
        self.is_vanished = !self.is_vanished;
        self.is_vanished
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OwnerAuraConfig {
    pub enabled: bool,
    pub particle_type: String,
    pub radius: f64,
    pub damage_per_tick: f64,
}

impl Default for OwnerAuraConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            particle_type: "SOUL_FIRE_FLAME".to_string(),
            radius: 5.0,
            damage_per_tick: 4.0,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SafeRestartCountdown {
    pub remaining_seconds: u32,
    pub is_active: bool,
}

impl SafeRestartCountdown {
    pub fn start() -> Self {
        Self {
            remaining_seconds: 30,
            is_active: true,
        }
    }

    pub fn tick(&mut self) -> Option<String> {
        if !self.is_active {
            return None;
        }

        let msg = match self.remaining_seconds {
            30 => Some("§c[Odysseia] §eEl servidor se reiniciará en 30 segundos. ¡Guarda tu progreso!".to_string()),
            15 => Some("§c[Odysseia] §eReiniciando en 15 segundos...".to_string()),
            10 => Some("§c[Odysseia] §cReiniciando en 10 segundos...".to_string()),
            5 => Some("§c[Odysseia] §4¡REINICIO EN 5 SEGUNDOS!".to_string()),
            0 => {
                self.is_active = false;
                Some("§c[Odysseia] §4¡Guardando datos y reiniciando ahora!".to_string())
            }
            _ => None,
        };

        if self.remaining_seconds > 0 {
            self.remaining_seconds -= 1;
        }

        msg
    }
}

pub struct OreSellProtection;

impl OreSellProtection {
    pub fn is_protected_item(material_name: &str, is_slimefun: bool, has_custom_lore: bool) -> bool {
        // Bloquear minerales automatizados de Slimefun o ítems con lore/metadatos personalizados
        if is_slimefun || has_custom_lore {
            return true;
        }

        let protected_ores = [
            "SLIME_BALL",
            "REINFORCED_ALLOY",
            "SYNTHETIC_DIAMOND",
            "CARBON_CHUNK",
            "BLISTERING_INGOT",
        ];

        let upper = material_name.to_uppercase();
        protected_ores.iter().any(|&ore| upper.contains(ore))
    }
}
