use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::RwLock;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProductDefinition {
    pub id: String,
    pub name: String,
    pub price: f64,
    pub category: String,
    pub commands: Vec<String>,
    pub kit_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StorePurchasePayload {
    pub transaction_id: String,
    pub player_name: String,
    pub player_uuid: String,
    pub package_id: String,
    pub price: f64,
    pub currency: String,
}

pub struct ProductCatalog {
    products: HashMap<String, ProductDefinition>,
}

impl ProductCatalog {
    pub fn new() -> Self {
        let mut products = HashMap::new();
        // Slimefun Basic Dusts & Ranks
        products.insert(
            "iron_dust".to_string(),
            ProductDefinition {
                id: "iron_dust".to_string(),
                name: "Iron Dust (Slimefun)".to_string(),
                price: 10.0,
                category: "slimefun_dusts".to_string(),
                commands: vec!["sf give {player} IRON_DUST 8".to_string()],
                kit_id: None,
            },
        );
        products.insert(
            "gold_dust".to_string(),
            ProductDefinition {
                id: "gold_dust".to_string(),
                name: "Gold Dust (Slimefun)".to_string(),
                price: 15.0,
                category: "slimefun_dusts".to_string(),
                commands: vec!["sf give {player} GOLD_DUST 8".to_string()],
                kit_id: None,
            },
        );
        products.insert(
            "vip_rank".to_string(),
            ProductDefinition {
                id: "vip_rank".to_string(),
                name: "Rango VIP DrakesCraft".to_string(),
                price: 99.0,
                category: "ranks".to_string(),
                commands: vec!["lp user {player} parent set vip".to_string()],
                kit_id: Some("vip".to_string()),
            },
        );

        Self { products }
    }

    pub fn get(&self, id: &str) -> Option<&ProductDefinition> {
        self.products.get(id)
    }

    pub fn count(&self) -> usize {
        self.products.len()
    }
}

pub struct PurchaseEngine {
    catalog: ProductCatalog,
    pending_kits: RwLock<HashMap<String, Vec<String>>>,
}

impl PurchaseEngine {
    pub fn new() -> Self {
        Self {
            catalog: ProductCatalog::new(),
            pending_kits: RwLock::new(HashMap::new()),
        }
    }

    pub fn catalog(&self) -> &ProductCatalog {
        &self.catalog
    }

    pub fn process_purchase(&self, payload: StorePurchasePayload) -> bool {
        let mut map = self.pending_kits.write().unwrap();
        if let Some(product) = self.catalog.get(&payload.package_id) {
            if let Some(ref kit) = product.kit_id {
                map.entry(payload.player_uuid)
                    .or_default()
                    .push(kit.clone());
            }
        }
        true
    }

    pub fn get_pending_kits(&self, player_uuid: &str) -> Vec<String> {
        let map = self.pending_kits.read().unwrap();
        map.get(player_uuid).cloned().unwrap_or_default()
    }
}
