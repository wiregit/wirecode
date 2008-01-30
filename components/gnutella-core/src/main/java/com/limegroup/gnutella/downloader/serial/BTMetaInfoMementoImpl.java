package com.limegroup.gnutella.downloader.serial;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
import java.net.URI;

import com.limegroup.bittorrent.TorrentFileSystem;

public class BTMetaInfoMementoImpl implements BTMetaInfoMemento{
    
    private Map<String, Object> serialObjects = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    public List<byte[]> getHashes() {
        return (List<byte[]>)serialObjects.get("hashes");    
    }

    public void setHashes(List<byte[]> hashes) {
        serialObjects.put("hashes", hashes);
    }

    public int getPieceLength() {
        Integer i = (Integer)serialObjects.get("pieceLength"); 
        if(i != null) {
            return i;
        } else {
            return 0;
        }
    }

    public void setPieceLength(int pieceLength) {
        serialObjects.put("pieceLength", pieceLength);
    }

    public TorrentFileSystem getFileSystem() {
        return (TorrentFileSystem)serialObjects.get("fileSystem");    
    }

    public void setFileSystem(TorrentFileSystem fileSystem) {
        serialObjects.put("fileSystem", fileSystem);
    }

    public byte[] getInfoHash() {
        return (byte [])serialObjects.get("infoHash");    
    }

    public void setInfoHash(byte[] infoHash) {
        serialObjects.put("infoHash", infoHash);
    }

    public float getRatio() {
        Float f = (Float)serialObjects.get("ratio"); 
        return f != null ? f : 0f;
    }

    public void setRatio(float ratio) {
        serialObjects.put("ratio", ratio);
    }

    public Serializable getFolderData() {
        return (Serializable)serialObjects.get("folderData");    
    }

    public void setFolderData(Serializable folderData) {
        serialObjects.put("folderData", folderData);
    }

    public URI[] getTrackers() {
        return (URI [])serialObjects.get("trackers");    
    }

    public void setTrackers(URI[] trackers) {
        serialObjects.put("trackers", trackers);
    }

    public boolean isPrivate() {
        Boolean b =  (Boolean)serialObjects.get("private"); 
        return b != null ? b : false;
    }

    public void setPrivate(boolean aPrivate) {
        serialObjects.put("private", aPrivate);
    }
}
