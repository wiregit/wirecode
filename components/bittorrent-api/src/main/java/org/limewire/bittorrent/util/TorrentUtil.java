package org.limewire.bittorrent.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;

public class TorrentUtil {
    /**
     * Returns a list of files in the given torrent.
     */
    public static List<File> buildTorrentFiles(Torrent torrent, File root) {
        List<File> files = new ArrayList<File>();

        TorrentInfo torrentMetaData = torrent.getTorrentInfo();
        if (torrentMetaData != null) {
            for (TorrentFileEntry torrentFileEntry : torrentMetaData.getTorrentFileEntries()) {
                files.add(new File(root, torrentFileEntry.getPath()));
            }
        }
        return files;
    }
}
