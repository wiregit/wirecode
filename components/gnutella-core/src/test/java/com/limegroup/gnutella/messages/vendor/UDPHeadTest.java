
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.*;

import junit.framework.Test;

/**
 * this class tests the handling of udp head requests and responses.
 */
public class UDPHeadTest extends BaseTestCase {

	
	static FileManagerStub _fm;
	static UploadManagerStub _um;
	
	static URN _have,_notHave; 
	
	
	public UDPHeadTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(UDPHeadTest.class);
	}
	
	public static void globalSetUp() throws Exception{
		_fm = new FileManagerStub();
		_um = new UploadManagerStub();
		
		_have = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
		_notHave = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFC");
		
		PrivilegedAccessor.setValue(UDPHeadPong.class, "_fileManager",_fm);
		PrivilegedAccessor.setValue(UDPHeadPong.class, "_uploadManager",_um);
		
	}
	
	public void testSetUp() {}
}
