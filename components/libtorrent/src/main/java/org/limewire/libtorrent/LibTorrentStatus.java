package org.limewire.libtorrent;

import java.math.BigInteger;

import org.limewire.util.StringUtils;

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

    public int paused;

    public int finished;

    public int valid;

    public String error;

    public LibTorrentStatus() {

    }

    public LibTorrentStatus(LibTorrentStatus copy) {
        this.total_done = new String(copy.total_done);
        this.total_download = new String(copy.total_download);
        this.total_upload = new String(copy.total_upload);
        this.download_rate = copy.download_rate;
        this.upload_rate = copy.upload_rate;
        this.num_peers = copy.num_peers;
        this.num_uploads = copy.num_uploads;
        this.num_seeds = copy.num_seeds;
        this.num_connections = copy.num_connections;
        this.state = copy.state;
        this.progress = copy.progress;
        this.paused = copy.paused;
        this.finished = copy.finished;
        this.valid = copy.valid;
        this.error = new String(copy.error);
    }

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

    public boolean isPaused() {
        return paused != 0;
    }

    public boolean isFinished() {
        return finished != 0;
    }

    public boolean isError() {
        return error != null && !StringUtils.isEmpty(error);
    }
}
