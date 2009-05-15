package org.limewire.libtorrent;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        TorrentManager libTorrentManager = new TorrentManagerImpl(new File(
                "/home/michael/Download/"),  null);
        libTorrentManager.initialize();

        libTorrentManager.start();

        
//        File torrentFile = new File(
//                "/home/pvertenten/Desktop/wndw - wireless networking in the developing world.torrent");
        // File torrentFile = new File(
        // "C:\\Documents and Settings\\pvertenten\\Desktop\\wndw - wireless networking in the developing world.torrent"
        // );
        // File torrentFile = new File(
        // "C:\\Users\\pvertenten\\Desktop\\ubuntu-8.10-alternate-i386.iso.torrent"
        // );

        
        String sha1 = "02ef6fe20fe871d32773d18d6aee842332511d24";
        String announce = "http://tracker001.legaltorrents.com:7070/announce";
        String name = "test123";
        
        
        
        Torrent torrent = new Torrent(libTorrentManager, new ScheduledThreadPoolExecutor(1));
//        torrent.init(torrentFile, libTorrentManager.getTorrentDownloadFolder());
        List<String> paths = Collections.emptyList();
        
        torrent.init(name, sha1, 5371896, announce, paths, libTorrentManager.getTorrentDownloadFolder(), null, null);
        torrent.start();

        String id = torrent.getSha1() != null ? torrent.getSha1() :  sha1;

        System.out.println("sha1_java: " + id);

        Thread.sleep(100000000);
    }
}
