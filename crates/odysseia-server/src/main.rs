use axum::{
    extract::Json,
    routing::{get, post},
    Router,
};
use odysseia_core::{BossType, ChatFilterEngine, ChatFilterRule, DragonFlightPolicy};
use odysseia_horror::{BloodMoonConfig, BloodMoonManager};
use odysseia_store::{PurchaseEngine, StorePurchasePayload};
use odysseia_telemetry::ServerStatusReport;
use std::net::SocketAddr;
use std::sync::{Arc, Mutex};
use tracing::info;

struct AppState {
    purchase_engine: PurchaseEngine,
    chat_filter: ChatFilterEngine,
    dragon_policy: DragonFlightPolicy,
    blood_moon: Mutex<BloodMoonManager>,
}

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();

    let state = Arc::new(AppState {
        purchase_engine: PurchaseEngine::new(),
        chat_filter: ChatFilterEngine::new(ChatFilterRule::default()),
        dragon_policy: DragonFlightPolicy::default(),
        blood_moon: Mutex::new(BloodMoonManager::new(BloodMoonConfig {
            enabled: true,
            spawn_multiplier: 2.5,
            mob_damage_multiplier: 1.8,
        })),
    });

    let app = Router::new()
        .route("/health", get(health_check))
        .route("/api/bosses", get(get_bosses_list))
        .route("/api/store/catalog", get(get_store_catalog))
        .route("/api/store/webhook", post(handle_store_webhook))
        .route("/api/chat/filter", post(handle_chat_filter))
        .route("/api/dragon/policy", get(get_dragon_policy))
        .route("/api/bloodmoon/status", get(get_bloodmoon_status))
        .route("/api/telemetry/report", post(handle_telemetry_report))
        .with_state(state);

    let addr = SocketAddr::from(([0, 0, 0, 0], 8080));
    info!("🚀 Odysseia Engine Server (Rust) corriendo en http://{}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn health_check() -> &'static str {
    "OK - Odysseia Rust Engine Fully Loaded"
}

async fn get_bosses_list() -> Json<Vec<serde_json::Value>> {
    let bosses = vec![
        BossType::DragonAncestral,
        BossType::WitherStorm,
        BossType::ColosoEnd,
        BossType::Poseidon,
        BossType::Zeus,
        BossType::Anubis,
        BossType::Hades,
        BossType::Fenrir,
        BossType::Kraken,
        BossType::Champi,
        BossType::Leviatan,
        BossType::DragonNegro,
        BossType::Yeti,
        BossType::GolemObsidiana,
        BossType::Minotauro,
        BossType::Manticora,
        BossType::Ciclope,
        BossType::Naga,
        BossType::Lich,
        BossType::Baphomet,
    ];

    let result = bosses
        .into_iter()
        .map(|b| {
            serde_json::json!({
                "id": b.id(),
                "display_name": b.display_name(),
                "natural_spawn": b.is_natural_spawn_allowed(),
                "profile": b.profile()
            })
        })
        .collect();

    Json(result)
}

async fn get_store_catalog(
    axum::extract::State(state): axum::extract::State<Arc<AppState>>,
) -> Json<serde_json::Value> {
    Json(serde_json::json!({
        "product_count": state.purchase_engine.catalog().count(),
        "status": "active"
    }))
}

async fn handle_chat_filter(
    axum::extract::State(state): axum::extract::State<Arc<AppState>>,
    Json(payload): Json<serde_json::Value>,
) -> Json<serde_json::Value> {
    let msg = payload.get("message").and_then(|v| v.as_str()).unwrap_or("");
    let forbidden = state.chat_filter.inspect_message(msg);
    Json(serde_json::json!({
        "allowed": forbidden.is_none(),
        "forbidden_word": forbidden
    }))
}

async fn get_dragon_policy(
    axum::extract::State(state): axum::extract::State<Arc<AppState>>,
) -> Json<DragonFlightPolicy> {
    Json(state.dragon_policy.clone())
}

async fn get_bloodmoon_status(
    axum::extract::State(state): axum::extract::State<Arc<AppState>>,
) -> Json<serde_json::Value> {
    let blood_moon = state.blood_moon.lock().unwrap();
    Json(serde_json::json!({
        "is_active": blood_moon.is_active()
    }))
}

async fn handle_store_webhook(
    axum::extract::State(state): axum::extract::State<Arc<AppState>>,
    Json(payload): Json<StorePurchasePayload>,
) -> Json<serde_json::Value> {
    info!("[STORE] Procesando compra para {}: {}", payload.player_name, payload.package_id);
    let success = state.purchase_engine.process_purchase(payload.clone());
    Json(serde_json::json!({
        "success": success,
        "transaction_id": payload.transaction_id,
        "message": "Compra registrada en Odysseia Core (Rust)"
    }))
}

async fn handle_telemetry_report(
    Json(report): Json<ServerStatusReport>,
) -> Json<serde_json::Value> {
    let msg = report.build_discord_status_message();
    info!("[TELEMETRY] Reporte recibido de Purpur: TPS 1m={:.2}", report.tps_1m);
    Json(serde_json::json!({
        "status": "received",
        "formatted_discord": msg
    }))
}
