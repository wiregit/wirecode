/*
 * Created on Apr 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.ManagedConnectionStub;
import com.limegroup.gnutella.messages.vendor.BestCandidatesVendorMessage;
import com.limegroup.gnutella.messages.vendor.FeaturesVendorMessage;
import com.limegroup.gnutella.util.*;

import java.util.Properties;

/**
	 * a stub which claims it is a good candidate for election
	 */
	class GoodLeafCandidate extends ManagedConnectionStub {
		
		
		Properties _goodProperties;
		final short _uptime;
		final int _filesShared;
		
		
		
		public GoodLeafCandidate(String host, int port, short uptime, int filesShared) {
			super(host,port);
			
			_goodProperties = new Properties();
			
			_goodProperties.setProperty(FeaturesVendorMessage.OS,"nachos ;-)");
			_goodProperties.setProperty(FeaturesVendorMessage.JVM,"2.0");
			_goodProperties.setProperty(FeaturesVendorMessage.INCOMING_TCP,"true");
			_goodProperties.setProperty(FeaturesVendorMessage.INCOMING_UDP,"true");
			_goodProperties.setProperty(FeaturesVendorMessage.BANDWIDTH,"20");
			_goodProperties.setProperty(FeaturesVendorMessage.FILES_SHARED,""+filesShared);
			
			_uptime = uptime;
			_filesShared = filesShared;
			_candidateHandler = new CandidateHandler(this);
			
			
			try {
				PrivilegedAccessor.setValue(_candidateHandler,"_features",_goodProperties);
				
			}catch(Exception e) {e.printStackTrace();}
			
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