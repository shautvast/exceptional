use axum::extract::{Query, State};
use axum::Json;

use crate::domain::models::post::{StacktraceError, SimpleStacktraceModel};
use crate::handlers::stacktraces::{ListStacktracesResponse, StacktraceResponse};
use crate::infra::repositories::stacktrace_repository::{get_all};
use crate::AppState;

pub async fn list_stacktraces(
    State(state): State<AppState>,
) -> Result<Json<ListStacktracesResponse>, StacktraceError> {
    let stacktraces = get_all(&state.pool)
        .await
        .map_err(|_| StacktraceError::InternalServerError)?;

    Ok(Json(adapt_posts_to_list_posts_response(stacktraces)))
}

fn adapt_post_to_post_response(post: SimpleStacktraceModel) -> StacktraceResponse {
    StacktraceResponse {
        id: post.id,
        stacktrace: post.stacktrace,
    }
}

fn adapt_posts_to_list_posts_response(posts: Vec<SimpleStacktraceModel>) -> ListStacktracesResponse {
    let posts_response: Vec<StacktraceResponse> =
        posts.into_iter().map(adapt_post_to_post_response).collect();

    ListStacktracesResponse {
        stacktraces: posts_response,
    }
}
