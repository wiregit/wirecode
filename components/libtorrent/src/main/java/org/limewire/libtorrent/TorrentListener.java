package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.libtorrent.callback.TorrentFinishedCallback;

public class TorrentListener implements AlertCallback, TorrentFinishedCallback {
	@Override
	public void callback(String message) {
		System.out.println(message);
	}

	@Override
	public void callback(String message, int i) {
		System.out.println("Completed: " + message);
	}
}
