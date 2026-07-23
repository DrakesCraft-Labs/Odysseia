use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub struct Vector3D {
    pub x: f64,
    pub y: f64,
    pub z: f64,
}

impl Vector3D {
    pub fn new(x: f64, y: f64, z: f64) -> Self {
        Self { x, y, z }
    }

    pub fn distance_squared(&self, other: &Vector3D) -> f64 {
        let dx = self.x - other.x;
        let dy = self.y - other.y;
        let dz = self.z - other.z;
        dx * dx + dy * dy + dz * dz
    }

    pub fn distance(&self, other: &Vector3D) -> f64 {
        self.distance_squared(other).sqrt()
    }

    pub fn normalize(&self) -> Self {
        let len = (self.x * self.x + self.y * self.y + self.z * self.z).sqrt();
        if len == 0.0 {
            Self::new(0.0, 0.0, 0.0)
        } else {
            Self::new(self.x / len, self.y / len, self.z / len)
        }
    }
}

/// Calcula si un jugador está dentro de la onda expansiva o aura de un Boss.
pub fn is_in_boss_aura(boss_pos: Vector3D, player_pos: Vector3D, aura_radius: f64) -> bool {
    boss_pos.distance_squared(&player_pos) <= (aura_radius * aura_radius)
}
