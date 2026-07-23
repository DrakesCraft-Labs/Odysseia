use std::collections::HashMap;
use std::sync::RwLock;

/// Detector y guardián de automatizaciones y relojes rápidos de Redstone en Rust.
pub struct RedstoneClockGuard {
    // Registra el número de actualizaciones por bloque (x, y, z) en la ventana actual
    tick_counts: RwLock<HashMap<(i32, i32, i32), u32>>,
    max_ticks_per_second: u32,
}

impl RedstoneClockGuard {
    pub fn new(max_ticks_per_second: u32) -> Self {
        Self {
            tick_counts: RwLock::new(HashMap::new()),
            max_ticks_per_second,
        }
    }

    /// Registra un pulso de redstone en un bloque y determina si viola el límite (debe romperse).
    pub fn record_and_check(&self, x: i32, y: i32, z: i32, is_slimefun_protected: bool) -> bool {
        // Los bloques protegidos de Slimefun nunca se rompen automáticamente
        if is_slimefun_protected {
            return false;
        }

        let mut map = self.tick_counts.write().unwrap();
        let count = map.entry((x, y, z)).or_insert(0);
        *count += 1;

        *count > self.max_ticks_per_second
    }

    /// Reinicia la ventana de tiempo (ejecutado cada segundo por el servidor)
    pub fn reset_window(&self) {
        let mut map = self.tick_counts.write().unwrap();
        map.clear();
    }
}
