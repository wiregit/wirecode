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
		
		CandidateHandler _handler;
		
		public GoodLeafCandidate(String host, int port, short uptime, int filesShared) {
			super(host,port);
			_uptime = uptime;
			_filesShared = filesShared;
			_handler = new CandidateHandler(this) {
				public int getFileShared() {
					return _filesShared;
				}
			};
			
		}
		
		public CandidateHandler getCandidateHandler() {
			return _handler;
		}
		
		public boolean isGoodCandidate() {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#isOpen()
		 */
		public boolean isOpen() {
			return true;
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