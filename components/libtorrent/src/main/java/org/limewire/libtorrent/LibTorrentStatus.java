package org.limewire.libtorrent;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

public class LibTorrentStatus extends Structure {
	public String total_done;
	public float download_rate;
	public int num_peers;
	public int state;
	public float progress;
	public boolean paused;
	public boolean finished;
}
