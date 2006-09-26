package com.limegroup.bittorrent;

interface BTLinkListener {
	public void linkClosed(BTLink closed);
	public void countDownloaded(int downloaded);
	public void linkInterested(BTLink interested);
	public void linkNotInterested(BTLink notInterested);
}
