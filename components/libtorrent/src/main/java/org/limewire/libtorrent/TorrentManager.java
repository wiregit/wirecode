package org.limewire.libtorrent;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;

import org.limewire.lifecycle.Service;

public interface TorrentManager extends Service {

    /**
     * Removes torrent from control of the torrent manager. Also delegates a
     * remove torrent call to the libtorrent wrapper lbirary.
     */
    public void removeTorrent(Torrent torrent);

    /**
     * Delegates a pause torrent call to the libtorrent wrapper library.
     */
    public void pauseTorrent(Torrent torrent);

    /**
     * Delegates a resume torrent call to the libtorrent wrapper library.
     */
    public void resumeTorrent(Torrent torrent);

    /**
     * Delegates to clear any error state and restarts the torrent.
     */
    public void recoverTorrent(Torrent torrent);
    
    /**
     * Returns a list of ip addresses for the peers connected to the specified
     * torrent.
     */
    public List<String> getPeers(Torrent torrent);

    /**
     * Returns the folder setup to download torrents to.
     */
    public File getTorrentDownloadFolder();

    /**
     * Moves the underlying storage of the torrent to the specified directory.
     * Currently this is used to move an a completed torrent from the incomplete
     * directory to the complete directory, without interrupting any seeding
     * which maybe be happening.
     */
    public void moveTorrent(Torrent torrent, File directory);

    /**
     * Returns the number of active torrents.
     */
    public int getNumActiveTorrents();

    /**
     * Registers the specified torrent with the TorrentManager. Delegates an add
     * torrent call to the underlying libtorrent wrapper library.
     */
    void registerTorrent(Torrent torrent);

    /**
     * Returns true if the torrent manager currently has a torrent registered
     * that is downloading from the specified torrent file.
     */
    public boolean isDownloading(File torrentFile);

    /**
     * Returns true if the torrent manager is currently downloading a torrent
     * with the specified sha1.
     */
    public boolean isDownloading(String sha1);

    /**
     * Returns the Executor used by the torrent code. 
     */
    public Executor getTorrentExecutor();
}