use serde::{Deserialize, Serialize};
use std::fmt;

/// Definición completa de los Bosses Míticos de Odysseia.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum BossType {
    DragonAncestral,
    WitherStorm,
    ColosoEnd,
    Poseidon,
    Zeus,
    Anubis,
    Hades,
    Fenrir,
    Kraken,
    Champi,
    Leviatan,
    DragonNegro,
    Yeti,
    GolemObsidiana,
    Minotauro,
    Manticora,
    Ciclope,
    Naga,
    Lich,
    Baphomet,
    Prometeo,
    DiosCorrupto,
    Spartan,
    Thor,
    Ares,
    Odin,
    Kratos,
    Loki,
}

impl BossType {
    pub fn from_id(id: &str) -> Option<Self> {
        match id.to_lowercase().as_str() {
            "dragon_ancestral" | "dragonancestral" => Some(Self::DragonAncestral),
            "wither_storm" | "witherstorm" => Some(Self::WitherStorm),
            "coloso_end" | "colosoend" => Some(Self::ColosoEnd),
            "poseidon" => Some(Self::Poseidon),
            "zeus" => Some(Self::Zeus),
            "anubis" => Some(Self::Anubis),
            "hades" => Some(Self::Hades),
            "fenrir" => Some(Self::Fenrir),
            "kraken" => Some(Self::Kraken),
            "champi" => Some(Self::Champi),
            "leviatan" => Some(Self::Leviatan),
            "dragon_negro" | "dragonnegro" => Some(Self::DragonNegro),
            "yeti" => Some(Self::Yeti),
            "golem_obsidiana" | "golemobsidiana" => Some(Self::GolemObsidiana),
            "minotauro" => Some(Self::Minotauro),
            "manticora" => Some(Self::Manticora),
            "ciclope" => Some(Self::Ciclope),
            "naga" => Some(Self::Naga),
            "lich" => Some(Self::Lich),
            "baphomet" => Some(Self::Baphomet),
            "prometeo" => Some(Self::Prometeo),
            "dios_corrupto" | "dioscorrupto" => Some(Self::DiosCorrupto),
            "spartan" => Some(Self::Spartan),
            "thor" => Some(Self::Thor),
            "ares" => Some(Self::Ares),
            "odin" => Some(Self::Odin),
            "kratos" => Some(Self::Kratos),
            "loki" => Some(Self::Loki),
            _ => None,
        }
    }

