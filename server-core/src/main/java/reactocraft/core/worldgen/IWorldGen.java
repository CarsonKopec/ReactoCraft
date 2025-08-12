package reactocraft.core.worldgen;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public interface IWorldGen extends Library {
    IWorldGen INSTANCE = Native.load("worldgen", IWorldGen.class);

    // Chuck generators
    Pointer generate_chunk(int x, int y, IntByReference outLen);
    Pointer generate_flat_chunk(int x, int y, IntByReference outLen);

    void free_buffer(Pointer buf, int len);
}
