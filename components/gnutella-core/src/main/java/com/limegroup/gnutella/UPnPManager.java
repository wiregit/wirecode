package com.limegroup.gnutella;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.device.DeviceChangeListener;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;


/**
 * Manages the mapping of ports to limewire on UPnP-enabled routers.  
 * 
 * According to the UPnP Standards, Internet Gateway Devices must have a
 * specific hierarchy.  The parts of that hierarchy that we care about are:
 * 
 * Device: urn:schemas-upnp-org:device:InternetGatewayDevice:1
 * 	 SubDevice: urn:schemas-upnp-org:device:WANDevice:1
 *     SubDevice: urn:schemas-upnp-org:device:WANConnectionDevice:1
 *        Service: urn:schemas-upnp-org:service:WANIPConnection:1
 * 
 * Every port mapping is a tuple of:
 *  - External address ("" is wildcard)
 *  - External port
 *  - Internal address
 *  - Internal port
 *  - Protocol (TCP|UDP)
 *  - Description
 * 
 * Port mappings can be removed, but that is a blocking network operation which will
 * slow down the shutdown process of Limewire.  It is safe to let port mappings persist 
 * between limewire sessions. In the meantime however, the NAT may assign a different 
 * ip address to the local node.  That's why we need to find any previous mappings 
 * the node has created and update them with our new address. In order to uniquely 
 * distinguish which mappings were made by us, we put part of our client GUID in the 
 * description field.  
 * 
 * For the TCP mapping, we use the following description: "Lime/TCP:<cliengGUID>"
 * For the UDP mapping, we use "Lime/UDP:<clientGUID>"
 * 
 * NOTES:
 * 
 * Not all NATs support mappings with different external port and internal ports. Therefore
 * if we were unable to map our desired port but were able to map another one, we should
 * pass this information on to Acceptor. 
 * 
 * Some buggy NATs do not distinguish mappings by the Protocol field.  Therefore
 * we first map the UDP port, and then the TCP port since it is more important should the
 * first mapping get overwritten.
 * 
 * The cyberlink library uses an internal thread that tries to discover any UPnP devices.  
 * After we discover a router or give up on trying to, we should call stop().
 * 
 */
public class UPnPManager extends ControlPoint implements DeviceChangeListener{
	
	/** some schemas */
	private static final String ROUTER_DEVICE= 
		"urn:schemas-upnp-org:device:InternetGatewayDevice:1";
	private static final String WAN_DEVICE = 
		"urn:schemas-upnp-org:device:WANDevice:1";
	private static final String WANCON_DEVICE=
		"urn:schemas-upnp-org:device:WANConnectionDevice:1";
	private static final String SERVICE_TYPE = 
		"urn:schemas-upnp-org:service:WANIPConnection:1";
	
	/** prefixes and a suffix for the descriptions of our TCP and UDP mappings */
	private static final String TCP_PREFIX = "Lime/TCP:";
	private static final String UDP_PREFIX = "Lime/UDP:";
	private static final String GUID_SUFFIX = 
		ApplicationSettings.CLIENT_ID.getValue().substring(0,10);
	
	private static final UPnPManager INSTANCE = new UPnPManager();
	
	private static final Log LOG = LogFactory.getLog(UPnPManager.class);
	
	public static UPnPManager instance() {
		return INSTANCE;
	}
	
	/** 
	 * the router we have and the sub-device necessary for port mapping 
	 *  LOCKING: this
	 */
	private Device _router;
	
	/**
	 * The port-mapping service we'll use.  LOCKING: this
	 */
	private Service _service;
	
	/** The tcp and udp mappings created this session */
	private Mapping _tcp, _udp;
	
	private UPnPManager() {
		super();
		addDeviceChangeListener(this);
		
		start();
	}
	
	/**
	 * @return whether we are behind an UPnP-enabled NAT/router
	 */
	public synchronized boolean NATPresent() {
		return _router != null && _service != null;
	}

	/**
	 * @return whether we have created mappings this session
	 */
	public synchronized boolean mappingsExist() {
		return _tcp != null && _udp != null;
	}
	
	/**
	 * @return the external address the NAT thinks we have.  Blocking.
	 * null if we can't find it.
	 */
	public InetAddress getNATAddress() throws UnknownHostException {
		Action getIP;
		
		synchronized(this) {
			if (!NATPresent())
				return null;
			getIP = _service.getAction("GetExternalIPAddress");
		}
		
		if (!getIP.postControlAction()) {
			LOG.debug("couldn't get our external address");
			return null;
		}
		
		Argument ret = getIP.getOutputArgumentList().getArgument("NewExternalIPAddress");
		return InetAddress.getByName(ret.getValue());
	}
	
