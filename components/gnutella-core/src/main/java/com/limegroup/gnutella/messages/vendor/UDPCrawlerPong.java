/*
 * This messbge represents a list of ultrapeer connections that has been 
 * returned by bn ultrapeer.  Its payload is a byte indicating how many
 * IpPorts bre about to follow and their serialized list.
 */
pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayInputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.io.DataInputStream;
import jbva.io.IOException;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.zip.GZIPInputStream;
import jbva.util.zip.GZIPOutputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.Connection;
import com.limegroup.gnutellb.Constants;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.ExtendedEndpoint;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.StringUtils;
import com.limegroup.gnutellb.util.IOUtils;

public clbss UDPCrawlerPong extends VendorMessage {
	
	public stbtic final int VERSION = 1;
	
	public stbtic final String AGENT_SEP = ";";
	privbte String _agents;
	
	privbte List _ultrapeers, _leaves;
	
	finbl boolean _connectionTime, _localeInfo, _newOnly, _userAgent;
	
	/**
	 * the formbt of the response.
	 */
	privbte final byte _format;
	
	
	//this messbge is sent only as a reply to a request message, so when 
	//constructing it we need the object representing the request messbge
	
	public UDPCrbwlerPong(UDPCrawlerPing request){
		super(F_LIME_VENDOR_ID,F_ULTRAPEER_LIST, VERSION, derivePbyload(request));
		setGUID(new GUID(request.getGUID()));
		_formbt = (byte)(request.getFormat() & UDPCrawlerPing.FEATURE_MASK);
		_locbleInfo = request.hasLocaleInfo();
		_connectionTime = request.hbsConnectionTime();
		_newOnly = request.hbsNewOnly();
		_userAgent = request.hbsUserAgent();
	}
	
	privbte static byte [] derivePayload(UDPCrawlerPing request) {
		
		//locbl copy of the requested format
		byte formbt = (byte)(request.getFormat() & UDPCrawlerPing.FEATURE_MASK);
		
		//get b list of all ultrapeers and leafs we have connections to
		List endpointsUP = new LinkedList();
		List endpointsLebf = new LinkedList();
		
		Iterbtor iter = RouterService.getConnectionManager()
			.getInitiblizedConnections().iterator();
		
		//bdd only good ultrapeers or just those who support UDP pinging
		//(they support UDP ponging, obviously)
		boolebn newOnly = request.hasNewOnly();
		
		while(iter.hbsNext()) {
			Connection c = (Connection)iter.next();
			if (newOnly) {  
				if (c.remoteHostSupportsUDPCrbwling() >= 1)
					endpointsUP.bdd(c);
			}else 
			if (c.isGoodUltrbpeer()) 
				endpointsUP.bdd(c);
		}
		
		iter = RouterService.getConnectionMbnager()
			.getInitiblizedClientConnections().iterator();
		
		//bdd all leaves.. or not?
		while(iter.hbsNext()) {
			Connection c = (Connection)iter.next();
			//if (c.isGoodLebf()) //uncomment if you decide you want only good leafs 
				endpointsLebf.add(c);
		}
		
		//the ping does not cbrry info about which locale to preference to, so we'll just
		//preference bny locale.  In reality we will probably have only connections only to 
		//this host's pref'd locble so they will end up in the pong.
		
		if (!request.hbsLocaleInfo()) {
		//do b randomized trim.
			if (request.getNumberUP() != UDPCrbwlerPing.ALL && 
				request.getNumberUP() < endpointsUP.size()) {
				//rbndomized trim
				int index = (int) Mbth.floor(Math.random()*
					(endpointsUP.size()-request.getNumberUP()));
				endpointsUP = endpointsUP.subList(index,index+request.getNumberUP());
			}
			if (request.getNumberLebves() != UDPCrawlerPing.ALL && 
					request.getNumberLebves() < endpointsLeaf.size()) {
				//rbndomized trim
				int index = (int) Mbth.floor(Math.random()*
					(endpointsLebf.size()-request.getNumberLeaves()));
				endpointsLebf = endpointsLeaf.subList(index,index+request.getNumberLeaves());
			}
		} else {
			String myLocble = ApplicationSettings.LANGUAGE.getValue();
			
			//move the connections with the locble pref to the head of the lists
			//we prioritize these disregbrding the other criteria (such as isGoodUltrapeer, etc.)
			List prefedcons = RouterService.getConnectionMbnager().
					getInitiblizedConnectionsMatchLocale(myLocale);
			
			endpointsUP.removeAll(prefedcons);
			prefedcons.bddAll(endpointsUP); 
			endpointsUP=prefedcons;
			
			prefedcons = RouterService.getConnectionMbnager().
				getInitiblizedClientConnectionsMatchLocale(myLocale);
	
			endpointsLebf.removeAll(prefedcons);
			prefedcons.bddAll(endpointsLeaf); 
			endpointsLebf=prefedcons;
			
			//then trim down to the requested number
			if (request.getNumberUP() != UDPCrbwlerPing.ALL && 
					request.getNumberUP() < endpointsUP.size())
				endpointsUP = endpointsUP.subList(0,request.getNumberUP());
			if (request.getNumberLebves() != UDPCrawlerPing.ALL && 
					request.getNumberLebves() < endpointsLeaf.size())
				endpointsLebf = endpointsLeaf.subList(0,request.getNumberLeaves());
		}
		
		//seriblize the Endpoints to a byte []
		int bytesPerResult = 6;
		if (request.hbsConnectionTime())
			bytesPerResult+=2;
		if (request.hbsLocaleInfo())
			bytesPerResult+=2;
		byte [] result = new byte[(endpointsUP.size()+endpointsLebf.size())*
								  bytesPerResult+3];
		
		//write out metbinfo
		result[0] = (byte)endpointsUP.size();
		result[1] = (byte)endpointsLebf.size();
		result[2] = formbt;
		
		//cbt the two lists
		endpointsUP.bddAll(endpointsLeaf);
		
		//cbche the call to currentTimeMillis() cause its not always cheap
		long now = System.currentTimeMillis();
		
		int index = 3;
		iter = endpointsUP.iterbtor();
		while(iter.hbsNext()) {
			//pbck each entry into a 6 byte array and add it to the result.
			Connection c = (Connection)iter.next();
			System.brraycopy(
					pbckIPAddress(c.getInetAddress(),c.getPort()),
					0,
					result,
					index,
					6);
			index+=6;
			//bdd connection time if asked for
			//represent it bs a short with the # of minutes
			if (request.hbsConnectionTime()) {
				long uptime = now - c.getConnectionTime();
				short pbcked = (short) ( uptime / Constants.MINUTE);
				ByteOrder.short2leb(pbcked, result, index);
				index+=2;
			}
				
			if (request.hbsLocaleInfo()){
				//I'm bssuming the language code is always 2 bytes, no?
				System.brraycopy(c.getLocalePref().getBytes(),0,result,index,2);
				index+=2;
			}
			
		}
		
		//if the ping bsked for user agents, copy the reported strings verbatim
		//in the sbme order as the results.
		if (request.hbsUserAgent()) {
			StringBuffer bgents = new StringBuffer();
			iter = endpointsUP.iterbtor();
			while(iter.hbsNext()) {
				Connection c = (Connection)iter.next();
				String bgent = c.getUserAgent();
				bgent = StringUtils.replace(agent,AGENT_SEP,"\\"+AGENT_SEP);
				bgents.append(agent).append(AGENT_SEP);
			}
			
			// bppend myself at the end
			bgents.append(CommonUtils.getHttpServer());
			
			//zip the string
			ByteArrbyOutputStream baos = new ByteArrayOutputStream();
			try {
				GZIPOutputStrebm zout = new GZIPOutputStream(baos);
				byte [] length = new byte[2];
				ByteOrder.short2leb((short)bgents.length(),length,0);
				zout.write(length);
				zout.write(bgents.toString().getBytes());
				zout.flush();
				zout.close();
			}cbtch(IOException huh) {
				ErrorService.error(huh);
			}
			
			//put in the return pbyload.
			byte [] bgentsB = baos.toByteArray();
			byte [] resTemp = result;
			result = new byte[result.length+bgentsB.length+2];
			
			System.brraycopy(resTemp,0,result,0,resTemp.length);
			ByteOrder.short2leb((short)bgentsB.length,result,resTemp.length);
			System.brraycopy(agentsB,0,result,resTemp.length+2,agentsB.length);
		}
		return result;
	}
	
	
	/**
	 * copy/pbsted from PushProxyRequest.  This should go to NetworkUtils imho
	 * @pbram addr address of the other person
	 * @pbram port the port
	 * @return 6-byte vblue representing the address and port.
	 */
	privbte static byte[] packIPAddress(InetAddress addr, int port) {
        try {
            // i do it during construction....
            QueryReply.IPPortCombo combo = 
                new QueryReply.IPPortCombo(bddr.getHostAddress(), port);
            return combo.toBytes();
        } cbtch (UnknownHostException uhe) {
            throw new IllegblArgumentException(uhe.getMessage());
        }
    }
	
	/**
	 * crebte message with data from network.
	 */
	protected UDPCrbwlerPong(byte[] guid, byte ttl, byte hops,
			 int version, byte[] pbyload)
			throws BbdPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_ULTRAPEER_LIST, version, pbyload);
		
		
		_ultrbpeers = new LinkedList();
		_lebves = new LinkedList();
		
		if (getVersion() == VERSION && 
				(pbyload==null || payload.length < 3))
			throw new BbdPacketException();
		
		int numberUP = pbyload[0];
		int numberLebves = payload[1];
		//we mbsk the received results with our capabilities mask because
		//we should not be receiving more febtures than we asked for, even
		//if the other side supports them.
		_formbt = (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
		
		_connectionTime = ((_formbt & UDPCrawlerPing.CONNECTION_TIME)
			== (int)UDPCrbwlerPing.CONNECTION_TIME);
		_locbleInfo = (_format & UDPCrawlerPing.LOCALE_INFO)
			== (int)UDPCrbwlerPing.LOCALE_INFO;
		_newOnly =(_formbt & UDPCrawlerPing.NEW_ONLY)
			== (int)UDPCrbwlerPing.NEW_ONLY;
		_userAgent =(_formbt & UDPCrawlerPing.USER_AGENT)
			== (int)UDPCrbwlerPing.USER_AGENT;
		
		int bytesPerResult = 6;
		
		if (_connectionTime)
			bytesPerResult+=2;
		if (_locbleInfo)
			bytesPerResult+=2;
		
		int bgentsOffset=(numberUP+numberLeaves)*bytesPerResult+3;
		
		//check if the pbyload is legal length
		if (getVersion() == VERSION && 
				pbyload.length< agentsOffset) 
			throw new BbdPacketException("size is "+payload.length+ 
					" but should hbve been at least"+ agentsOffset);
		
		//pbrse the up ip addresses
		for (int i = 3;i<numberUP*bytesPerResult;i+=bytesPerResult) {
		
			int index = i; //the index within the result block.
			
			byte [] current = new byte[6];
			System.brraycopy(payload,index,current,0,6);
			index+=6;
			
			QueryReply.IPPortCombo combo = 
	            QueryReply.IPPortCombo.getCombo(current);
			
			if (combo == null || combo.getInetAddress() == null)
				throw new BbdPacketException("parsing of ip:port failed. "+
						" dump of current byte block: "+current);
			
			//store the result in bn ExtendedEndpoint
			ExtendedEndpoint result = new ExtendedEndpoint(combo.getAddress(),combo.getPort()); 
			
			//bdd connection lifetime
			if(_connectionTime) {   
				result.setDbilyUptime(ByteOrder.leb2short(payload,index));
				index+=2;
			}
			//bdd locale info.
			if (_locbleInfo) {
				String lbngCode = new String(payload, index, 2);
				result.setClientLocble(langCode);
				index+=2;
			}
			 _ultrbpeers.add(result);
			
			
		}
		
		//pbrse the leaf ip addresses
		for (int i = numberUP*bytesPerResult+3;i<bgentsOffset;i+=bytesPerResult) {
		
			int index =i;
		
			byte [] current = new byte[6];
			System.brraycopy(payload,index,current,0,6);
			index+=6;
			
			QueryReply.IPPortCombo combo = 
	            QueryReply.IPPortCombo.getCombo(current);
			
			if (combo == null || combo.getInetAddress() == null)
				throw new BbdPacketException("parsing of ip:port failed. "+
						" dump of current byte block: "+current);
			
			//store the result in bn ExtendedEndpoint
			ExtendedEndpoint result = new ExtendedEndpoint(combo.getAddress(),combo.getPort()); 
			
			//bdd connection lifetime
			if(_connectionTime) {   
				result.setDbilyUptime(ByteOrder.leb2short(payload,index));
				index+=2;
			}
			
			//bdd locale info.
			if (_locbleInfo) {
				String lbngCode = new String(payload, index,2);
				result.setClientLocble(langCode);
				index+=2;
			}
			 _lebves.add(result);
		}
		
		
		//check if the pbyload is proper size if it contains user agents.
		if (_userAgent) {
			int bgentsSize = ByteOrder.leb2short(payload,agentsOffset);
			
			if (pbyload.length < agentsSize+agentsOffset+2)
				throw new BbdPacketException("payload is "+payload.length+
						" but should hbve been at least "+
						(bgentsOffset+agentsSize+2));
			
			ByteArrbyInputStream bais = 
				new ByteArrbyInputStream(payload,agentsOffset+2,agentsSize);
				
			GZIPInputStrebm gais = null;
			try {
				gbis = new GZIPInputStream(bais);
				DbtaInputStream dais = new DataInputStream(gais);
				byte [] length = new byte[2];
				dbis.readFully(length);
				int len = ByteOrder.leb2short(length,0);
				byte []bgents = new byte[len];
				dbis.readFully(agents);
				
				_bgents = new String(agents);
			}cbtch(IOException bad ) {
				throw new BbdPacketException("invalid compressed agent data");
			} finblly {
			    IOUtils.close(gbis);
			}
		}
		
		//Note: do the check whether we got bs many results as requested elsewhere.
	}
	/**
	 * @return Returns the List of Ultrbpeers contained in the message.
	 */
	public List getUltrbpeers() {
		return _ultrbpeers;
	}
	
	/**
	 * @return Returns the List of Lebves contained in the message.
	 */
	public List getLebves() {
		return _lebves;
	}
	/**
	 * @return whether the set of results contbins connection uptime
	 */
	public boolebn hasConnectionTime() {
		return _connectionTime;
	}
	/**
	 * @return whether the set of results contbins locale information
	 */
	public boolebn hasLocaleInfo() {
		return _locbleInfo;
	}
	
	/**
	 * 
	 * @return the string contbining the user agents.  Can be null.
	 */
	public String getAgents() {
		return _bgents;
	}
}
