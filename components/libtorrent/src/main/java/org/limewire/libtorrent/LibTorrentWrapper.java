package org.limewire.libtorrent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.OSUtils;

import com.sun.jna.Memory;
import com.sun.jna.Native;

public class LibTorrentWrapper {

    private static final Log LOG = LogFactory.getLog(LibTorrentWrapper.class);
    
    private LibTorrent libTorrent;

    /**
     * Initializes the LibTorrent library. Finding necessary dependencies first,
     * then loading the libtorrent library as a jna lib.
     */
    public void initialize(String path) {
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
          //  System.loadLibrary("stdc++.6");
           // System.loadLibrary("gcc_s.1");
           // System.loadLibrary("System.B");
        }

        // TODO make sure right libraries are loaded on linux too.
        this.libTorrent = (LibTorrent) Native.loadLibrary("torrent-wrapper", LibTorrent.class);
        
        init(path);
    }

    public void add_torrent(String path, String fastResumePath) {
        LOG.debugf("before add_torrent: {0}", path);
        catchWrapperException(libTorrent.add_torrent(path, fastResumePath));
        LOG.debugf("after add_torrent: {0}", path);
    }

    private void init(String path) {
        LOG.debugf("before init: {0}", path);
        catchWrapperException(libTorrent.init(path));
        LOG.debugf("after init: {0}", path);
    }

    public void freeze_and_save_all_fast_resume_data() {
        LOG.debug("before get_alerts");
        catchWrapperException(libTorrent.freeze_and_save_all_fast_resume_data());
        LOG.debug("after get_alerts");
    }

    public void pause_torrent(String id) {
        LOG.debugf("before pause_torrent: {0}", id);
        catchWrapperException(libTorrent.pause_torrent(id));
        LOG.debugf("after pause_torrent: {0}", id);
        
    }

    public void resume_torrent(String id) {
        LOG.debugf("before resume_torrent: {0}", id);
        catchWrapperException(libTorrent.resume_torrent(id));
        LOG.debugf("after resume_torrent: {0}", id);
     
    }

    public void get_torrent_status(String id, LibTorrentStatus status) {
        LOG.debugf("before get_torrent_status: {0}", id);
        catchWrapperException(libTorrent.get_torrent_status(id, status));
        LOG.debugf("after get_torrent_status: {0}", id);
    }

    public void remove_torrent(String id) {
        LOG.debugf("before remove_torrent: {0}", id);
        catchWrapperException(libTorrent.remove_torrent(id));
        LOG.debugf("after remove_torrent: {0}", id);
    }
    
    public List<String> get_peers(String id) {
        
        Memory numPeersMemory = new Memory(8);
        
        LOG.debugf("before get_num_peers: {0}", id);
        libTorrent.get_num_peers(id, numPeersMemory);
        LOG.debugf("before get_num_peers: {0}", id);
        
        int numUnfilteredPeers = numPeersMemory.getInt(0);
        
        if (numUnfilteredPeers == 0) {
            return Collections.emptyList();
        }
        
        Memory memory = new Memory(numUnfilteredPeers*16);

        LOG.debugf("before get_peers: {0}", id);
        libTorrent.get_peers(id, memory);
        LOG.debugf("after get_peers: {0}", id);
        
        List<String> peers =  Arrays.asList(memory.getString(0).split(";"));
        
        return peers;
    }

    public void signal_fast_resume_data_request(String id) {
        LOG.debugf("before print signal_fast_resume_data_request: {0}", id);
        catchWrapperException(libTorrent.signal_fast_resume_data_request(id));
        LOG.debugf("after print signal_fast_resume_data_request: {0} - {1}", id);
    }

    public void move_torrent(String id, String absolutePath) {
        LOG.debugf("before move_torrent: {0} - {1}", id, absolutePath);
        // TODO libtorrent documentation says this method will only work if the
        // new path is on the same device, might need a fallback plan
        catchWrapperException(libTorrent.move_torrent(id, absolutePath));
        LOG.debugf("after move_torrent: {0} - {1}", id, absolutePath);
    }

    public void abort_torrents() {
        LOG.debug("before abort");
        catchWrapperException(libTorrent.abort_torrents());
        LOG.debug("after abort");
    }

    public void add_torrent_existing(String sha1, String trackerURI, String fastResumeData) {
        LOG.debugf("before add_torrent_old: {0} - {1}", sha1, trackerURI);
        catchWrapperException(libTorrent.add_torrent_existing(sha1, trackerURI, fastResumeData));
        LOG.debugf("after add_torrent_old: {0} - {1}", sha1, trackerURI);
    }

    public void free_torrent_status(LibTorrentStatus oldStatus) {
        LOG.debugf("before free_torrent_status: {0}", oldStatus);
        catchWrapperException(libTorrent.free_torrent_status(oldStatus.getPointer()));
        LOG.debugf("after free_torrent_status: {0}", oldStatus);
    }    
    
    private void catchWrapperException(WrapperStatus status) {
        if (status != null) {
            throw new LibTorrentException(status);
        }
    }
}
