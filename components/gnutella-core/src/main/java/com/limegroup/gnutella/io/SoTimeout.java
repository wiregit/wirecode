package com.limegroup.gnutella.io;

import java.net.SocketException;

public interface SoTimeout {

    public int getSoTimeout() throws SocketException;
}
