
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;

import java.util.Properties;
import java.io.IOException;

/**
 * Handles the receiving and sending of FeaturesVM and BCVM messages.
 * Every <tt>Connection</tt> has such object.
 * 
 * Note: the getters of this file assume that the Properties object is taken
 * from a FeaturesVendorMessage.  The constructors of that message verify the format
 * and catch any NumberFormatExceptions, so be careful if you decide to replace
 * the Properties object arbitrarily.
 */
public class CandidateHandler {
	
	/**
	 * reference to the connection object that will be sending the messages
	 */
	private final Connection _connection;
	
	/**
	 * the features this connection advertised.
	 * LOCKING: this
	 */
	private Properties _features;
	
	
	public CandidateHandler(Connection c) {
		_connection = c;
	}
	
    
    /**
     * The possibly non-null VendorMessagePayload which describes what
     * candidates the guy on the other side of this connection advertises.
     * LOCKING: this
     */
    protected BestCandidatesVendorMessage _candidatesReceived = null;
    
    /**
     * The possibly non-null VendorMessagePayload that this connection last
     * sent or is pending to be sent
     * LOCKING: this
     */
    protected BestCandidatesVendorMessage _candidatesSent = null;
    
    /**
     * the last time we received an advertisement on this connection
     * LOCKING: this
     */
    private long _lastReceivedAdvertisementTime;
    
    /**
     * the last time we sent an advertisement on this connection
     * LOCKING: this
     */
    private long _lastSentAdvertisementTime;
    
    /**
     * we should not send or receive advertisements more often than this
     * interval.  Not final so that tests can change it.
     */
    private static long ADVERTISEMENT_INTERVAL = 2 * Constants.MINUTE;  // 2 minutes?
    
    /**
     * this flag is set only if we needed to update the connection with
     * new candidates before the advertisement  interval had expired.
     */
    private boolean _needsAdvertisement;

    
    /**
     * handles either a FeaturesVM or a BestCandidatesVM
     * @param vm the vendor message
     */
    public synchronized void handleVendorMessage(VendorMessage vm) {
    	
    	if (vm instanceof FeaturesVendorMessage) {
    		
    		FeaturesVendorMessage fvm = (FeaturesVendorMessage)vm;
    		
    		_features.putAll(fvm.getProperties());
    		
    		BestCandidates.initialize();
    	} 
    	
    	else if (vm instanceof BestCandidatesVendorMessage) {
    		//do nothing if we are a leaf or the connection is a leaf.
        	if (!RouterService.isSupernode() || 
        			! (_connection.isGoodUltrapeer() && _connection.getUptime() > 10))
        		return;
        	
        	

        	 // it is not possible for two threads to try and set this, but it is
        	 // possible for the message parsing thread to set this while the advertising
        	// thread is trying to read it.
        	 
        	
        	//make sure they aren't advertising too soon.
        	if (System.currentTimeMillis() - _lastReceivedAdvertisementTime <
        		ADVERTISEMENT_INTERVAL)
        		return;
        	
        		//update the values
        	_lastReceivedAdvertisementTime = System.currentTimeMillis();
        	_candidatesReceived = (BestCandidatesVendorMessage)vm;
        	
        	
        	//then add a ref of the advertiser to each candidate received
        	Candidate [] candidates = _candidatesReceived.getBestCandidates();
        	
        	if (candidates[0]!=null)
        		candidates[0].setAdvertiser(_connection);
        	if (candidates[1]!=null)
        		candidates[1].setAdvertiser(_connection);
        	
        	//and update our internal table
        	BestCandidates.update(candidates);

    	}
    	
    }
    
    
    public synchronized void handleBestCandidatesMessage(BestCandidatesVendorMessage m) 
		throws IOException {
	
	
    	//first see if we are not sending to this connection too soon.
    	if (System.currentTimeMillis() - _lastSentAdvertisementTime 
			< ADVERTISEMENT_INTERVAL) {
		
    		//we are trying to send too soon.  However if the message is new it should be scheduled
    		//to be sent once the interval expires.
    		if (!m.isSame(_candidatesSent)) {
    			_needsAdvertisement=true;	
				_candidatesSent=m;
			}
		
			return;
    	}
	
    	//if there is an update pending or the message is new, send it.
    	if (_needsAdvertisement || !m.isSame(_candidatesSent)) {
		 	_candidatesSent=m;
			_needsAdvertisement=false;
			_lastSentAdvertisementTime = System.currentTimeMillis();
			_connection.send(_candidatesSent);
    	}
    }
    
    public synchronized boolean isGoodCandidate() {
    	if (!
				( isTCPCapable() && //incoming TCP
				isUDPCapable() && //unsolicited UDP
				_connection.isGoodLeaf() &&
				getBandwidth() > 16 &&
				getJVM().indexOf("1.4.0") == -1))
					return false;
			
			//filter out non-32 bit windowses
			if (getOS().startsWith("windows") && 
					getOS().indexOf("xp") ==-1 &&
					getOS().indexOf("2000") ==-1)
					return false;
			
			//and mac os 9
			if (getOS().startsWith("mac os") &&
					!getOS().endsWith("x"))
				return false;
			
			return true;
    }
    
    public synchronized Candidate [] getCandidates() {
    	if (_candidatesReceived == null)
			return null;
		
		/*Candidate [] ret = new Candidate[2];
		
		
		ret[0] = _candidatesReceived.getBestCandidates()[0];
		ret[1] = _candidatesReceived.getBestCandidates()[1];*/
    	return _candidatesReceived.getBestCandidates();
		
    }
    /**
	 * @return the reported shared files.
	 */
	public int getFileShared() {
		return _features == null ? -1 :
			Integer.parseInt(_features.getProperty(FeaturesVendorMessage.FILES_SHARED,"-1"));
	}
	/**
	 * @return the reported JVM version
	 */
	public String getJVM() {
		return _features!= null ? _features.getProperty(FeaturesVendorMessage.JVM) : null;
	}
	/**
	 * @return the reported OS version
	 */
	public String getOS() {
		return _features!=null ? _features.getProperty(FeaturesVendorMessage.OS) : null;
	}
	/**
	 * @return the reported incoming TCP capability
	 */
	public boolean isTCPCapable() {
		return _features== null ? false: 
			Boolean.valueOf(
					_features.getProperty(FeaturesVendorMessage.INCOMING_TCP,"false")).booleanValue();
	}
	/**
	 * @return the reported UDP capability
	 */
	public boolean isUDPCapable() {
		return _features== null ? false: 
			Boolean.valueOf(
					_features.getProperty(FeaturesVendorMessage.INCOMING_UDP,"false")).booleanValue();
	}
	
	/**
	 * @return the reported upstream bandwidth.
	 */
	public int getBandwidth() {
		return _features == null ? -1 :
			Integer.parseInt(_features.getProperty(FeaturesVendorMessage.BANDWIDTH,"-1"));
	}
    
}
