package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.libtorrent.callback.TorrentFinishedCallback;
import com.sun.jna.Library;
import com.sun.jna.Memory;

public interface LibTorrent extends Library {

	public void init(String path);
	
	public int add_torrent(String id, String path);
	
	public int pause_torrent(String id);
	
	public int resume_torrent(String id);
	
	public boolean is_torrent_paused(String id);
	public boolean is_torrent_seed(String id);
	public boolean is_torrent_finished(String id);
	public boolean is_torrent_valid(String id);
	
	public void get_alerts(AlertCallback alertCallback, TorrentFinishedCallback torrentFinishedCallback);

	public TorrentStatus get_torrent_status(String id);
	
	public TorrentStatus get_torrent_status(String id, Memory memory);

//	public int get_torrent_status(String id);
}
