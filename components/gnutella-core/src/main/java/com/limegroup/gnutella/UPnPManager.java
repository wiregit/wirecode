package com.limegroup.gnutella;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
 * distinguish which mappings were made by us, we put our client GUID in the description 
 * field.
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
	
	private Map /*String description->Mapping */ getExistingMappings() {
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
		public final String _externalAddress, _externalPort;
		public final String _internalAddress, _internalPort;
		public final String _protocol,_description;
		
		public Mapping(String externalAddress,String externalPort,
				String internalAddress, String internalPort,
				String protocol, String description) {
			_externalAddress=externalAddress;
			_externalPort=externalPort;
			_internalAddress=internalAddress;
			_internalPort=internalPort;
			_protocol=protocol;
			_description=description;
		}
		
	}
	public static void main(String[] args) throws Exception {
		final UPnPManager cp = new UPnPManager();
		Thread.sleep(4000);
		LOG.debug("start");
		LOG.debug("found "+cp.getNATAddress());
		Map m = cp.getExistingMappings();
		System.out.println(m);
		synchronized(cp)  {
			cp.wait();
		}
	}
	
}
