use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum OdysseyArmorSet {
    ZeusLightning,
    PoseidonOcean,
    HadesUnderworld,
    AnubisShadow,
    FenrirWolf,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ArmorEffectBonus {
    pub speed_multiplier: f64,
    pub damage_boost: f64,
    pub resistance_boost: f64,
    pub special_ability: String,
}

impl OdysseyArmorSet {
    pub fn get_set_bonus(&self) -> ArmorEffectBonus {
        match self {
            Self::ZeusLightning => ArmorEffectBonus {
                speed_multiplier: 1.3,
                damage_boost: 15.0,
                resistance_boost: 5.0,
                special_ability: "Rayo pasivo al golpear".to_string(),
            },
            Self::PoseidonOcean => ArmorEffectBonus {
                speed_multiplier: 1.2,
                damage_boost: 10.0,
                resistance_boost: 10.0,
                special_ability: "Respiración acuática infinita".to_string(),
            },
            Self::HadesUnderworld => ArmorEffectBonus {
                speed_multiplier: 1.1,
                damage_boost: 20.0,
                resistance_boost: 15.0,
                special_ability: "Inmunidad al fuego y lava".to_string(),
            },
            Self::AnubisShadow => ArmorEffectBonus {
                speed_multiplier: 1.4,
                damage_boost: 12.0,
                resistance_boost: 8.0,
                special_ability: "Invisibilidad al agacharse".to_string(),
            },
            Self::FenrirWolf => ArmorEffectBonus {
                speed_multiplier: 1.5,
                damage_boost: 25.0,
                resistance_boost: 5.0,
                special_ability: "Robo de vida en combate".to_string(),
            },
        }
    }
}
