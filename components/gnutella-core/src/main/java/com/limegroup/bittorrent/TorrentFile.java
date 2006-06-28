package com.limegroup.bittorrent;

import java.io.File;

/*
 * simple class holding the length and the path of a file
 */
class TorrentFile extends File {
	private static final long serialVersionUID = 4051327846800962608L;

	private final long length;

	/** 
	 * The indices of the first and last blocks 
	 * of the torrent this file occupies
	 */
	int begin, end;

	TorrentFile(long length, String path) {
		super(path);
		this.length = length;
		begin = -1; //these need to be initialized.
		end = -1; 
	}
	
	public long length() {
		return length;
	}
	
}
