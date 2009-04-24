package org.limewire.libtorrent;

import java.math.BigInteger;

import com.sun.jna.Structure;

public class LibTorrentStatus extends Structure {
    public String total_done;

    public float download_rate;

    public int num_peers;

    public int state;

    public float progress;

    public boolean paused;

    public boolean finished;

    public long getTotalDone() {
        if (total_done == null) {
            return -1;
        } else {
            BigInteger totalDone = new BigInteger(total_done);
            return totalDone.longValue();
        }
    }
}
