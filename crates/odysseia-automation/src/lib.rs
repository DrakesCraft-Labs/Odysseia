use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::RwLock;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ClockAction {
    Allow,
    Throttle,
    Break,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AutomationGuardConfig {
    pub min_pulses: u32,
    pub max_pulses: u32,
    pub max_warnings_before_break: u32,
}

impl Default for AutomationGuardConfig {
    fn default() -> Self {
        Self {
            min_pulses: 40,
            max_pulses: 180,
            max_warnings_before_break: 2,
        }
    }
}

pub struct RedstoneClockGuard {
    config: AutomationGuardConfig,
    tick_counts: RwLock<HashMap<(i32, i32, i32), u32>>,
    warning_counts: RwLock<HashMap<(i32, i32, i32), u32>>,
}

impl RedstoneClockGuard {
    pub fn new(config: AutomationGuardConfig) -> Self {
        Self {
            config,
            tick_counts: RwLock::new(HashMap::new()),
            warning_counts: RwLock::new(HashMap::new()),
        }
    }

    pub fn evaluate_clock(
        &self,
        x: i32,
        y: i32,
        z: i32,
        is_clock_structure: bool,
        is_protected_slimefun: bool,
    ) -> ClockAction {
        if is_protected_slimefun || !is_clock_structure {
            return ClockAction::Allow;
        }

        let key = (x, y, z);
        let mut ticks_map = self.tick_counts.write().unwrap();
        let pulse_count = ticks_map.entry(key).or_insert(0);
        *pulse_count += 1;

        if *pulse_count >= self.config.min_pulses || *pulse_count >= self.config.max_pulses {
            let mut warnings_map = self.warning_counts.write().unwrap();
            let warnings = warnings_map.entry(key).or_insert(0);

            if *warnings >= self.config.max_warnings_before_break {
                ClockAction::Break
            } else {
                *warnings += 1;
                ClockAction::Throttle
            }
        } else {
            ClockAction::Allow
        }
    }

    pub fn record_and_check(&self, x: i32, y: i32, z: i32, is_protected: bool) -> bool {
        self.evaluate_clock(x, y, z, true, is_protected) == ClockAction::Break
    }

    pub fn reset_window(&self) {
        self.tick_counts.write().unwrap().clear();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_clock_escalation_policy() {
        let guard = RedstoneClockGuard::new(AutomationGuardConfig::default());
        assert_eq!(
            guard.evaluate_clock(10, 64, 10, true, true),
            ClockAction::Allow
        );
        assert_eq!(
            guard.evaluate_clock(10, 64, 10, false, false),
            ClockAction::Allow
        );
        for _ in 0..39 {
            assert_eq!(
                guard.evaluate_clock(20, 64, 20, true, false),
                ClockAction::Allow
            );
        }
        assert_eq!(
            guard.evaluate_clock(20, 64, 20, true, false),
            ClockAction::Throttle
        );
        assert_eq!(
            guard.evaluate_clock(20, 64, 20, true, false),
            ClockAction::Throttle
        );
        assert_eq!(
            guard.evaluate_clock(20, 64, 20, true, false),
            ClockAction::Break
        );
    }
}
