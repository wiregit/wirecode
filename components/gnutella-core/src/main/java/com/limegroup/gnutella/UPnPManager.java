package com.limegroup.gnutella;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.device.DeviceChangeListener;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;
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
 *  - External address (can be wildcard)
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
 */
public class UPnPManager extends ControlPoint implements DeviceChangeListener{
	
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
	static {
		if (!CommonUtils.isJava14OrLater())
			LOG.warn("loading UPnPManager on jvm that doesn't support it!");
	}
	
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
	
	/**
	 * existing port mappings. LOCKING: this
	 */
	private Map _mappings;
	
	/** whether we are currently performing any UPnP operations */
	private volatile boolean _running;
	
	private UPnPManager() {
		super();
		addDeviceChangeListener(this);
		
		start();
		_running=true;
	}
	
	/**
	 * @return whether we are behind an UPnP-enabled NAT/router
	 */
	public synchronized boolean NATPresent() {
		return _router != null && _service != null;
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
		} else
			LOG.debug("found service");
		
		// get the existing mappings
		try {
			Map m = getExistingMappings();
			synchronized(this) {
				_mappings = m;
			}
			if (LOG.isDebugEnabled())
				LOG.debug("mappings on startup: "+_mappings);
		}catch(NumberFormatException bad) {
			//the router returned invalid port values, it must be malfunctioning.
			LOG.debug("removing broken router",bad);
			_router = null;
			_service = null;
			return;
		}
		
		// delete any previous mappings for this client if they exist
		Mapping tcp = (Mapping)_mappings.remove(TCP_PREFIX + GUID_SUFFIX);
		Mapping udp = (Mapping)_mappings.remove(UDP_PREFIX + GUID_SUFFIX);
		if (tcp != null)
			removeMapping(tcp);
		if (udp != null)
			removeMapping(udp);
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
	 * @param port the port that we wish to forward
	 * @return whether the port is available or not
	 */
	public synchronized boolean portAvailable(int port) {
		// if no nat, its always available
		if (!NATPresent())
			return true;
		
		for (Iterator iter = _mappings.values().iterator();iter.hasNext();) {
			Mapping m = (Mapping)iter.next();
			if (m._externalPort == port)
				return false;
		}
		return true;
	}
	
	/**
	 * adds a mapping on the router to the specified port
	 * @param port
	 */
	public void mapPort(int port) {
		if (LOG.isDebugEnabled())
			LOG.debug("existing mappings: "+_mappings);
		
		String localAddress = NetworkUtils.ip2string(
				RouterService.getAcceptor().getAddress(false));
		
		Mapping tcp = new Mapping("",
				""+port,
				localAddress,
				""+port,
				"TCP",
				TCP_PREFIX + GUID_SUFFIX);
		Mapping udp = new Mapping("",
				""+port,
				localAddress,
				""+port,
				"UDP",
				UDP_PREFIX + GUID_SUFFIX);
		
		// add udp first in case it gets overwritten.
		if (addMapping(udp))
			_mappings.put(udp._description,udp);
		if (addMapping(tcp))
			_mappings.put(tcp._description,tcp);
	}
	
	private Map /*String description->Mapping */ getExistingMappings() 
		throws NumberFormatException {
		Map ret = new HashMap();
		Action check;
		synchronized(this){
			check = _service.getAction("GetGenericPortMappingEntry");
		}
		for (int i=0;;i++) {
			
			check.setArgumentValue("NewPortMappingIndex",i);
			if (!check.postControlAction()) 
				break;
			
			ArgumentList l = check.getOutputArgumentList();
			Mapping m = new Mapping(
					l.getArgument("NewRemoteHost").getValue(),
					l.getArgument("NewExternalPort").getValue(),
					l.getArgument("NewInternalClient").getValue(),
					l.getArgument("NewInternalPort").getValue(),
					l.getArgument("NewProtocol").getValue(),
					l.getArgument("NewPortMappingDescription").getValue());
			
			ret.put(m._description,m);
			
		}
		return ret;
	}
	
	/**
	 * @param m Port mapping to send to the NAT
	 * @return whether it worked or not
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
	 * halts any UPnP operations.  Not called stop() because there 
	 * exists a stop() in the parent class which is (ab)used frequently
	 */
	public void halt() {
		if (stop()) 
			_running=false;
	}
	
	public void finalize() {
		if (_running) {
			LOG.warn("finalizing a running UPnPManager!");
			halt();
			super.finalize();
		}
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
	
	public static void main(String[] args) throws Exception {
		final UPnPManager cp = new UPnPManager();
		Thread.sleep(4000);
		LOG.debug("start");
		LOG.debug("found "+cp.getNATAddress());
		Map m = cp.getExistingMappings();
		for (Iterator iter = m.values().iterator();iter.hasNext();) 
			cp.removeMapping((Mapping)iter.next());
		synchronized(cp)  {
			cp.wait();
		}
	}
	
}
