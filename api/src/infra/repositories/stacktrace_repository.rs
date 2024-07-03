use diesel::{
    ExpressionMethods, Insertable, QueryDsl, Queryable, RunQueryDsl,
    Selectable, SelectableHelper,
};
use serde::{Deserialize, Serialize};

use crate::domain::models::post::{SimpleStacktraceModel};
use crate::infra::db::schema::stacktraces;
use crate::infra::errors::{adapt_infra_error, InfraError};

#[derive(Serialize, Queryable, Selectable)]
#[diesel(table_name = stacktraces)]
#[diesel(check_for_backend(diesel::pg::Pg))]
pub struct StacktraceDb {
    pub id: i32,
    pub stacktrace: String,
}

#[derive(Insertable)]
#[diesel(table_name = stacktraces)]
pub struct NewPostDb {
    pub stacktrace: String,
}

pub async fn insert(
    pool: &deadpool_diesel::postgres::Pool,
    new_post: NewPostDb,
) -> Result<SimpleStacktraceModel, InfraError> {
    let conn = pool.get().await.map_err(adapt_infra_error)?;
    let res = conn
        .interact(|conn| {
            diesel::insert_into(stacktraces::table)
                .values(new_post)
                .returning(StacktraceDb::as_returning())
                .get_result(conn)
        })
        .await
        .map_err(adapt_infra_error)?
        .map_err(adapt_infra_error)?;

    Ok(adapt_stacktracedb_to_stacktracemodel(res))
}

pub async fn get(
    pool: &deadpool_diesel::postgres::Pool,
    id: i32,
) -> Result<SimpleStacktraceModel, InfraError> {
    let conn = pool.get().await.map_err(adapt_infra_error)?;
    let res = conn
        .interact(move |conn| {
            stacktraces::table
                .filter(stacktraces::id.eq(id))
                .select(StacktraceDb::as_select())
                .get_result(conn)
        })
        .await
        .map_err(adapt_infra_error)?
        .map_err(adapt_infra_error)?;

    Ok(adapt_stacktracedb_to_stacktracemodel(res))
}

pub async fn get_all(
    pool: &deadpool_diesel::postgres::Pool,
) -> Result<Vec<SimpleStacktraceModel>, InfraError> {
    let conn = pool.get().await.map_err(adapt_infra_error)?;
    let res = conn
        .interact(move |conn| {
            let query = stacktraces::table.into_boxed::<diesel::pg::Pg>();

            query.select(StacktraceDb::as_select()).load::<StacktraceDb>(conn)
        })
        .await
        .map_err(adapt_infra_error)?
        .map_err(adapt_infra_error)?;

    let posts: Vec<SimpleStacktraceModel> = res
        .into_iter()
        .map(|post_db| adapt_stacktracedb_to_stacktracemodel(post_db))
        .collect();

    Ok(posts)
}

fn adapt_stacktracedb_to_stacktracemodel(post_db: StacktraceDb) -> SimpleStacktraceModel {
    SimpleStacktraceModel {
        id: post_db.id,
        stacktrace: post_db.stacktrace,
    }
}
