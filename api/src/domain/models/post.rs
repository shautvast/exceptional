use axum::http::StatusCode;
use axum::Json;
use axum::response::IntoResponse;
use serde::{Deserialize, Serialize};
use serde_json::json;

use crate::infra::errors::InfraError;

// #[derive(Clone, Debug, PartialEq)]
// pub struct PostModel {
//     pub id: Uuid,
//     pub title: String,
//     pub body: String,
//     pub published: bool,
// }

#[derive(Clone, Debug, PartialEq)]
pub struct SimpleStacktraceModel {
    pub id: i32,
    pub stacktrace: String,
}

#[derive(Debug)]
pub enum StacktraceError {
    InternalServerError,
    NotFound(i32),
    InfraError(InfraError),
}

impl IntoResponse for StacktraceError {
    fn into_response(self) -> axum::response::Response {
        let (status, err_msg) = match self {
            Self::NotFound(id) => (
                StatusCode::NOT_FOUND,
                format!("StacktraceModel with id {} has not been found", id),
            ),
            Self::InfraError(db_error) => (
                StatusCode::INTERNAL_SERVER_ERROR,
                format!("Internal server error: {}", db_error),
            ),
            _ => (
                StatusCode::INTERNAL_SERVER_ERROR,
                String::from("Internal server error"),
            ),
        };
        (
            status,
            Json(
                json!({"resource":"StacktraceModel", "message": err_msg, "happened_at" : chrono::Utc::now() }),
            ),
        )
            .into_response()
    }
}
