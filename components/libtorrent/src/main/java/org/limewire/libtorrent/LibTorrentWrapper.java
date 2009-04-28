package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.util.OSUtils;

import com.sun.jna.Memory;
import com.sun.jna.Native;

public class LibTorrentWrapper implements LibTorrent {

    private LibTorrent libTorrent;

    @Override
    public void initialize() {
        if (OSUtils.isWindows()) {
            System.loadLibrary("mingwm10");
            System.loadLibrary("boost_system-mgw34-mt-1_38");
            System.loadLibrary("boost_date_time-mgw34-mt-1_38");
            System.loadLibrary("boost_filesystem-mgw34-mt-1_38");
            System.loadLibrary("boost_thread-mgw34-mt-1_38");
            System.loadLibrary("torrent");
        } else if (OSUtils.isLinux()) {
            // compile into torrent-wrapper.so
        } else if (OSUtils.isMacOSX()) {
            System.loadLibrary("torrent");
        }

        // TODO make sure right libraries are loaded on linux too.
        this.libTorrent = (LibTorrent) Native.loadLibrary("torrent-wrapper", LibTorrent.class);
    }

    @Override
    public LibTorrentInfo add_torrent(String path) {
        log("before add_torrent: " + path);
        LibTorrentInfo info = libTorrent.add_torrent(path);
        log("after add_torrent: " + path);
        return info;
    }

    private void log(String message) {
        System.out.println(message);
    }

    @Override
    public void init(String path) {
        log("before init: " + path);
        libTorrent.init(path);
        log("after init: " + path);
    }

    @Override
    public void get_alerts(AlertCallback alertCallback) {
        log("before get_alerts");
        libTorrent.get_alerts(alertCallback);
        log("after get_alerts");
    }

    @Override
    public int pause_torrent(String id) {
        log("before pause_torrent: " + id);
        int ret = libTorrent.pause_torrent(id);
        log("after pause_torrent: " + id);
        return ret;
    }

    @Override
    public int resume_torrent(String id) {
        log("before resume_torrent: " + id);
        int ret = libTorrent.resume_torrent(id);
        log("after resume_torrent: " + id);
        return ret;
    }

    @Override
    public LibTorrentStatus get_torrent_status(String id) {
        log("before get_torrent_status: " + id);
        int size = new LibTorrentStatus().size();
        Memory memory = new Memory(size);
        LibTorrentStatus status = libTorrent.get_torrent_status(id, memory);
        log("after get_torrent_status: " + id);
        return status;
    }

    @Override
    public LibTorrentStatus get_torrent_status(String id, Memory memory) {
        log("before get_torrent_status: " + id);
        LibTorrentStatus status = libTorrent.get_torrent_status(id);
        log("after get_torrent_status: " + id);
        return status;
    }

    @Override
    public int remove_torrent(String id) {
        log("before remove_torrent: " + id);
        int ret = libTorrent.remove_torrent(id);
        log("after remove_torrent: " + id);
        return ret;
    }

    @Override
    public void print() {
        log("before print");
        libTorrent.print();
        log("after print");
    }

    @Override
    public boolean move_torrent(String id, String absolutePath) {
        log("before move_torrent: " + id + " - " + absolutePath);
        // TODO libtorrent documentation says this method will only work if the
        // new path is on the same device, might need a fallback plan
        boolean ret = libTorrent.move_torrent(id, absolutePath);
        log("after move_torrent: " + id + " - " + absolutePath);
        return ret;
    }

    @Override
    public void abort_torrents() {
        log("before abort");
        libTorrent.abort_torrents();
        log("after abort");
    }

}
