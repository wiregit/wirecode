package org.limewire.bittorrent.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.bittorrent.Torrent;

public class TorrentUtil {
    /**
     * Returns a list of files in the given torrent, relative to the given root.
     * If the torrent only contains a single file, the root is returned.
     */
    public static List<File> buildTorrentFiles(Torrent torrent, File root) {
        List<File> files = new ArrayList<File>();
        if(torrent.getPaths().size() > 0) {
            for(String path : torrent.getPaths()) {
                files.add(new File(root, path));
            }
        } else {
            files.add(root);
        }
        return files;
    }
}
