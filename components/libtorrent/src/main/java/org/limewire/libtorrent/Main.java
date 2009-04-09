package org.limewire.libtorrent;

import java.io.File;

public class Main {
	public static void main(String[] args) throws InterruptedException {
		LibTorrent libTorrent = new LibTorrentWrapper();
		libTorrent.init(new File("/home/pvertenten/Desktop").getAbsolutePath());
		String id = "id";
		libTorrent
				.add_torrent(
						id,
						new File(
								"/home/pvertenten/Desktop/wndw - wireless networking in the developing world.torrent")
								.getAbsolutePath());

		int count = 1;
		boolean paused = false;
		while (true) {
			System.out.println("paused: " + libTorrent.is_torrent_paused(id));
			System.out.println("finished: "
					+ libTorrent.is_torrent_finished(id));
			System.out.println("seed: " + libTorrent.is_torrent_seed(id));
			System.out.println("valid: " + libTorrent.is_torrent_valid(id));
//			if (count % 200 == 0) {
//				if (paused) {
//					libTorrent.resume_torrent(id);
//					paused = false;
//				} else {
//					libTorrent.pause_torrent(id);
//					paused = true;
//				}
//			}

			TorrentListener torrentListener = new TorrentListener();
			libTorrent.get_alerts(torrentListener, torrentListener);
			TorrentStatus torrentStatus = libTorrent.get_torrent_status(id);
			torrentStatus.read();
			System.out.println("total_done_java: " + torrentStatus.total_done);
			System.out.println("download_rate_java: "
					+ torrentStatus.download_rate);
			System.out.println("num_peers_java: " + torrentStatus.num_peers);
			System.out.println("state_java: " + torrentStatus.state);
			System.out.println("progress_java: " + torrentStatus.progress);
			count++;
			Thread.sleep(100);
		}
	}
}
