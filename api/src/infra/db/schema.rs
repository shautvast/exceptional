diesel::table! {
    stacktraces (id) {
        id -> Integer,
        stacktrace -> Varchar,
    }
}
