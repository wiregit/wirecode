package com.limegroup.gnutella.messages.vendor;


import com.limegroup.gnutella.messages.*;
import java.util.*; //both Properties and Stringtokenizer are in 1.1.8
import com.limegroup.gnutella.upelection.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;

/**
 * This message is used to inform another node of the features of 
 * this specific node.  It is intended to be sent either when a 
 * change occurs or at fixed intervals.
 */
public class FeaturesVendorMessage extends VendorMessage {
	
	public static final int VERSION = 1;
	
	//some feature headers.
	public static final String OS ="OS";
	public static final String INCOMING_TCP="IN_TCP";	
	public static final String INCOMING_UDP="IN_UDP";
	public static final String FILES_SHARED="Files";
	public static final String JVM="JVM";  //why not
	public static final String BANDWIDTH="BW";
	
	
	private static final String CR="\n";
	private static final String COL=":";
	
	/**
	 * the properties object.
	 */
	Properties _properties;
	
	int _filesShared, _bandwith;
	boolean _inTcp,_inUdp;
	
	/**
	 * constructs an object with data from the network
	 */
	protected FeaturesVendorMessage(byte[] guid, byte ttl, byte hops, 
                   int version, byte[] payload) throws BadPacketException {
		super(guid,ttl,hops,F_LIME_VENDOR_ID, F_FEATURES,version, payload);
		
		if (getVersion() > VERSION)
			throw new BadPacketException("cannot parse messages version more than "+VERSION);
		
		_properties = new Properties();
		String serialized = new String(payload);
		
		StringTokenizer tokenizer = new StringTokenizer(serialized,CR);
		
		while (tokenizer.hasMoreTokens()) {
			
			String pair = tokenizer.nextToken();
			int sep = pair.indexOf(COL);
			if (sep==-1 || pair.lastIndexOf(COL)!=sep) 
				throw new BadPacketException("invalid format");
			
			String name = pair.substring(0,sep);
			String value = pair.substring(sep+1,pair.length());
			_properties.put(name,value);
		}
		
		parseFields();
	}
	
	private final void parseFields() throws BadPacketException{
		if (_properties==null)
			return;
		try {
			_filesShared=Integer.parseInt(_properties.getProperty(FILES_SHARED));
			_bandwith = Integer.parseInt(_properties.getProperty(BANDWIDTH));
		}catch(NumberFormatException bad) {
			throw new BadPacketException ("invalid integer values in packet");
		}
		
		_inTcp = Boolean.valueOf(_properties.getProperty(INCOMING_TCP)).booleanValue();
		_inUdp = Boolean.valueOf(_properties.getProperty(INCOMING_UDP)).booleanValue();
	}
	
	/**
	 * creates a Vendor Message containing some of our features.
	 */
	public FeaturesVendorMessage() {
		super(F_LIME_VENDOR_ID, F_FEATURES, VERSION, derivePayload(getProperties()));
		try{
			parseFields();
		}catch(BadPacketException ignored) {} //we don't care when sending out.
	}
	
	/**
	 * a constructor which takes explicit properties object.
	 * no verification is done, so use only for testing
	 */
	public FeaturesVendorMessage(Properties props) {
		super(F_LIME_VENDOR_ID, F_FEATURES, VERSION, derivePayload(props));
		_properties = props;
		try{
			parseFields();
		}catch(BadPacketException ignored) {ignored.printStackTrace();} //this is only for testing.
	}
	
	private static byte[] derivePayload(Properties props) {
		StringBuffer ret = new StringBuffer();
		
		synchronized(ret) {
			for (Iterator iter = props.keySet().iterator();iter.hasNext();) {
				String key = (String)iter.next();
				String value = props.getProperty(key);
				ret.append(key).append(COL).append(value).append(CR);
			}
		}
		
		//strip the CR at the end
		if (ret.length() > CR.length())
			ret.delete(ret.lastIndexOf(CR),ret.length());
		
		return ret.toString().getBytes();
	}
	
	/**
	 * gets some properties from the system.
	 */
	private static Properties getProperties() {
		Properties  ret = new Properties();
		
		ret.setProperty(OS,CommonUtils.getOS().toLowerCase());
		ret.setProperty(JVM,CommonUtils.getJavaVersion());
		ret.setProperty(FILES_SHARED,""+RouterService.getFileManager().getNumFiles());
		ret.setProperty(INCOMING_TCP,""+RouterService.acceptedIncomingConnection());
		ret.setProperty(INCOMING_UDP,""+RouterService.isOOBCapable());
		
		//FIXME: decide which parameter to use for the available bandwith.
		ret.setProperty(BANDWIDTH,""+(int)RouterService.getConnectionManager().
					getMeasuredUpstreamBandwidth());
		//add more here
		
		return ret;
	}
	
	/**
	 * @return the operating system the node reported.
	 */
	public String getOS() {
		return _properties.getProperty(OS);
	}
	
	/**
	 * @return the java version the node reported
	 */
	public String getJVM() {
		return _properties.getProperty(JVM);
	}
	
	/**
	 * 
	 * @return the number of shared files the node reported
	 */
	public int getFileShared() {
		return _filesShared;
	}
	
	/**
	 * 
	 * @return whether the node reported that it can receive incoming TCP
	 */
	public boolean isTCPCapable() {
		return _inTcp;
	}
	
	/**
	 * 
	 * @return whether the node reported that it can receive incoming UDP
	 */
	public boolean isUDPCapable() {
		return _inUdp;
	}
	
	/**
	 * 
	 * @return the reported upstream bandwidth by the node.
	 */
	public int getBandidth() {
		return _bandwith;
	}
}
