use rand::Rng;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum HorrorEventType {
    Screamer,
    CreepyWhisper { message: String },
    ShadowStalker,
    WitherStormAtmosphere { is_final_strike: bool },
}

pub struct HorrorNightEngine {
    enabled: bool,
}

impl HorrorNightEngine {
    pub fn new(enabled: bool) -> Self {
        Self { enabled }
    }

    pub fn generate_strike(&self, strike_index: u8) -> Option<HorrorEventType> {
        if !self.enabled {
            return None;
        }

        let mut rng = rand::rng();
        let event_id = rng.random_range(0..4);

        match event_id {
            0 => Some(HorrorEventType::Screamer),
            1 => {
                let whispers = [
                    "§8[§4???§8] §7\"Puedo oler tu miedo en la oscuridad...\"",
                    "§8[§4???§8] §7\"El Wither Storm todo lo consume...\"",
                    "§8[§4???§8] §7\"No estás solo en estas sombras...\"",
                    "§8[§4???§8] §7\"Tus gritos no llegarán a la superficie...\"",
                ];
                let idx = rng.random_range(0..whispers.len());
                Some(HorrorEventType::CreepyWhisper {
                    message: whispers[idx].to_string(),
                })
            }
            2 => Some(HorrorEventType::ShadowStalker),
            _ => Some(HorrorEventType::WitherStormAtmosphere {
                is_final_strike: strike_index == 3,
            }),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BloodMoonConfig {
    pub enabled: bool,
    pub spawn_multiplier: f64,
    pub mob_damage_multiplier: f64,
}

pub struct BloodMoonManager {
    config: BloodMoonConfig,
    is_active: bool,
}

impl BloodMoonManager {
    pub fn new(config: BloodMoonConfig) -> Self {
        Self {
            config,
            is_active: false,
        }
    }

    pub fn check_night_start(&mut self, is_blood_moon_night: bool) -> bool {
        if self.config.enabled && is_blood_moon_night {
            self.is_active = true;
            return true;
        }
        self.is_active = false;
        false
    }

    pub fn is_active(&self) -> bool {
        self.is_active
    }
}
