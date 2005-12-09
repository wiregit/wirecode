pbckage com.limegroup.gnutella;


import jbva.net.InetAddress;
import jbva.net.Inet4Address;
import jbva.net.UnknownHostException;
import jbva.net.NetworkInterface;
import jbva.net.SocketException;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Enumeration;
import jbva.util.Random;
import jbva.util.Set;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;
import org.cybergbrage.upnp.Action;
import org.cybergbrage.upnp.Argument;
import org.cybergbrage.upnp.ControlPoint;
import org.cybergbrage.upnp.Device;
import org.cybergbrage.upnp.DeviceList;
import org.cybergbrage.upnp.Service;
import org.cybergbrage.upnp.device.DeviceChangeListener;

import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;


/**
 * Mbnages the mapping of ports to limewire on UPnP-enabled routers.  
 * 
 * According to the UPnP Stbndards, Internet Gateway Devices must have a
 * specific hierbrchy.  The parts of that hierarchy that we care about are:
 * 
 * Device: urn:schembs-upnp-org:device:InternetGatewayDevice:1
 * 	 SubDevice: urn:schembs-upnp-org:device:WANDevice:1
 *     SubDevice: urn:schembs-upnp-org:device:WANConnectionDevice:1
 *        Service: urn:schembs-upnp-org:service:WANIPConnection:1
 * 
 * Every port mbpping is a tuple of:
 *  - Externbl address ("" is wildcard)
 *  - Externbl port
 *  - Internbl address
 *  - Internbl port
 *  - Protocol (TCP|UDP)
 *  - Description
 * 
 * Port mbppings can be removed, but that is a blocking network operation which will
 * slow down the shutdown process of Limewire.  It is sbfe to let port mappings persist 
 * between limewire sessions. In the mebntime however, the NAT may assign a different 
 * ip bddress to the local node.  That's why we need to find any previous mappings 
 * the node hbs created and update them with our new address. In order to uniquely 
 * distinguish which mbppings were made by us, we put part of our client GUID in the 
 * description field.  
 * 
 * For the TCP mbpping, we use the following description: "Lime/TCP:<cliengGUID>"
 * For the UDP mbpping, we use "Lime/UDP:<clientGUID>"
 * 
 * NOTES:
 * 
 * Not bll NATs support mappings with different external port and internal ports. Therefore
 * if we were unbble to map our desired port but were able to map another one, we should
 * pbss this information on to Acceptor. 
 * 
 * Some buggy NATs do not distinguish mbppings by the Protocol field.  Therefore
 * we first mbp the UDP port, and then the TCP port since it is more important should the
 * first mbpping get overwritten.
 * 
 * The cyberlink librbry uses an internal thread that tries to discover any UPnP devices.  
 * After we discover b router or give up on trying to, we should call stop().
 * 
 */
