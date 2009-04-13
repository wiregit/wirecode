package org.limewire.libtorrent;

import java.io.File;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        LibTorrentManager libTorrentManager = new LibTorrentManager();
        libTorrentManager.init();
        String id = libTorrentManager
                .addTorrent(
                        new File(
                                "/home/pvertenten/Desktop/wndw - wireless networking in the developing world.torrent"));

        System.out.println(" id: " + id);
        
//        System.out.println("pausing");
//        libTorrentManager.pauseTorrent(id);
//        System.out.println("paused");
        while(true) {
//            System.out.println("getting status");
            LibTorrentStatus torrentStatus = libTorrentManager.getStatus(id);
            torrentStatus.read();
//            System.out.println("status got");
            System.out.println("total_done_java: " + torrentStatus.total_done);
            System.out.println("download_rate_java: " + torrentStatus.download_rate);
            System.out.println("num_peers_java: " + torrentStatus.num_peers);
            System.out.println("state_java: " + torrentStatus.state);
            System.out.println("progress_java: " + torrentStatus.progress);
            Thread.sleep(3000);
        }
    }
}
