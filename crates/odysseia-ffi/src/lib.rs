use odysseia_automation::RedstoneClockGuard;
use odysseia_core::{BossType, ChatFilterEngine, ChatFilterRule, Vector3D};
use odysseia_telemetry::ServerStatusReport;
use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use std::sync::OnceLock;

static REDSTONE_GUARD: OnceLock<RedstoneClockGuard> = OnceLock::new();
static CHAT_FILTER: OnceLock<ChatFilterEngine> = OnceLock::new();

fn get_redstone_guard() -> &'static RedstoneClockGuard {
    REDSTONE_GUARD.get_or_init(|| RedstoneClockGuard::new(5))
}

fn get_chat_filter() -> &'static ChatFilterEngine {
    CHAT_FILTER.get_or_init(|| ChatFilterEngine::new(ChatFilterRule::default()))
}

/// Devuelve si un Boss es elegible para apariciones naturales en el mundo.
#[no_mangle]
pub extern "C" fn odysseia_is_boss_natural(boss_id_ptr: *const c_char) -> i32 {
    if boss_id_ptr.is_null() {
        return 0;
    }
    let c_str = unsafe { CStr::from_ptr(boss_id_ptr) };
    if let Ok(id_str) = c_str.to_str() {
        if let Some(boss) = BossType::from_id(id_str) {
            return if boss.is_natural_spawn_allowed() { 1 } else { 0 };
        }
    }
    0
}

/// Verifica si la posición del jugador está dentro del aura de daño de un Boss.
#[no_mangle]
pub extern "C" fn odysseia_check_boss_aura(
    bx: f64, by: f64, bz: f64,
    px: f64, py: f64, pz: f64,
    aura_radius: f64
) -> i32 {
    let boss = Vector3D::new(bx, by, bz);
    let player = Vector3D::new(px, py, pz);
    if odysseia_core::is_in_boss_aura(boss, player, aura_radius) { 1 } else { 0 }
}

/// Registra el pulso de redstone y responde si viola el límite de velocidad.
#[no_mangle]
pub extern "C" fn odysseia_check_redstone_clock(x: i32, y: i32, z: i32, is_protected: i32) -> i32 {
    let guard = get_redstone_guard();
    if guard.record_and_check(x, y, z, is_protected != 0) { 1 } else { 0 }
}

/// Inspecciona un mensaje de chat para detectar palabras prohibidas.
#[no_mangle]
pub extern "C" fn odysseia_chat_filter_inspect(msg_ptr: *const c_char) -> *mut c_char {
    if msg_ptr.is_null() {
        return std::ptr::null_mut();
    }
    let c_str = unsafe { CStr::from_ptr(msg_ptr) };
    if let Ok(msg) = c_str.to_str() {
        let filter = get_chat_filter();
        if let Some(forbidden) = filter.inspect_message(msg) {
            return CString::new(forbidden).unwrap().into_raw();
        }
    }
    std::ptr::null_mut()
}

/// Formatea la respuesta de estado para DiscordSRV en C-String.
#[no_mangle]
pub extern "C" fn odysseia_format_tps_message(
    tps1m: f64, tps5m: f64, tps15m: f64,
    used_ram: u64, max_ram: u64,
    _online_count: u32, max_players: u32
) -> *mut c_char {
    let report = ServerStatusReport {
        tps_1m: tps1m,
        tps_5m: tps5m,
        tps_15m: tps15m,
        used_ram_mb: used_ram,
        max_ram_mb: max_ram,
        online_players: vec![],
        max_players,
    };
    let formatted = report.build_discord_status_message();
    CString::new(formatted).unwrap().into_raw()
}

/// Libera la memoria de una cadena C devuelta por Rust.
#[no_mangle]
pub extern "C" fn odysseia_free_string(s: *mut c_char) {
    if !s.is_null() {
        unsafe {
            let _ = CString::from_raw(s);
        }
    }
}
