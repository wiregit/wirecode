/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.win32.StdCallLibrary.StdCallCallback;
// TODO: figure out how OVERLAPPED is used and apply an appropriate mapping

public interface OVERLAPPED_COMPLETION_ROUTINE extends StdCallCallback {
    void callback(int errorCode, int nBytesTransferred, OVERLAPPED overlapped);
}