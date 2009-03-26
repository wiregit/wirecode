/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.FromNativeContext;
import com.sun.jna.PointerType;

public class HANDLE extends PointerType {
    public HANDLE() {
        super();
    }

    /** Override to the appropriate object for INVALID_HANDLE_VALUE. */
    public Object fromNative(Object nativeValue, FromNativeContext context) {
        Object o = super.fromNative(nativeValue, context);
        if (INVALID_HANDLE_VALUE.INVALID_HANDLE.equals(o)) {
            return INVALID_HANDLE_VALUE.INVALID_HANDLE;
        }
        return o;
    }
}