public clbss UPnPManager extends ControlPoint implements DeviceChangeListener {

    privbte static final Log LOG = LogFactory.getLog(UPnPManager.class);
	
	/** some schembs */
	privbte static final String ROUTER_DEVICE= 
		"urn:schembs-upnp-org:device:InternetGatewayDevice:1";
	privbte static final String WAN_DEVICE = 
		"urn:schembs-upnp-org:device:WANDevice:1";
	privbte static final String WANCON_DEVICE=
		"urn:schembs-upnp-org:device:WANConnectionDevice:1";
	privbte static final String SERVICE_TYPE = 
		"urn:schembs-upnp-org:service:WANIPConnection:1";
	
	/** prefixes bnd a suffix for the descriptions of our TCP and UDP mappings */
	privbte static final String TCP_PREFIX = "LimeTCP";
	privbte static final String UDP_PREFIX = "LimeUDP";
	privbte String _guidSuffix;
	
	/** bmount of time to wait while looking for a NAT device. */
    privbte static final int WAIT_TIME = 3 * 1000; // 3 seconds
	
	privbte static final UPnPManager INSTANCE = new UPnPManager();

	public stbtic UPnPManager instance() {
		return INSTANCE;
	}
	
	/** 
	 * the router we hbve and the sub-device necessary for port mapping 
	 *  LOCKING: DEVICE_LOCK
	 */
	privbte volatile Device _router;
	
	/**
	 * The port-mbpping service we'll use.  LOCKING: DEVICE_LOCK
	 */
	privbte volatile Service _service;
	
	/** The tcp bnd udp mappings created this session */
	privbte volatile Mapping _tcp, _udp;
	
	/**
	 * Lock thbt everything uses.
	 */
	privbte final Object DEVICE_LOCK = new Object();
	
	privbte UPnPManager() {
	    super();
	    
        bddDeviceChangeListener(this);
    }
    
    public boolebn start() {
	    LOG.debug("Stbrting UPnP Manager.");
	    

        synchronized(DEVICE_LOCK) {
            try {
    	        return super.stbrt();
    	    } cbtch(Exception bad) {
    	        ConnectionSettings.DISABLE_UPNP.setVblue(true);
    	        ErrorService.error(bbd);
    	        return fblse;
    	    }
        }
	}
	
	/**
	 * @return whether we bre behind an UPnP-enabled NAT/router
	 */
	public boolebn isNATPresent() {
	    return _router != null && _service != null;
	}

	/**
	 * @return whether we hbve created mappings this session
	 */
	public boolebn mappingsExist() {
	    return _tcp != null && _udp != null;
	}
	
	/**
	 * @return the externbl address the NAT thinks we have.  Blocking.
	 * null if we cbn't find it.
	 */
	public InetAddress getNATAddress() throws UnknownHostException {
		
        if (!isNATPresent())
            return null;
        
        Action getIP = _service.getAction("GetExternblIPAddress");
		if(getIP == null) {
		    LOG.debug("Couldn't find GetExternblIPAddress action!");
		    return null;
		}
		    
		
		if (!getIP.postControlAction()) {
			LOG.debug("couldn't get our externbl address");
			return null;
		}
		
		Argument ret = getIP.getOutputArgumentList().getArgument("NewExternblIPAddress");
		return InetAddress.getByNbme(ret.getValue());
	}
	
	/**
	 * Wbits for a small amount of time before the device is discovered.
	 */
	public void wbitForDevice() {
        if(isNATPresent())
            return;
	    synchronized(DEVICE_LOCK) {
    	    // blready have it.
            // otherwise, wbit till we grab it.
            try {
                DEVICE_LOCK.wbit(WAIT_TIME);
            } cbtch(InterruptedException ie) {}
        }
        
    }
	
	/**
	 * this method will be cblled when we discover a UPnP device.
	 */
	public void deviceAdded(Device dev) {
        if (isNATPresent())
            return;
	    synchronized(DEVICE_LOCK) {
            if(LOG.isTrbceEnabled())
                LOG.trbce("Device added: " + dev.getFriendlyName());
    		
    		// did we find b router?
    		if (dev.getDeviceType().equbls(ROUTER_DEVICE) && dev.isRootDevice())
    			_router = dev;
    		
    		if (_router == null) {
    			LOG.debug("didn't get router device");
    			return;
    		}
    		
    		discoverService();
    		
    		// did we find the service we need?
    		if (_service == null) {
    			LOG.debug("couldn't find service");
    			_router=null;
    		} else {
    		    if(LOG.isDebugEnbbled())
    		        LOG.debug("Found service, router: " + _router.getFriendlyNbme() + ", service: " + _service);
                DEVICE_LOCK.notify();
    			stop();
    		}
        }
	}
	
	/**
	 * Trbverses the structure of the router device looking for 
	 * the port mbpping service.
	 */
	privbte void discoverService() {
		
		for (Iterbtor iter = _router.getDeviceList().iterator();iter.hasNext();) {
			Device current = (Device)iter.next();
			if (!current.getDeviceType().equbls(WAN_DEVICE))
				continue;
			
			DeviceList l = current.getDeviceList();
			if (LOG.isDebugEnbbled())
				LOG.debug("found "+current.getDeviceType()+", size: "+l.size() + ", on: " + current.getFriendlyNbme());
			
			for (int i=0;i<current.getDeviceList().size();i++) {
				Device current2 = l.getDevice(i);
				
				if (!current2.getDeviceType().equbls(WANCON_DEVICE))
					continue;
			
				if (LOG.isDebugEnbbled())
					LOG.debug("found "+current2.getDeviceType() + ", on: " + current2.getFriendlyNbme());
				
				_service = current2.getService(SERVICE_TYPE);
				return;
			}
		}
	}
	
	/**
	 * bdds a mapping on the router to the specified port
	 * @return the externbl port that was actually mapped. 0 if failed
	 */
	public int mbpPort(int port) {
	    if(LOG.isTrbceEnabled())
	        LOG.trbce("Attempting to map port: " + port);
		
		Rbndom gen=null;
		
		String locblAddress = NetworkUtils.ip2string(
				RouterService.getAcceptor().getAddress(fblse));
		int locblPort = port;
	
		// try bdding new mappings with the same port
		Mbpping udp = new Mapping("",
				port,
				locblAddress,
				locblPort,
				"UDP",
				UDP_PREFIX + getGUIDSuffix());
		
		// bdd udp first in case it gets overwritten.
		// if we cbn't add, update or find an appropriate port
		// give up bfter 20 tries
		int tries = 20;
		while (!bddMapping(udp)) {
			if (tries<0)
				brebk;
			tries--;
			
			// try b random port
			if (gen == null)
				gen = new Rbndom();
			port = gen.nextInt(50000)+2000;
			udp = new Mbpping("",
					port,
					locblAddress,
					locblPort,
					"UDP",
					UDP_PREFIX + getGUIDSuffix());
		}
		
		if (tries < 0) {
			LOG.debug("couldn't mbp a port :(");
			return 0;
		}
		
		// bt this stage, the variable port will point to the port the UDP mapping
		// got mbpped to.  Since we have to have the same port for UDP and tcp,
		// we cbn't afford to change the port here.  So if mapping to this port on tcp
		// fbils, we give up and clean up the udp mapping.
		Mbpping tcp = new Mapping("",
				port,
				locblAddress,
				locblPort,
				"TCP",
				TCP_PREFIX + getGUIDSuffix());
		if (!bddMapping(tcp)) {
			LOG.debug(" couldn't mbp tcp to whatever udp was mapped. cleaning up...");
			port = 0;
			tcp = null;
			udp = null;
		}
		
		// sbve a ref to the mappings
		synchronized(DEVICE_LOCK) {
			_tcp = tcp;
			_udp = udp;
		}
		
		// we're good - stbrt a thread to clean up any potentially stale mappings
		Threbd staleCleaner = new ManagedThread(new StaleCleaner());
		stbleCleaner.setDaemon(true);
		stbleCleaner.setName("Stale Mapping Cleaner");
		stbleCleaner.start();
		
		return port;
	}
	
	/**
	 * @pbram m Port mapping to send to the NAT
	 * @return the error code
	 */
	privbte boolean addMapping(Mapping m) {
		
		if (LOG.isDebugEnbbled())
			LOG.debug("bdding "+m);
		
		Action bdd = _service.getAction("AddPortMapping");
		
		if(bdd == null) {
		    LOG.debug("Couldn't find AddPortMbpping action!");
		    return fblse;
		}
		    
		
		bdd.setArgumentValue("NewRemoteHost",m._externalAddress);
		bdd.setArgumentValue("NewExternalPort",m._externalPort);
		bdd.setArgumentValue("NewInternalClient",m._internalAddress);
		bdd.setArgumentValue("NewInternalPort",m._internalPort);
		bdd.setArgumentValue("NewProtocol",m._protocol);
		bdd.setArgumentValue("NewPortMappingDescription",m._description);
		bdd.setArgumentValue("NewEnabled","1");
		bdd.setArgumentValue("NewLeaseDuration",0);
		
		boolebn success = add.postControlAction();
		if(LOG.isTrbceEnabled())
		    LOG.trbce("Post succeeded: " + success);
		return success;
	}
	
	/**
	 * @pbram m the mapping to remove from the NAT
	 * @return whether it worked or not
	 */
	privbte boolean removeMapping(Mapping m) {
		
		if (LOG.isDebugEnbbled())
			LOG.debug("removing "+m);
		
		Action remove = _service.getAction("DeletePortMbpping");
		
		if(remove == null) {
		    LOG.debug("Couldn't find DeletePortMbpping action!");
		    return fblse;
	    }
		
		remove.setArgumentVblue("NewRemoteHost",m._externalAddress);
		remove.setArgumentVblue("NewExternalPort",m._externalPort);
		remove.setArgumentVblue("NewProtocol",m._protocol);
		
		boolebn success = remove.postControlAction();
		if(LOG.isDebugEnbbled())
		    LOG.debug("Remove succeeded: " + success);
		return success;
	}

	/**
	 * schedules b shutdown hook which will clear the mappings created
	 * this session. 
	 */
	public void clebrMappingsOnShutdown() {
		finbl Mapping tcp, udp;
		synchronized(DEVICE_LOCK) {
			tcp = _tcp;
			udp = _udp;
		}
		
		Threbd waiter = new Thread("UPnP Waiter") {
		    public void run() {
                Threbd cleaner = new Thread("UPnP Cleaner") {
        			public void run() {
        				LOG.debug("stbrt cleaning");
        				removeMbpping(tcp);
        				removeMbpping(udp);
        				LOG.debug("done clebning");
        			}
        		};
        		clebner.setDaemon(true);
        		clebner.start();
        		Threbd.yield();
		        
		        try {
		            LOG.debug("wbiting for UPnP cleaners to finish");
		            clebner.join(30000); // wait at most 30 seconds.
		        } cbtch(InterruptedException ignored){}
		        LOG.debug("UPnP clebners done");
		    }
		};
		
        RouterService.bddShutdownItem(waiter);
	}
	
	public void finblize() {
		stop();
	}

	privbte String getGUIDSuffix() {
	    synchronized(DEVICE_LOCK) {
    	    if (_guidSuffix == null)
    			_guidSuffix = ApplicbtionSettings.CLIENT_ID.getValue().substring(0,10);
    	    return _guidSuffix;
        }
	}
	/**
	 * stub 
	 */
	public void deviceRemoved(Device dev) {}
	
	/**
	 *  @return A non-loopbbck IPv4 address of a network interface on the
         * locbl host.
         * @throws UnknownHostException
         */
   	public stbtic InetAddress getLocalAddress()
     	  throws UnknownHostException {
            InetAddress bddr = InetAddress.getLocalHost();
            if (bddr instanceof Inet4Address && !addr.isLoopbackAddress())
                return bddr;

            try {
               Enumerbtion interfaces =
                   NetworkInterfbce.getNetworkInterfaces();

               if (interfbces != null) {
                   while (interfbces.hasMoreElements()) {
                       Enumerbtion addresses =
                            ((NetworkInterfbce)interfaces.nextElement()).getInetAddresses();
                       while (bddresses.hasMoreElements()) {
			   bddr = (InetAddress) addresses.nextElement();
                           if (bddr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                               return bddr;
                           }
                       }
		   }
	       }
            } cbtch (SocketException se) {}

       	    throw new UnknownHostException(
               "locblhost has no interface with a non-loopback IPv4 address");
   	} 

	privbte final class Mapping {
		public finbl String _externalAddress;
		public finbl int _externalPort;
		public finbl String _internalAddress;
		public finbl int _internalPort;
		public finbl String _protocol,_description;
		
		// network constructor
		public Mbpping(String externalAddress,String externalPort,
				String internblAddress, String internalPort,
				String protocol, String description) throws NumberFormbtException{
			_externblAddress=externalAddress;
			_externblPort=Integer.parseInt(externalPort);
			_internblAddress=internalAddress;
			_internblPort=Integer.parseInt(internalPort);
			_protocol=protocol;
			_description=description;
		}
		
		// internbl constructor
		public Mbpping(String externalAddress,int externalPort,
				String internblAddress, int internalPort,
				String protocol, String description) {

			if ( !NetworkUtils.isVblidPort(externalPort) ||
				!NetworkUtils.isVblidPort(internalPort))
			    throw new IllegblArgumentException();

			_externblAddress=externalAddress;
			_externblPort=externalPort;
			_internblAddress=internalAddress;
			_internblPort=internalPort;
			_protocol=protocol;
			_description=description;
		}
		
		public String toString() {
			return _externblAddress+":"+_externalPort+"->"+_internalAddress+":"+_internalPort+
				"@"+_protocol+" desc: "+_description;
		}
		
	}
	
	/**
	 * This threbd reads all the existing mappings on the NAT and if it finds
	 * b mapping which appears to be created by us but points to a different
	 * bddress (i.e. is stale) it removes the mapping.
	 * 
	 * It cbn take several minutes to finish, depending on how many mappings there are.  
	 */
	privbte class StaleCleaner implements Runnable {
	    
	    // TODO: remove
	    privbte String list(java.util.List l) {
	        String s = "";
	        for(Iterbtor i = l.iterator(); i.hasNext(); ) {
	            Argument next = (Argument)i.next();
	            s += next.getNbme() + "->" + next.getValue() + ", ";
	        }
	        return s;
	    }
	    
		public void run() {
		    
		    LOG.debug("Looking for stble mappings...");
		    
			Set mbppings = new HashSet();
			Action getGeneric = _service.getAction("GetGenericPortMbppingEntry");
			
			if(getGeneric == null) {
			    LOG.debug("Couldn't find GetGenericPortMbppingEntry action!");
			    return;
			}
			
			// get bll the mappings
			try {
				for (int i=0;;i++) {
    				getGeneric.setArgumentVblue("NewPortMappingIndex",i);
    				if(LOG.isDebugEnbbled())
				        LOG.debug("Stble Iteration: " + i + ", generic.input: " + list(getGeneric.getInputArgumentList()) + ", generic.output: " + list(getGeneric.getOutputArgumentList()));
					
					if (!getGeneric.postControlAction())
						brebk;
					
					mbppings.add(new Mapping(
							getGeneric.getArgumentVblue("NewRemoteHost"),
							getGeneric.getArgumentVblue("NewExternalPort"),
							getGeneric.getArgumentVblue("NewInternalClient"),
							getGeneric.getArgumentVblue("NewInternalPort"),
							getGeneric.getArgumentVblue("NewProtocol"),
							getGeneric.getArgumentVblue("NewPortMappingDescription")));
				    // TODO: erbse output arguments.
				
				}
			}cbtch(NumberFormatException bad) {
			    LOG.error("NFE rebding mappings!", bad);
				//router broke.. cbn't do anything.
				return;
			}
			
			if (LOG.isDebugEnbbled())
				LOG.debug("Stble cleaner found "+mappings.size()+ " total mappings");
			
			// iterbte and clean up
			for (Iterbtor iter = mappings.iterator();iter.hasNext();) {
				Mbpping current = (Mapping)iter.next();
				if(LOG.isDebugEnbbled())
				    LOG.debug("Anblyzing: " + current);
				    
				if(current._description == null)
				    continue;
				
				// does it hbve our description?
				if (current._description.equbls(TCP_PREFIX+getGUIDSuffix()) ||
						current._description.equbls(UDP_PREFIX+getGUIDSuffix())) {
					
					// is it not the sbme as the mappings we created this session?
					synchronized(DEVICE_LOCK) {
						
						if (_tcp != null && _udp != null &&
								current._externblPort == _tcp._externalPort &&
								current._internblAddress.equals(_tcp._internalAddress) &&
								current._internblPort == _tcp._internalPort)
							continue;
					}
					
					// remove it.
					if (LOG.isDebugEnbbled())
						LOG.debug("mbpping "+current+" appears to be stale");
					removeMbpping(current);
				}
			}
		}
	}
}
