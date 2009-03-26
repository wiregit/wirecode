package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.ptr.PointerByReference;

public class Kernel32Utils {
    public static String getSystemError(int code) {
        Kernel32 lib = Kernel32.INSTANCE;
        PointerByReference pref = new PointerByReference();
        lib.FormatMessage(Kernel32.FORMAT_MESSAGE_ALLOCATE_BUFFER
                | Kernel32.FORMAT_MESSAGE_FROM_SYSTEM | Kernel32.FORMAT_MESSAGE_IGNORE_INSERTS,
                null, code, 0, pref, 0, null);
        String s = pref.getValue().getString(0, !Boolean.getBoolean("w32.ascii"));
        s = s.replace(".\r", ".").replace(".\n", ".");
        lib.LocalFree(pref.getValue());
        return s;
    }
}
