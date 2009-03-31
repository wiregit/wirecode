package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.Structure;

public class CFArrayCallBacks extends Structure {
	public int version;
	public CFArrayRetainCallBack retain;
	public CFArrayReleaseCallBack release;
	public CFArrayCopyDescriptionCallBack copyDescription;
	public CFArrayEqualCallBack equal;
	
}
