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
	
	
	private static final String CR="\n";
	private static final String COL=":";
	
	/**
	 * the properties object.
	 */
	Properties _properties;
	
	/**
	 * constructs an object with data from the network
	 */
	protected FeaturesVendorMessage(byte[] guid, byte ttl, byte hops, 
                   int version, byte[] payload) throws BadPacketException {
		super(guid,ttl,hops,F_LIME_VENDOR_ID, F_FEATURES,version, payload);
		
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
	}
	
	/**
	 * creates a Vendor Message containing some of our features.
	 */
	public FeaturesVendorMessage() {
		super(F_LIME_VENDOR_ID, F_FEATURES, VERSION, derivePayload(getProperties()));
	}
	
	/**
	 * a constructor which takes explicit properties object.
	 * no verification is done, so use only for testing
	 */
	public FeaturesVendorMessage(Properties props) {
		super(F_LIME_VENDOR_ID, F_FEATURES, VERSION, derivePayload(props));
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
		
		ret.setProperty(OS,CommonUtils.getOSVersion());
		ret.setProperty(JVM,CommonUtils.getJavaVersion());
		ret.setProperty(FILES_SHARED,""+RouterService.getFileManager().getNumFiles());
		ret.setProperty(INCOMING_TCP,""+RouterService.acceptedIncomingConnection());
		ret.setProperty(INCOMING_UDP,""+RouterService.isOOBCapable());
		//add more here
		
		return ret;
	}
}
