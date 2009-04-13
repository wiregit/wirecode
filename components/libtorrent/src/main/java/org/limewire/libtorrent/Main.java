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


        Thread.sleep(100000000);
    }
}
