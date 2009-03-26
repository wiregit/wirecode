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
        INVALID_HANDLE_VALUE invalidHandle = new INVALID_HANDLE_VALUE();
        if (invalidHandle.equals(o)) {
            return invalidHandle;
        }
        return o;
    }
}