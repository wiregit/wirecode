package com.limegroup.bittorrent;

import java.io.File;

/**
 * Holds the length and the path of a file.
 */
public class TorrentFile extends File {
	private static final long serialVersionUID = 4051327846800962608L;

	private final long length;

	/** 
	 * The indices of the first and last blocks 
	 * of the torrent this file occupies
	 */
	private int begin, end;
	
	private long startByte, endByte;
	
	private String torrentPath = null;

	TorrentFile(long length, String path) {
		super(path);
		this.length = length;
		begin = -1; //these need to be initialized.
		end = -1; 
	}
	
	@Override
    public long length() {
		return length;
	}
	
	public void setBeginPiece(int begin) {
		this.begin = begin;
	}
	
	public int getBeginPiece() {
		return begin;
	}
	
	public void setEndPiece(int end) {
		this.end = end;
	}
	
	public int getEndPiece() {
		return end;
	}

    public long getStartByte() {
        return startByte;
    }

    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    public void setEndByte(long endByte) {
        this.endByte = endByte;
    }

    public String getTorrentPath() {
        return torrentPath;
    }

    public void setTorrentPath(String torrentPath) {
        this.torrentPath = torrentPath;
    }
    
    

	
	
}
