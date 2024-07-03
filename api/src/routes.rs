use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::routing::{get, post};
use axum::Router;

use crate::handlers::stacktraces::{create_stacktrace, get_stacktrace, list_stacktraces};
use crate::AppState;

pub fn app_router(state: AppState) -> Router<AppState> {
    Router::new()
        .route("/", get(root))
        .nest("/api/stacktraces", stacktraces_routes(state.clone()))
        .fallback(handler_404)
}

async fn root() -> &'static str {
    "Server is running!"
}

async fn handler_404() -> impl IntoResponse {
    (
        StatusCode::NOT_FOUND,
        "The requested resource was not found",
    )
}

fn stacktraces_routes(state: AppState) -> Router<AppState> {
    Router::new()
        .route("/", post(create_stacktrace))
        .route("/", get(list_stacktraces))
        .route("/:id", get(get_stacktrace))
        .with_state(state)
}
