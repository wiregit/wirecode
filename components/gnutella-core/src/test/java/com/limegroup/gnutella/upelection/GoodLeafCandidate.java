/*
 * Created on Apr 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.ManagedConnectionStub;
import com.limegroup.gnutella.messages.vendor.BestCandidatesVendorMessage;


/**
	 * a stub which claims it is a good candidate for election
	 */
	class GoodLeafCandidate extends ManagedConnectionStub {
		
		final short _uptime;
		final int _filesShared;
		
		public GoodLeafCandidate(String host, int port, short uptime, int filesShared) {
			super(host,port);
			_uptime = uptime;
			_filesShared = filesShared;
		}
		
		
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#getBandwidth()
		 */
		public int getBandwidth() {
			return 20;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#getJVM()
		 */
		public String getJVM() {
			return "1.5.0";
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#getOS()
		 */
		public String getOS() {
			return "nachos";
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#isGoodLeaf()
		 */
		public boolean isGoodLeaf() {
			return true;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#remoteHostSupportsBestCandidates()
		 */
		public int remoteHostSupportsBestCandidates() {
			return BestCandidatesVendorMessage.VERSION;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#getFileShared()
		 */
		public int getFileShared() {
			return _filesShared;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#isTCPCapable()
		 */
		public boolean isTCPCapable() {
			return true;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#isUDPCapable()
		 */
		public boolean isUDPCapable() {
			return true;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#isSupernodeClientConnection()
		 */
		public boolean isSupernodeClientConnection() {
			return true;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#getUptime()
		 */
		public short getUptime() {
			return _uptime;
		}
}