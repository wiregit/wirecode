package org.limewire.libtorrent.callback;

import com.sun.jna.Callback;

public interface AlertCallback extends Callback {
	
	public void callback(String message);
}
