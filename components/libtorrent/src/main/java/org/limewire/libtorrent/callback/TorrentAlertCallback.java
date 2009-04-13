package org.limewire.libtorrent.callback;

import com.sun.jna.Callback;

public interface TorrentAlertCallback extends Callback {
	public void callback(String id, String message);
}
