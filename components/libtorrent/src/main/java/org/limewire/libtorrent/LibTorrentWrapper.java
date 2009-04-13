package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.libtorrent.callback.TorrentFinishedCallback;

import com.sun.jna.Memory;
import com.sun.jna.Native;

public class LibTorrentWrapper implements LibTorrent {

	private final LibTorrent libTorrent;

	public LibTorrentWrapper() {
		this.libTorrent = (LibTorrent) Native.loadLibrary("torrentwrapper",
				LibTorrent.class);
	}

	@Override
	public String add_torrent(String id, String path) {
		return libTorrent.add_torrent(id, path);
	}

	@Override
	public void init(String path) {
		libTorrent.init(path);
	}

	@Override
	public void get_alerts(AlertCallback alertCallback,
			TorrentFinishedCallback torrentFinishedCallback) {
		libTorrent.get_alerts(alertCallback, torrentFinishedCallback);
	}

	@Override
	public int pause_torrent(String id) {
		return libTorrent.pause_torrent(id);
	}

	@Override
	public int resume_torrent(String id) {
		return libTorrent.resume_torrent(id);
	}

	@Override
	public boolean is_torrent_paused(String id) {
		return libTorrent.is_torrent_paused(id);
	}

	@Override
	public LibTorrentStatus get_torrent_status(String id) {
		int size = new LibTorrentStatus().size();
		Memory memory = new Memory(size);
		return libTorrent.get_torrent_status(id, memory);
	}

	@Override
	public LibTorrentStatus get_torrent_status(String id, Memory memory) {
		return libTorrent.get_torrent_status(id);
	}

	@Override
	public boolean is_torrent_finished(String id) {
		return libTorrent.is_torrent_finished(id);
	}

	@Override
	public boolean is_torrent_valid(String id) {
		return libTorrent.is_torrent_valid(id);
	}

	@Override
	public boolean is_torrent_seed(String id) {
		return libTorrent.is_torrent_seed(id);
	}

}
