use serde::Deserialize;

#[derive(Deserialize, Debug)]
pub struct Throwable {
    cause: Option<Box<Throwable>>,
    #[serde(rename (deserialize = "stackTrace"))]
    stack_trace: Vec<Stacktrace>,
    message: Option<String>,
    suppressed: Vec<String>,
    #[serde(rename (deserialize = "localizedMessage"))]
    localized_message: Option<String>,
}

#[derive(Deserialize, Debug)]
pub struct Stacktrace{
    #[serde(rename (deserialize = "classLoaderName"))]
    class_loader_name: Option<String>,
    #[serde(rename (deserialize = "moduleName"))]
    module_name: Option<String>,
    #[serde(rename (deserialize = "moduleVersion"))]
    module_version: Option<String>,
    #[serde(rename (deserialize = "methodName"))]
    method_name: Option<String>,
    #[serde(rename (deserialize = "fileName"))]
    file_name: Option<String>,
    #[serde(rename (deserialize = "lineNumber"))]
    line_number: Option<u32>,
    #[serde(rename (deserialize = "className"))]
    class_name: Option<String>,
    #[serde(rename (deserialize = "nativeMethod"))]
    native_method: Option<bool>,
}