	/**
	 * this method will be called when we discover a UPnP device.
	 */
	public synchronized void deviceAdded(Device dev) {
		
		// we've found what we need
		if (_service != null && _router != null) {
			LOG.debug("we already have a router");
			return;
		}

		// did we find a router?
		if (dev.getDeviceType().equals(ROUTER_DEVICE) && dev.isRootDevice())
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
			LOG.debug("found service");
			stop();
		}
	}
	
	/**
	 * Traverses the structure of the router device looking for 
	 * the port mapping service.
	 */
	private void discoverService() {
		
		for (Iterator iter = _router.getDeviceList().iterator();iter.hasNext();) {
			Device current = (Device)iter.next();
			if (!current.getDeviceType().equals(WAN_DEVICE))
				continue;
			
			DeviceList l = current.getDeviceList();
			if (LOG.isDebugEnabled())
				LOG.debug("found "+current.getDeviceType()+" "+l.size());
			
			for (int i=0;i<current.getDeviceList().size();i++) {
				Device current2 = l.getDevice(i);
				
				if (!current2.getDeviceType().equals(WANCON_DEVICE))
					continue;
			
				if (LOG.isDebugEnabled())
					LOG.debug("found "+current2.getDeviceType());
				
				_service = current2.getService(SERVICE_TYPE);
				return;
			}
		}
	}
	
	/**
	 * adds a mapping on the router to the specified port
	 * @return the external port that was actually mapped. 0 if failed
	 */
	public int mapPort(int port) {
		
		Random gen=null;
		
		String localAddress = NetworkUtils.ip2string(
				RouterService.getAcceptor().getAddress(false));
		int localPort = port;
	
		// try adding new mappings with the same port
		Mapping udp = new Mapping("",
				""+port,
				localAddress,
				""+localPort,
				"UDP",
				UDP_PREFIX + GUID_SUFFIX);
		
		// add udp first in case it gets overwritten.
		// if we can't add, update or find an appropriate port
		// give up after 20 tries
		int tries = 20;
		while (!addMapping(udp)) {
			if (tries<0)
				break;
			tries--;
			
			// try a random port
			if (gen == null)
				gen = new Random();
			port = gen.nextInt(50000)+2000;
			udp = new Mapping("",
					""+port,
					localAddress,
					""+localPort,
					"UDP",
					UDP_PREFIX + GUID_SUFFIX);
		}
		
		if (tries < 0) {
			LOG.debug("couldn't map a port :(");
			return 0;
		}
		
		// at this stage, the variable port will point to the port the UDP mapping
		// got mapped to.  Since we have to have the same port for UDP and tcp,
		// we can't afford to change the port here.  So if mapping to this port on tcp
		// fails, we give up and clean up the udp mapping.
		Mapping tcp = new Mapping("",
				""+port,
				localAddress,
				""+localPort,
				"TCP",
				TCP_PREFIX + GUID_SUFFIX);
		if (!addMapping(tcp)) {
			LOG.debug(" couldn't map tcp to whatever udp was mapped. cleaning up...");
			port = 0;
			tcp = null;
			udp = null;
		}
		
		// save a ref to the mappings
		synchronized(this) {
			_tcp = tcp;
			_udp = udp;
		}
		
		// we're good - start a thread to clean up any potentially stale mappings
		Thread staleCleaner = new ManagedThread(new StaleCleaner());
		staleCleaner.setDaemon(false);
		staleCleaner.setName("Stale Mapping Cleaner");
		staleCleaner.start();
		
		return port;
	}
	
	/**
	 * @param m Port mapping to send to the NAT
	 * @return the error code
	 */
	private boolean addMapping(Mapping m) {
		
		if (LOG.isDebugEnabled())
			LOG.debug("adding "+m);
		
		Action add;
		synchronized(this) {
			add = _service.getAction("AddPortMapping");
		}
		
		add.setArgumentValue("NewRemoteHost",m._externalAddress);
		add.setArgumentValue("NewExternalPort",m._externalPort);
		add.setArgumentValue("NewInternalClient",m._internalAddress);
		add.setArgumentValue("NewInternalPort",m._internalPort);
		add.setArgumentValue("NewProtocol",m._protocol);
		add.setArgumentValue("NewPortMappingDescription",m._description);
		add.setArgumentValue("NewEnabled","1");
		add.setArgumentValue("NewLeaseDuration",0);
		
		return add.postControlAction();
	}
	
	/**
	 * @param m the mapping to remove from the NAT
	 * @return whether it worked or not
	 */
	private boolean removeMapping(Mapping m) {
		
		if (LOG.isDebugEnabled())
			LOG.debug("removing "+m);
		
		Action remove;
		synchronized(this) {
			remove = _service.getAction("DeletePortMapping");
		}
		
		remove.setArgumentValue("NewRemoteHost",m._externalAddress);
		remove.setArgumentValue("NewExternalPort",m._externalPort);
		remove.setArgumentValue("NewProtocol",m._protocol);
		
		return remove.postControlAction();
	}

	/**
	 * schedules a shutdown hook which will clear the mappings created
	 * this session. 
	 */
	public void clearMappingsOnShutdown() {
		final Mapping tcp, udp;
		synchronized(this) {
			tcp = _tcp;
			udp = _udp;
		}
		
		Runnable cleaner = new Runnable() {
			public void run() {
				LOG.debug("start cleaning");
				removeMapping(tcp);
				removeMapping(udp);
				LOG.debug("done cleaning");
			}
		};
		
		Thread remover = new ManagedThread(cleaner);
		remover.setDaemon(false);
		remover.setName("shutdown mapping cleaner");
		Runtime.getRuntime().addShutdownHook(remover);
	}
	
	public void finalize() {
		stop();
	}

	/**
	 * stub 
	 */
	public void deviceRemoved(Device dev) {}
	
	private final class Mapping {
		public final String _externalAddress;
		public final int _externalPort;
		public final String _internalAddress;
		public final int _internalPort;
		public final String _protocol,_description;
		
		public Mapping(String externalAddress,String externalPort,
				String internalAddress, String internalPort,
				String protocol, String description) throws NumberFormatException{
			_externalAddress=externalAddress;
			_externalPort=Integer.parseInt(externalPort);
			_internalAddress=internalAddress;
			_internalPort=Integer.parseInt(internalPort);
			_protocol=protocol;
			_description=description;
		}
		
		public String toString() {
			return _externalAddress+":"+_externalPort+"->"+_internalAddress+":"+_internalPort+
				"@"+_protocol+" desc: "+_description;
		}
		
	}
	
	/**
	 * This thread reads all the existing mappings on the NAT and if it finds
	 * a mapping which appears to be created by us but points to a different
	 * address (i.e. is stale) it removes the mapping.
	 * 
	 * It can take several minutes to finish, depending on how many mappings there are.  
	 */
	private class StaleCleaner implements Runnable {
		public void run() {
			Set mappings = new HashSet();
			Action getGeneric;
			synchronized(UPnPManager.this) {
				getGeneric = _service.getAction("GetGenericPortMappingEntry");
			}
			
			// get all the mappings
			try {
				for (int i=0;;i++) {
					getGeneric.setArgumentValue("NewPortMappingIndex",i);
					
					if (!getGeneric.postControlAction())
						break;
					
					mappings.add(new Mapping(
							getGeneric.getArgumentValue("NewRemoteHost"),
							getGeneric.getArgumentValue("NewExternalPort"),
							getGeneric.getArgumentValue("NewInternalClient"),
							getGeneric.getArgumentValue("NewInternalPort"),
							getGeneric.getArgumentValue("NewProtocol"),
							getGeneric.getArgumentValue("NewPortMappingDescription")));
				}
			}catch(NumberFormatException bad) {
				//router broke.. can't do anything.
				return;
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("Stale cleaner found "+mappings.size()+ " total mappings");
			
			// iterate and clean up
			for (Iterator iter = mappings.iterator();iter.hasNext();) {
				Mapping current = (Mapping)iter.next();
				
				// does it have our description?
				if (current._description.equals(TCP_PREFIX+GUID_SUFFIX) ||
						current._description.equals(UDP_PREFIX+GUID_SUFFIX)) {
					
					// is it not the same as the mappings we created this session?
					synchronized(this) {
						
						if (_tcp != null && _udp != null &&
								current._externalPort == _tcp._externalPort &&
								current._internalAddress.equals(_tcp._internalAddress) &&
								current._internalPort == _tcp._internalPort)
							continue;
					}
					
					// remove it.
					if (LOG.isDebugEnabled())
						LOG.debug("mapping "+current+" appears to be stale");
					removeMapping(current);
				}
			}
		}
	}
}
