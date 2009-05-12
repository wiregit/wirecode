package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.OSUtils;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class LibTorrentWrapper implements LibTorrent {

    private static final Log LOG = LogFactory.getLog(LibTorrentWrapper.class);
    
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
    public void add_torrent(LibTorrentInfo info, String path, 
            LongHeap longHeap, Sha1Heap sha1Heap, Pointer ptr) {
        LOG.debugf("before add_torrent: {0}", path);
        libTorrent.add_torrent(info, path, longHeap, sha1Heap, ptr);
        LOG.debugf("after add_torrent: {0}", path);
    }

    @Override
    public void init(String path) {
        LOG.debugf("before init: {0}", path);
        libTorrent.init(path);
        LOG.debugf("after init: {0}", path);
    }

    @Override
    public void get_alerts(AlertCallback alertCallback) {
        LOG.debug("before get_alerts");
        libTorrent.get_alerts(alertCallback);
        LOG.debug("after get_alerts");
    }

    @Override
    public int pause_torrent(String id) {
        LOG.debugf("before pause_torrent: {0}", id);
        int ret = libTorrent.pause_torrent(id);
        LOG.debugf("after pause_torrent: {0}", id);
        return ret;
    }

    @Override
    public int resume_torrent(String id) {
        LOG.debugf("before resume_torrent: {0}", id);
        int ret = libTorrent.resume_torrent(id);
        LOG.debugf("after resume_torrent: {0}", id);
        return ret;
    }

    @Override
    public void get_torrent_status(String id, LibTorrentStatus status, 
            LongHeap longHeap1, LongHeap longHeap2, LongHeap longHeap3) {
        LOG.debugf("before get_torrent_status: {0}", id);
        libTorrent.get_torrent_status(id, status, longHeap1, longHeap2, longHeap3);
        LOG.debugf("after get_torrent_status: {0}", id);
    }

    @Override
    public int remove_torrent(String id) {
        LOG.debugf("before remove_torrent: {0}", id);
        int ret = libTorrent.remove_torrent(id);
        LOG.debugf("after remove_torrent: {0}", id);
        return ret;
    }
    
    @Override
    public void get_peers(String id, Memory memory) {
        LOG.debugf("before get_peers: {0}", id);
        libTorrent.get_peers(id, memory);
        LOG.debugf("after get_peers: {0}", id);
    }

    @Override
    public int get_num_peers(String id) {
        LOG.debugf("before get_num_peers: {0}", id);
        int ret = libTorrent.get_num_peers(id);
        LOG.debugf("after get_num_peers: {0} - {1}", id, ret);
        return ret;
    }

    @Override 
    public boolean signal_fast_resume_data_request(String id) {
        LOG.debugf("before print signal_fast_resume_data_request: {0}", id);
        boolean ret = libTorrent.signal_fast_resume_data_request(id);
        LOG.debugf("after print signal_fast_resume_data_request: {0} - {1}", id, ret);
        return ret;
    }

    @Override
    public boolean move_torrent(String id, String absolutePath) {
        LOG.debugf("before move_torrent: {0} - {1}", id, absolutePath);
        // TODO libtorrent documentation says this method will only work if the
        // new path is on the same device, might need a fallback plan
        boolean ret = libTorrent.move_torrent(id, absolutePath);
        LOG.debugf("after move_torrent: {0} - {1}", id, absolutePath);
        return ret;
    }

    @Override
    public void abort_torrents() {
        LOG.debug("before abort");
        libTorrent.abort_torrents();
        LOG.debug("after abort");
    }

    @Override
    public void add_torrent_existing(String sha1, String trackerURI, String fastResumeData) {
        LOG.debugf("before add_torrent_old: {0} - {1}", sha1, trackerURI);
        libTorrent.add_torrent_existing(sha1, trackerURI, fastResumeData);
        LOG.debugf("after add_torrent_old: {0} - {1}", sha1, trackerURI);
    }

}
