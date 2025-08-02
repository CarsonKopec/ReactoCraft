use noise::{NoiseFn, Perlin};

#[unsafe(no_mangle)]
pub extern "C" fn generate_chunk(x: i32, z: i32, out_len: *mut usize) -> *mut u8 {
    let chuck_size = 16;
    let scale = 0.05;
    let perlin = Perlin::new();

    let mut heightmap: Vec<u8> = Vec::with_capacity((chuck_size * chuck_size) as usize);

    for i in 0..chuck_size {
        for j in 0..chuck_size {
            let x_coord = x as f64 + i as f64;
            let z_coord = z as f64 + i as f64;
            let noise_value = perlin.get([x_coord * scale, z_coord * scale]);

            let height = ((noise_value + 1.0) * 65.0 + 50.0) as u8;
            heightmap.push(height);
        }
    }

    unsafe {
        *out_len = heightmap.len();
        let ptr = heightmap.as_ptr() as *mut u8;
        std::mem::forget(heightmap);
        ptr
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn generate_flat_chunk(x: i32, z: i32, out_len: *mut usize) -> *mut u8 {
    let chunk_size = 16;
    let height = 64;

    let mut chunk = Vec::with_capacity(chunk_size * height * chunk_size);

    for y in 0..height {
        for z_pos in 0..chunk_size {
            for x_pos in 0..chunk_size {
                let block_id = match y {
                    0 => 3,       // bedrock
                    1..=3 => 2,   // dirt
                    4 => 1,       // grass
                    _ => 0,       // air
                };
                chunk.push(block_id);
            }
        }
    }

    unsafe {
        *out_len = chunk.len();
        let ptr = chunk.as_ptr() as *mut u8;
        std::mem::forget(chunk);
        ptr
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn free_buffer(ptr: *mut u8, len: usize) {
    if ptr.is_null() {
        return;
    }
    let slice = unsafe { std::slice::from_raw_parts_mut(ptr, len) };
    unsafe {
        let _ = Box::from_raw(slice);
    }
}
