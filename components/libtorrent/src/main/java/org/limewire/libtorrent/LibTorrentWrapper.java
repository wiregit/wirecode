package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class LibTorrentWrapper implements LibTorrent {

    private final LibTorrent libTorrent;

    public LibTorrentWrapper() {
        System.loadLibrary("test");
        System.loadLibrary("mingwm10");
        // System.loadLibrary("msvcrt");
        // System.loadLibrary("ws2_32");
        //System.loadLibrary("shlwapi");
        System.loadLibrary("torrent");
        System.loadLibrary("boost_system-mgw34-mt-1_36");
        System.loadLibrary("boost_date_time-mgw34-mt-1_36");
        System.loadLibrary("boost_filesystem-mgw34-mt-1_36");
        System.loadLibrary("boost_thread-mgw34-mt-1_36");

        this.libTorrent = (LibTorrent) Native.loadLibrary("torrent-wrapper", LibTorrent.class);
    }

    @Override
    public LibTorrentInfo add_torrent(String path) {
        return libTorrent.add_torrent(path);
    }

    @Override
    public void init(String path) {
        libTorrent.init(path);
    }

    @Override
    public void get_alerts(AlertCallback alertCallback) {
        libTorrent.get_alerts(alertCallback);
    }

    @Override
    public int pause_torrent(String id) {
        return libTorrent.pause_torrent(id);
    }

    @Override
    public int resume_torrent(String id) {
        return libTorrent.resume_torrent(id);
    }

    @Override
    public boolean is_torrent_paused(String id) {
        return libTorrent.is_torrent_paused(id);
    }

    @Override
    public LibTorrentStatus get_torrent_status(String id) {
        int size = new LibTorrentStatus().size();
        Memory memory = new Memory(size);
        return libTorrent.get_torrent_status(id, memory);
    }

    @Override
    public LibTorrentStatus get_torrent_status(String id, Memory memory) {
        return libTorrent.get_torrent_status(id);
    }

    @Override
    public boolean is_torrent_finished(String id) {
        return libTorrent.is_torrent_finished(id);
    }

    @Override
    public boolean is_torrent_valid(String id) {
        return libTorrent.is_torrent_valid(id);
    }

    @Override
    public boolean is_torrent_seed(String id) {
        return libTorrent.is_torrent_seed(id);
    }

    @Override
    public int remove_torrent(String id) {
        return libTorrent.remove_torrent(id);
    }

    @Override
    public void print() {
        libTorrent.print();
    }

}
