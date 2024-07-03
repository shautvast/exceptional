use axum::extract::State;
use axum::Json;
use tracing::info;

use crate::AppState;
use crate::domain::models::post::StacktraceError;
use crate::handlers::stacktraces::StacktraceResponse;
use crate::infra::repositories::stacktrace_repository;

static mut counter: usize = 0;

pub async fn create_stacktrace(
    State(state): State<AppState>,
    data: String,
) -> Result<Json<StacktraceResponse>, StacktraceError> {
    unsafe {
        counter += 1;
    }
    let new_post_db = stacktrace_repository::NewPostDb {
        stacktrace: data,
    };

    let created_stacktrace = stacktrace_repository::insert(&state.pool, new_post_db)
        .await
        .map_err(StacktraceError::InfraError)?;

    let post_response = StacktraceResponse {
        id: created_stacktrace.id,
        stacktrace: created_stacktrace.stacktrace,
    };

    Ok(Json(post_response))
}
