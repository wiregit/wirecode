package org.limewire.bittorrent.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;

public class TorrentUtil {
    /**
     * Returns a list of files in the given torrent.
     */
    public static List<File> buildTorrentFiles(Torrent torrent, File root) {
        List<File> files = new ArrayList<File>();
        for (TorrentFileEntry torrentFileEntry : torrent.getTorrentFileEntries()) {
            files.add(new File(root, torrentFileEntry.getPath()));
        }
        return files;
    }
    
    /**
     * @return null if there was an error parsing
     */
    public static BTData parseTorrentFile(File torrentFile) {
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrentFile);
            fileChannel = fis.getChannel();
            Object obj = Token.parse(fileChannel);
            if (obj instanceof Map) {
                BTDataImpl torrentData = new BTDataImpl((Map)obj);
                torrentData.clearPieces();
                return torrentData;
            }
        } catch (IOException ie) {
            // TODO log
        } finally {
            IOUtils.close(fis);
            IOUtils.close(fileChannel);
        }
        return null;
    }
}
