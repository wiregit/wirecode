package org.limewire.libtorrent.callback;

import com.sun.jna.Callback;

public interface TorrentFinishedCallback extends Callback {
	public void callback(String message, int i);
}
