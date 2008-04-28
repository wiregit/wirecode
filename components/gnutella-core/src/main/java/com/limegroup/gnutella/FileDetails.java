package com.limegroup.gnutella;

import java.net.InetSocketAddress;
import java.util.Set;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Common interface implemented by {@link FileDesc} and {@link RemoteFileDesc}.
 */
public interface FileDetails {

	/**
	 * Returns the file name.
	 * @return
	 */
	String getFileName();
	/**
	 * Returns the sha1 urn or <code>null</code> if there is none.
	 * @return
	 */
	URN getSHA1Urn();
	/**
	 * Returns the size of the file.
	 * @return
	 */
	long getFileSize();
	/**
	 * Returns the set of urns.
	 * @return
	 */
	Set<URN> getUrns();
	/**
	 * Returns the xml document or <code>null</code> if there is none for this
	 * file.
	 * @return
	 */
	LimeXMLDocument getXMLDocument();
	/**
	 * Returns address of the host that holds the file.
	 * @return
	 */
	InetSocketAddress getInetSocketAddress();
	/**
	 * Returns whether or not the host that holds this file is firewalled.
	 * @return
	 */	
	boolean isFirewalled();
	
	/**
	 * Returns the client guid of the remote host, could be <code>null</code> if
	 * not available. 
	 */
	byte[] getClientGUID();
}
