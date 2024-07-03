use axum::extract::State;
use axum::Json;

use crate::domain::models::post::{StacktraceError, SimpleStacktraceModel};
use crate::handlers::stacktraces::StacktraceResponse;
use crate::infra::errors::InfraError;
use crate::infra::repositories::stacktrace_repository;
use crate::utils::PathExtractor;
use crate::AppState;

pub async fn get_stacktrace(
    State(state): State<AppState>,
    PathExtractor(post_id): PathExtractor<i32>,
) -> Result<Json<StacktraceResponse>, StacktraceError> {
    let post =
        stacktrace_repository::get(&state.pool, post_id)
            .await
            .map_err(|db_error| match db_error {
                InfraError::InternalServerError => StacktraceError::InternalServerError,
                InfraError::NotFound => StacktraceError::NotFound(post_id),
            })?;

    Ok(Json(adapt_post_to_post_response(post)))
}

fn adapt_post_to_post_response(post: SimpleStacktraceModel) -> StacktraceResponse {
    StacktraceResponse {
        id: post.id,
        stacktrace: post.stacktrace,
    }
}
