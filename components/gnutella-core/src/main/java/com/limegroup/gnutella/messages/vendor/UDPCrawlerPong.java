/*
 * This message represents a list of ultrapeer connections that has been 
 * returned by an ultrapeer.  Its payload is a byte indicating how many
 * IpPorts are about to follow and their serialized list.
 */
package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.IPPortCombo;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.StringUtils;

public class UDPCrawlerPong extends VendorMessage {
	
	public static final int VERSION = 1;
	
	public static final String AGENT_SEP = ";";
	
	//this message is sent only as a reply to a request message, so when 
	//constructing it we need the object representing the request message
	
	public UDPCrawlerPong(UDPCrawlerPing request){
		super(F_LIME_VENDOR_ID,F_CRAWLER_PONG, VERSION, derivePayload(request));
		setGUID(new GUID(request.getGUID()));
	}
	
	private static byte [] derivePayload(UDPCrawlerPing request) {
		
		//local copy of the requested format
		byte format = (byte)(request.getFormat() & UDPCrawlerPing.FEATURE_MASK);
		
		//get a list of all ultrapeers and leafs we have connections to
		List<Connection> endpointsUP = new LinkedList<Connection>();
		List<Connection> endpointsLeaf = new LinkedList<Connection>();
		
		//add only good ultrapeers or just those who support UDP pinging
		//(they support UDP ponging, obviously)
		boolean newOnly = request.hasNewOnly();
		
        for(Connection c : RouterService.getConnectionManager().getInitializedConnections()) {
			if (newOnly) {  
				if (c.remoteHostSupportsUDPCrawling() >= 1)
					endpointsUP.add(c);
			} else if (c.isGoodUltrapeer())  {
			    endpointsUP.add(c);
            }
		}
		
		//add all leaves.. or not?
        for(Connection c : RouterService.getConnectionManager().getInitializedClientConnections()) {
			//if (c.isGoodLeaf()) //uncomment if you decide you want only good leafs 
				endpointsLeaf.add(c);
		}
		
		//the ping does not carry info about which locale to preference to, so we'll just
		//preference any locale.  In reality we will probably have only connections only to 
		//this host's pref'd locale so they will end up in the pong.
		
		if (!request.hasLocaleInfo()) {
		//do a randomized trim.
			if (request.getNumberUP() != UDPCrawlerPing.ALL && 
				request.getNumberUP() < endpointsUP.size()) {
				//randomized trim
				int index = (int) Math.floor(Math.random()*
					(endpointsUP.size()-request.getNumberUP()));
				endpointsUP = endpointsUP.subList(index,index+request.getNumberUP());
			}
			if (request.getNumberLeaves() != UDPCrawlerPing.ALL && 
					request.getNumberLeaves() < endpointsLeaf.size()) {
				//randomized trim
				int index = (int) Math.floor(Math.random()*
					(endpointsLeaf.size()-request.getNumberLeaves()));
				endpointsLeaf = endpointsLeaf.subList(index,index+request.getNumberLeaves());
			}
		} else {
			String myLocale = ApplicationSettings.LANGUAGE.getValue();
			
			//move the connections with the locale pref to the head of the lists
			//we prioritize these disregarding the other criteria (such as isGoodUltrapeer, etc.)
			List<Connection> prefedcons =
                RouterService.getConnectionManager().getInitializedConnectionsMatchLocale(myLocale);
			
			endpointsUP.removeAll(prefedcons);
			prefedcons.addAll(endpointsUP); 
			endpointsUP=prefedcons;
			
			prefedcons =
                RouterService.getConnectionManager().getInitializedClientConnectionsMatchLocale(myLocale);
	
			endpointsLeaf.removeAll(prefedcons);
			prefedcons.addAll(endpointsLeaf); 
			endpointsLeaf=prefedcons;
			
			//then trim down to the requested number
			if (request.getNumberUP() != UDPCrawlerPing.ALL && 
					request.getNumberUP() < endpointsUP.size())
				endpointsUP = endpointsUP.subList(0,request.getNumberUP());
			if (request.getNumberLeaves() != UDPCrawlerPing.ALL && 
					request.getNumberLeaves() < endpointsLeaf.size())
				endpointsLeaf = endpointsLeaf.subList(0,request.getNumberLeaves());
		}
		
		//serialize the Endpoints to a byte []
		int bytesPerResult = 6;
		if (request.hasConnectionTime())
			bytesPerResult+=2;
		if (request.hasLocaleInfo())
			bytesPerResult+=2;

        int index;
		if(request.hasNodeUptime()) {
		    index = 7;
		} else {
		    index = 3;
		}
		
        byte [] result = new byte[(endpointsUP.size()+endpointsLeaf.size())*
								  bytesPerResult+index];
		
		//write out metainfo
		result[0] = (byte)endpointsUP.size();
		result[1] = (byte)endpointsLeaf.size();
		result[2] = format;
		
        if(request.hasNodeUptime()) {
            long currentAverage = RouterService.getConnectionManager().getCurrentAverageUptime()/1000L;//in sec
            if(currentAverage > Integer.MAX_VALUE)
                currentAverage = Integer.MAX_VALUE;
            ByteOrder.int2leb((int)currentAverage, result, 3);
		}
        
		//cat the two lists
		endpointsUP.addAll(endpointsLeaf);
		
        //cache the call to currentTimeMillis() cause its not always cheap
		long now = System.currentTimeMillis();
		
        for(Connection c : endpointsUP) {
			//pack each entry into a 6 byte array and add it to the result.
			System.arraycopy(
					packIPAddress(c.getInetAddress(),c.getPort()),
					0,
					result,
					index,
					6);
			index+=6;
			//add connection time if asked for
			//represent it as a short with the # of minutes
			if (request.hasConnectionTime()) {
				long uptime = now - c.getConnectionTime();
				short packed = (short) ( uptime / Constants.MINUTE);
				ByteOrder.short2leb(packed, result, index);
				index+=2;
			}
				
			if (request.hasLocaleInfo()){
				//I'm assuming the language code is always 2 bytes, no?
				System.arraycopy(c.getLocalePref().getBytes(),0,result,index,2);
				index+=2;
			}
			
		}
		
		//if the ping asked for user agents, copy the reported strings verbatim
		//in the same order as the results.
		if (request.hasUserAgent()) {
			StringBuffer agents = new StringBuffer();
            for(Connection c: endpointsUP) {
				String agent = c.getUserAgent();
				agent = StringUtils.replace(agent,AGENT_SEP,"\\"+AGENT_SEP);
				agents.append(agent).append(AGENT_SEP);
			}
			
			// append myself at the end
			agents.append(CommonUtils.getHttpServer());
			
			//zip the string
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				GZIPOutputStream zout = new GZIPOutputStream(baos);
				byte [] length = new byte[2];
				ByteOrder.short2leb((short)agents.length(),length,0);
				zout.write(length);
				zout.write(agents.toString().getBytes());
				zout.flush();
				zout.close();
			}catch(IOException huh) {
				ErrorService.error(huh);
			}
			
			//put in the return payload.
			byte [] agentsB = baos.toByteArray();
			byte [] resTemp = result;
			result = new byte[result.length+agentsB.length+2];
			
			System.arraycopy(resTemp,0,result,0,resTemp.length);
			ByteOrder.short2leb((short)agentsB.length,result,resTemp.length);
			System.arraycopy(agentsB,0,result,resTemp.length+2,agentsB.length);
		}
		return result;
	}
	
	
	/**
	 * copy/pasted from PushProxyRequest.  This should go to NetworkUtils imho
	 * @param addr address of the other person
	 * @param port the port
	 * @return 6-byte value representing the address and port.
	 */
	private static byte[] packIPAddress(InetAddress addr, int port) {
        try {
            // i do it during construction....
            IPPortCombo combo = 
                new IPPortCombo(addr.getHostAddress(), port);
            return combo.toBytes();
        } catch (UnknownHostException uhe) {
            throw new IllegalArgumentException(uhe.getMessage());
        }
    }
}
