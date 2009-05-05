package org.limewire.libtorrent.callback;

import org.limewire.libtorrent.LibTorrentAlert;

import com.sun.jna.Callback;

public interface AlertCallback extends Callback {
    public void callback(LibTorrentAlert alert);
}
