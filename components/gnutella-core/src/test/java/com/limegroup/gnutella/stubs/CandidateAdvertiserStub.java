
package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.messages.vendor.BestCandidatesVendorMessage;
import com.limegroup.gnutella.upelection.CandidateAdvertiser;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Stub which won't start the Candidate advertiser in a different thread.
 */
public class CandidateAdvertiserStub extends CandidateAdvertiser {
	
	public CandidateAdvertiserStub() { //don't schedule advertisement
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.ManagedThread#managedRun()
	 */
	public void managedRun() {
		// stub
		return;
	}
	
	/**
	 * utility getter for the private <tt>BestCandidatesVendorMessage</tt> field
	 * @return the message which was set to be advertised.
	 */
	public BestCandidatesVendorMessage getMsg() {
		try {
			return (BestCandidatesVendorMessage) PrivilegedAccessor.getValue(this, "_bcvm");
		}catch(Exception e) {
			return null;
		}
	}
	
}
