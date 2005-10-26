package com.limegroup.gnutella.archive;

import com.limegroup.gnutella.FileDesc;

class File {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/File.java,v 1.1.2.1 2005-10-26 20:02:48 tolsen Exp $";

	private FileDesc _fd;
	
	File( FileDesc fd ) {
		_fd = fd;
	}
}
