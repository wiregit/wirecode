package org.limewire.libtorrent;

import java.io.File;

import org.limewire.listener.EventListener;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        TorrentManager libTorrentManager = new TorrentManager();
        
         LibTorrentInfo info = libTorrentManager
         .addTorrent(new File(
         "/home/pvertenten/Desktop/wndw - wireless networking in the developing world.torrent"));
                
//        LibTorrentInfo info = libTorrentManager
//                .addTorrent(new File(
//                        "C:\\Documents and Settings\\pvertenten\\Desktop\\wndw - wireless networking in the developing world.torrent"));
//        LibTorrentInfo info = libTorrentManager
//        .addTorrent(new File(
//                "C:\\Users\\pvertenten\\Desktop\\ubuntu-8.10-alternate-i386.iso.torrent"));
        
        System.out.println(info);
        String id = info.sha1;

        System.out.println("sha1_java: " + id);

        libTorrentManager.addListener(id, new EventListener<LibTorrentEvent>() {
            @Override
            public void handleEvent(LibTorrentEvent event) {

                LibTorrentStatus torrentStatus = event.getTorrentStatus();

                System.out.println("total_done_java: " + torrentStatus.total_done);
                System.out.println("download_rate_java: " + torrentStatus.download_rate);
                System.out.println("num_peers_java: " + torrentStatus.num_peers);
                System.out.println("state_java: " + torrentStatus.state + " - "
                        + LibTorrentState.forId(torrentStatus.state));
                System.out.println("progress_java: " + torrentStatus.progress);
                System.out.println("paused_java: " + torrentStatus.paused);
            }
        });

        Thread.sleep(100000000);
    }
}
