use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DragonFlightPolicy {
    pub max_altitude: f64,
    pub max_speed: f64,
    pub stamina_depletion_rate: f64,
    pub stamina_regen_rate: f64,
}

impl Default for DragonFlightPolicy {
    fn default() -> Self {
        Self {
            max_altitude: 250.0,
            max_speed: 1.8,
            stamina_depletion_rate: 1.5,
            stamina_regen_rate: 2.0,
        }
    }
}

pub struct DragonMountState {
    pub current_stamina: f64,
    pub max_stamina: f64,
    pub is_mounted: bool,
}

impl DragonMountState {
    pub fn new(max_stamina: f64) -> Self {
        Self {
            current_stamina: max_stamina,
            max_stamina,
            is_mounted: false,
        }
    }

    pub fn tick_flight(&mut self, current_y: f64, policy: &DragonFlightPolicy) -> bool {
        if current_y > policy.max_altitude {
            self.current_stamina -= policy.stamina_depletion_rate * 2.0;
        } else {
            self.current_stamina -= policy.stamina_depletion_rate;
        }

        if self.current_stamina < 0.0 {
            self.current_stamina = 0.0;
            return false; // Exhausted, must land
        }
        true
    }

    pub fn tick_rest(&mut self, policy: &DragonFlightPolicy) {
        if self.current_stamina < self.max_stamina {
            self.current_stamina = (self.current_stamina + policy.stamina_regen_rate).min(self.max_stamina);
        }
    }
}
