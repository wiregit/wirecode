
package com.limegroup.gnutella.mp3;

/**
 * class which handles specifically the annotation of OGG files.
 */
public class OGGDataEditor extends AudioMetaDataEditor {
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.mp3.MetaDataEditor#commitMetaData(java.lang.String)
	 */
	public int commitMetaData(String filename) {
		throw new Error("commitMetaData for oggs not implemented");
		
	}
}
