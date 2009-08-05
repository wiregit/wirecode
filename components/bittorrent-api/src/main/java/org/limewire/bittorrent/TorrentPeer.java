package org.limewire.bittorrent;

public interface TorrentPeer {
    public String getPeerId();

    public String getIPAddress();

    public short getSource();

    public float getUploadSpeed();

    public float getDownloadSpeed();

    public float getPayloadUploadSpeed();

    public float getPayloadDownloadSpeed();

    public float getProgress();

    public String getCountry();
}
