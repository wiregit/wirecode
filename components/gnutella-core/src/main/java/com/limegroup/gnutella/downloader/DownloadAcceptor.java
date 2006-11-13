package com.limegroup.gnutella.downloader;

import java.net.Socket;

public interface DownloadAcceptor {

	public void acceptDownload(String file, int index, byte[] clientGUID,
			Socket socket);

}