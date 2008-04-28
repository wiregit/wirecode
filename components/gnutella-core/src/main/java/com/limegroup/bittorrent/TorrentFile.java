package com.limegroup.bittorrent;

import java.io.File;

/*
 * simple class holding the length and the path of a file
 */
public class TorrentFile extends File {
	private static final long serialVersionUID = 4051327846800962608L;

	private final long length;

	/** 
	 * The indices of the first and last blocks 
	 * of the torrent this file occupies
	 */
	private int begin, end;

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
	
	public void setBegin(int begin) {
		this.begin = begin;
	}
	
	public int getBegin() {
		return begin;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}
	
	public int getEnd() {
		return end;
	}
	
}
