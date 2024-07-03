use serde::{Deserialize, Serialize};

pub use create_stacktrace::create_stacktrace;
pub use get_stacktrace::get_stacktrace;
pub use list_stacktraces::list_stacktraces;

mod create_stacktrace;
mod get_stacktrace;
mod list_stacktraces;

#[derive(Debug, Deserialize)]
pub struct CreatePostRequest {
    stacktrace: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct StacktraceResponse {
    id: i32,
    stacktrace: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ListStacktracesResponse {
    stacktraces: Vec<StacktraceResponse>,
}
