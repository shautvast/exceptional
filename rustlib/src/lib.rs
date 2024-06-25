mod throwable;

use std::ffi::{c_char, CStr};

#[no_mangle]
pub extern "C" fn log_java_exception(raw_string: *const c_char) {
    let c_str = unsafe { CStr::from_ptr(raw_string) };
    let string = c_str.to_str();
    if let Ok(json) = string {
        println!("receiving {}", json);
        let error: throwable::Throwable = serde_json::from_str(json).unwrap();
        println!("{:?}", error);
    }
}

#[cfg(test)]
mod tests {}
