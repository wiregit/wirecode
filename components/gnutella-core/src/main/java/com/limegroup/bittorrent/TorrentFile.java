
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.Serializable;

/**
 * A TorrentFile keeps the following information together:
 * The path in LimeWire's temporary folder where we'll save the file, like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext".
 * The file size.
 * The number of the first piece that has data that is part of this file.
 * The number of the last piece that has data that is part of this file.
 * 
 * For a multifile .torrent, you can make a TorrentFile object for each file in the torrent. (do)
 */
class TorrentFile implements Serializable {

	/** A long unique number that will identify this version of this object when it's serialized to disk. */
	private static final long serialVersionUID = 4051327846800962608L;

	/** The file size in bytes. */
	final long LENGTH;

	/**
	 * The path to where we'll save the file in LimeWire's temporary folder.
	 * Like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext".
	 */
	final String PATH;

	/**
	 * The piece number that this file starts in.
	 * -1 before we know.
	 */
	int begin;

	/**
	 * The index of the last block in the torrent that this file occupies.
	 * -1 before we know.
	 * 
	 * If a file ends on a piece boundary, end will be the piece number of the next piece.
	 * 
	 * pieces: 00000000111111112222222233333333
	 * files:  aaaabbcccccdddddeeeeeee
	 * 
	 * File d ends on the boundary between pieces 1 and 2.
	 * end for d will be 2, not 1, even though no part of d is in piece 2.
	 * 
	 * In a single-file .torrent, end is the number of pieces:
	 * 
	 * pieces: 00000000111111112222222233333333
	 * files:  aaaaaaaaaaaaaaaaaaaaaaaaaaaa
	 * 
	 * end will be 4, the number of pieces.
	 * 
	 * TODO: Confirm this is the way it should be.
	 */
	int end;

	/**
	 * Make a new TorrentFile object, which keeps the temporary path to a file and its size together.
	 * 
	 * @param length The file size in bytes.
	 * @param path   The complete path to where we'll save this file in LimeWire's temporary folder.
	 *               Like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext".
	 */
	TorrentFile(long length, String path) {

		// Save the given length and path
		LENGTH = length;
		PATH = path;

		// We don't know which pieces this file has data in yet
		begin = -1; // Set begin and end to -1 until we know
		end = -1;
	}

	/**
	 * Express this TorrentFile object as text.
	 * 
	 * @return A String with the temporary save path and file size like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext 12345"
	 */
	public String toString() {

		// Put the path and length together
		return PATH + " " + LENGTH;
	}
}
