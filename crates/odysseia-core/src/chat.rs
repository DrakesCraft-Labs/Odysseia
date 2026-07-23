use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatFilterRule {
    pub forbidden_words: Vec<String>,
    pub max_warnings: u32,
}

impl Default for ChatFilterRule {
    fn default() -> Self {
        Self {
            forbidden_words: vec![
                "hacks".to_string(),
                "dupe".to_string(),
                "xray".to_string(),
                "killaura".to_string(),
            ],
            max_warnings: 3,
        }
    }
}

pub struct ChatFilterEngine {
    rule: ChatFilterRule,
}

impl ChatFilterEngine {
    pub fn new(rule: ChatFilterRule) -> Self {
        Self { rule }
    }

    pub fn inspect_message(&self, message: &str) -> Option<String> {
        let lower = message.to_lowercase();
        for word in &self.rule.forbidden_words {
            if lower.contains(word) {
                return Some(word.clone());
            }
        }
        None
    }
}
