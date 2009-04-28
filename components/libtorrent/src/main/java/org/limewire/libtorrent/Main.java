package org.limewire.libtorrent;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        TorrentManager libTorrentManager = new TorrentManagerImpl(new File(
                "/home/pvertenten/Desktop/"));
        libTorrentManager.initialize();

        libTorrentManager.start();

        
        File torrentFile = new File(
                "/home/pvertenten/Desktop/wndw - wireless networking in the developing world.torrent");
        // File torrentFile = new File(
        // "C:\\Documents and Settings\\pvertenten\\Desktop\\wndw - wireless networking in the developing world.torrent"
        // );
        // File torrentFile = new File(
        // "C:\\Users\\pvertenten\\Desktop\\ubuntu-8.10-alternate-i386.iso.torrent"
        // );

        
        String sha1 = "02ef6fe20fe871d32773d18d6aee842332511d24";
        String announce = "http://tracker001.legaltorrents.com:7070/announce";
        String name = "test123";
        
        
        
        Torrent torrent = new Torrent(libTorrentManager);
//        torrent.init(torrentFile, libTorrentManager.getTorrentDownloadFolder());
        torrent.init(name, sha1, announce, libTorrentManager.getTorrentDownloadFolder());
        torrent.start();

        String id = torrent.getSha1() != null ? torrent.getSha1() :  sha1;

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
