package com.limegroup.bittorrent;

import java.io.Serializable;

/*
 * simple class holding the length and the path of a file
 */
class TorrentFile implements Serializable {
	private static final long serialVersionUID = 4051327846800962608L;

	final long LENGTH;

	final String PATH;
	
	/** 
	 * The indices of the first and last blocks 
	 * of the torrent this file occupies
	 */
	int begin, end;

	TorrentFile(long length, String path) {
		LENGTH = length;
		PATH = path;
		begin = -1; //these need to be initialized.
		end = -1; 
	}
	
	public String toString() {
		return PATH + " " + LENGTH;
	}
}
