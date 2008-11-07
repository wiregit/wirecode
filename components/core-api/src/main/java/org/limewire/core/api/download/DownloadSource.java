package org.limewire.core.api.download;

public interface DownloadSource {
	public String getName();
	
	/**
	 * The IP address of the source
	 * @return
	 */
	public String getAddress();
}
