use crossbeam_channel::{Receiver, Sender, bounded};
use std::ffi::c_char;
use std::sync::OnceLock;
use std::thread::JoinHandle;
use std::{slice, thread};
use std::time::Duration;

use reqwest::blocking::Client; // can I use non-blocking here?

// same value, but different meanings
// TODO find a way to set the buffer size from java.
// why not just add it to the function
const CAPACITY: isize = 32760;
const READ: isize = 32760;
static CHANNEL: OnceLock<(Sender<String>, Receiver<String>)> = OnceLock::new();
static HANDLE: OnceLock<JoinHandle<()>> = OnceLock::new();

/// Reads the data from the bytebuffer in the caller thread and sends the data to a background
/// thread that updates the datastore
///
/// # Safety
///
/// The function is unsafe for skipped checks on UTF-8 and string length and because it reads from a
/// mutable raw pointer.
/// Still it's guaranteed to be safe because
/// 1. We make sure the part that's read is not being mutated at the same time (happens in the same thread)  
/// 2. don't need to check the length since it's calculated and stored within the byte buffer
/// 3. the bytes are guaranteed to be UTF-8
#[no_mangle]
pub unsafe extern "C" fn buffer_updated(buffer: *mut c_char) {
    // using a channel for the bytes read from the buffer
    // this decouples the originating from the http request
    let (sender, receiver) = CHANNEL.get_or_init(|| bounded(1000));
    HANDLE.get_or_init(|| {
        thread::spawn(move || {
            let http_client = Client::new();
            loop {
                let maybe_job = receiver.recv();
                if let Ok(data) = maybe_job {
                    _ = http_client
                        .post("http://localhost:3000/api/stacktraces")
                        .body(data)
                        .send();
                }
            }
        })
    });
    let mut read_pos = get_u32(buffer, READ) as isize;
    if read_pos == CAPACITY {
        read_pos = 0;
    }

    let mut remaining = CAPACITY - read_pos; // nr of bytes to read before end of buffer
    let len = if remaining == 1 {
        let byte_high = get_u8(buffer, read_pos);
        read_pos = 0;
        let byte_low = get_u8(buffer, read_pos);
        read_pos += 1;
        let l = (byte_high as u16) << 8 | byte_low as u16;
        remaining = l as isize;
        l
    } else if remaining == 2 {
        let l = get_u16(buffer, read_pos);
        read_pos = 0;
        remaining = 0;
        l
    } else {
        let l = get_u16(buffer, read_pos);
        read_pos += 2;
        remaining -= 2;
        l
    } as isize;

    // must copy to maintain it safely once read from the buffer
    // can safely skip checks for len and utf8
    if len <= remaining {
        let s = std::str::from_utf8_unchecked(slice::from_raw_parts(
            buffer.offset(read_pos).cast::<u8>(),
            len as usize,
        )).to_owned();
        let send_result = sender.send_timeout(s, Duration::from_secs(10));
        if send_result.is_err() {
            println!("overflow detected, discarding");
        }
        read_pos += len;
    } else {
        let s1 = std::str::from_utf8_unchecked(slice::from_raw_parts(
            buffer.offset(read_pos).cast::<u8>(),
            remaining as usize,
        ));
        let s2 = std::str::from_utf8_unchecked(slice::from_raw_parts(
            buffer.cast::<u8>(),
            (len - remaining) as usize,
        ));
        let mut s = String::with_capacity(len as usize);
        s.push_str(s1);
        s.push_str(s2);
        let send_result = sender.send_timeout(s, Duration::from_secs(10));
        if send_result.is_err() {
            println!("overflow detected, discarding");
        }

        read_pos = len - remaining;
    }
    put_u32(buffer, READ, read_pos as u32);
}

fn get_u8(s: *const c_char, pos: isize) -> u8 {
    unsafe { *s.offset(pos) as u8 }
}

fn get_u16(s: *const c_char, pos: isize) -> u16 {
    let mut b: [u8; 2] = [0; 2];
    unsafe {
        b[0] = *s.offset(pos) as u8;
        b[1] = *s.offset(pos + 1) as u8;
    }
    u16::from_be_bytes(b)
}

fn get_u32(s: *mut c_char, pos: isize) -> u32 {
    let mut b: [u8; 4] = [0; 4];
    unsafe {
        b[0] = *s.offset(pos) as u8;
        b[1] = *s.offset(pos + 1) as u8;
        b[2] = *s.offset(pos + 2) as u8;
        b[3] = *s.offset(pos + 3) as u8;
    }
    u32::from_be_bytes(b)
}

fn put_u32(s: *mut c_char, pos: isize, value: u32) {
    let bytes = u32::to_be_bytes(value);
    unsafe {
        *s.offset(pos) = bytes[0] as c_char;
        *s.offset(pos + 1) = bytes[1] as c_char;
        *s.offset(pos + 2) = bytes[2] as c_char;
        *s.offset(pos + 3) = bytes[3] as c_char;
    }
}
