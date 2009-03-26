/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class SECURITY_ATTRIBUTES extends Structure {
    public int nLength = size();
    public Pointer lpSecurityDescriptor;
    public boolean bInheritHandle;
}