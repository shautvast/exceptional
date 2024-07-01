use std::ffi::c_char;

mod throwable;

const CAPACITY: isize = 32760;
const READ: isize = 32760;

#[no_mangle]
pub extern "C" fn buffer_updated(buffer: *mut c_char) {
    let mut read_pos = get_u32(buffer, READ) as isize;

    let mut remaining = CAPACITY - read_pos;
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

    let mut result = Vec::with_capacity(len as usize);
    if len <= remaining {
        for i in 0..len {
            unsafe { result.push(*buffer.offset(read_pos + i) as u8); }
        }
        read_pos += len;
    } else {
        for i in 0..remaining {
            unsafe { result.push(*buffer.offset(read_pos + i) as u8); }
        }
        read_pos = 0;
        for i in 0..len - remaining {
            unsafe { result.push(*buffer.offset(i) as u8); }
        }
        read_pos += len - remaining;
    }
    put_u32(buffer, READ, read_pos as u32);

    let string = String::from_utf8(result);
    if let Ok(json) = string {
        println!("receiving {}", json);
    } else {
        println!("not ok");
    }
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