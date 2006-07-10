package com.limegroup.bittorrent;


public interface BTPiece {

	/**
	 * @return the interval this piece represents
	 */
	public BTInterval getInterval();

	/**
	 * @return requested data
	 */
	public byte[] getData();

}