    pub fn id(&self) -> &'static str {
        match self {
            Self::DragonAncestral => "dragon_ancestral",
            Self::WitherStorm => "wither_storm",
            Self::ColosoEnd => "coloso_end",
            Self::Poseidon => "poseidon",
            Self::Zeus => "zeus",
            Self::Anubis => "anubis",
            Self::Hades => "hades",
            Self::Fenrir => "fenrir",
            Self::Kraken => "kraken",
            Self::Champi => "champi",
            Self::Leviatan => "leviatan",
            Self::DragonNegro => "dragon_negro",
            Self::Yeti => "yeti",
            Self::GolemObsidiana => "golem_obsidiana",
            Self::Minotauro => "minotauro",
            Self::Manticora => "manticora",
            Self::Ciclope => "ciclope",
            Self::Naga => "naga",
            Self::Lich => "lich",
            Self::Baphomet => "baphomet",
            Self::Prometeo => "prometeo",
            Self::DiosCorrupto => "dios_corrupto",
            Self::Spartan => "spartan",
            Self::Thor => "thor",
            Self::Ares => "ares",
            Self::Odin => "odin",
            Self::Kratos => "kratos",
            Self::Loki => "loki",
        }
    }

    pub fn display_name(&self) -> &'static str {
        match self {
            Self::DragonAncestral => "§c§lDRAGÓN ANCESTRAL DEL VACÍO",
            Self::WitherStorm => "§5§lWITHER STORM ANCESTRAL",
            Self::ColosoEnd => "§d§lCOLOSO DEL END",
            Self::Poseidon => "§b§lPOSEIDÓN, SEÑOR DE LOS MARES",
            Self::Zeus => "§e§lZEUS, DIOS DEL RAYO",
            Self::Anubis => "§6§lANUBIS, GUARDIÁN DE LAS SOMBRAS",
            Self::Hades => "§4§lHADES, REY DEL INFRAMUNDO",
            Self::Fenrir => "§8§lFENRIR, EL LOBO DEVORADOR",
            Self::Kraken => "§9§lKRAKEN DE LAS PROFUNDIDADES",
            Self::Champi => "§a§lCHAMPI, EL MONSTRUO FÚNGICO",
            Self::Leviatan => "§3§lLEVIATÁN ANCESTRAL",
            Self::DragonNegro => "§0§lDRAGÓN NEGRO DE OBSIDIANA",
            Self::Yeti => "§f§lYETI DE LAS CUMBRES HELADAS",
            Self::GolemObsidiana => "§8§lGOLEM DE OBSIDIANA PURA",
            Self::Minotauro => "§c§lMINOTAURO DEL LABERINTO",
            Self::Manticora => "§6§lMÁNTICORA VENENOSA",
            Self::Ciclope => "§e§lCÍCLOPE FURIOSO",
            Self::Naga => "§2§lNAGA ANCESTRAL",
            Self::Lich => "§5§lLICH NIGROMANTE",
            Self::Baphomet => "§4§lBAPHOMET, SEÑOR OSCURO",
            Self::Prometeo => "§c§lPROMETEO, EL RENACIDO FÉNIX",
            Self::DiosCorrupto => "§5§lDIOS CORRUPTO ANCESTRAL",
            Self::Spartan => "§c§lREY ESPARTANO LLEÓNIDAS",
            Self::Thor => "§e§lTHOR, DIOS DEL TRUENO",
            Self::Ares => "§4§lARES, DIOS DE LA GUERRA",
            Self::Odin => "§9§lODÍN, PADRE DE TODO",
            Self::Kratos => "§c§lKRATOS, FANTASMA DE SPARTA",
            Self::Loki => "§d§lLOKI, DIOS DE LAS ENGAÑOS",
        }
    }

    /// Bosses elegibles para spawneo natural en el mundo.
    /// (Tanto Wither Storm como Dragón Ancestral están excluidos de spawn natural).
    pub fn is_natural_spawn_allowed(&self) -> bool {
        !matches!(self, Self::DragonAncestral | Self::WitherStorm | Self::DiosCorrupto)
    }

    /// Divine Favor Rewards: Recompensa de 5,000 XP y Loot Firmado
    pub fn divine_xp_reward(&self) -> u32 {
        5000
    }

    pub fn profile(&self) -> BossCombatProfile {
        match self {
            Self::DragonAncestral => BossCombatProfile {
                max_health: 3500.0,
                attack_damage: 45.0,
                defense: 25.0,
                scale: 1.5,
                aura_radius: 18.0,
            },
            Self::WitherStorm => BossCombatProfile {
                max_health: 3000.0,
                attack_damage: 50.0,
                defense: 30.0,
                scale: 3.0,
                aura_radius: 25.0,
            },
            Self::DiosCorrupto => BossCombatProfile {
                max_health: 4000.0,
                attack_damage: 60.0,
                defense: 35.0,
                scale: 2.0,
                aura_radius: 20.0,
            },
            Self::Prometeo => BossCombatProfile {
                max_health: 2000.0,
                attack_damage: 35.0,
                defense: 20.0,
                scale: 1.3,
                aura_radius: 12.0,
            },
            Self::ColosoEnd => BossCombatProfile {
                max_health: 2500.0,
                attack_damage: 40.0,
                defense: 20.0,
                scale: 2.5,
                aura_radius: 15.0,
            },
            _ => BossCombatProfile {
                max_health: 1500.0,
                attack_damage: 30.0,
                defense: 15.0,
                scale: 1.2,
                aura_radius: 10.0,
            },
        }
    }
}

impl fmt::Display for BossType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.id())
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BossCombatProfile {
    pub max_health: f64,
    pub attack_damage: f64,
    pub defense: f64,
    pub scale: f64,
    pub aura_radius: f64,
}
