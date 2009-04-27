package org.limewire.libtorrent;

import java.math.BigInteger;

import com.sun.jna.Structure;

public class LibTorrentStatus extends Structure {

    public String total_done;

    public String total_download;
    
    public String total_upload;

    public float download_rate;

    public float upload_rate;

    public int num_peers;

    public int num_uploads;

    public int num_seeds;

    public int num_connections;

    public int state;

    public float progress;

    public boolean paused;

    public boolean finished;

    public boolean valid;

    public long getTotalDone() {
        if (total_done == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_done);
            return total.longValue();
        }
    }
    
    public long getTotalDownload() {
        if (total_download == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_download);
            return total.longValue();
        }
    }

    public long getTotalUpload() {
        if (total_upload == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_upload);
            return total.longValue();
        }
    }
}
