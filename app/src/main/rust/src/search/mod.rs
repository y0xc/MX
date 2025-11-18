pub mod types;
pub mod lexer;
pub mod parser;
pub mod manager;
pub mod result_manager;
pub mod jni;

#[cfg(test)]
pub mod mock_memory;

pub use types::{SearchMode, SearchQuery, SearchValue, ValueType};
pub use parser::parse_search_query;
pub use manager::{SearchEngineManager, SEARCH_ENGINE_MANAGER, SearchProgressCallback};
pub use result_manager::SearchResultItem;
