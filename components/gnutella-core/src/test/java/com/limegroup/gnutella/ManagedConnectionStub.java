package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;

import java.net.*;

/** 
 * A stubbed-out ManagedConnection that does nothing.  Useful for testing, since
 * ManagedConnection has no public-access constructors.  ManagedConnectionStub
 * is in this package instead of com.limegroup.gnutella.stubs because it
 * requires package-access to ManagedConnection.
 * 
 * Added ability to report fake remote ip:port.
 */
public class ManagedConnectionStub extends ManagedConnection {
	
	String _host;
	int _port;
	InetAddress _addr;
	boolean _inited;
	
    public ManagedConnectionStub() {
        super("1.2.3.4", 6346);
		
		try {
            PrivilegedAccessor.setValue(this, "_manager", new ConnectionManagerStub());
        } catch(Exception e) {
            ErrorService.error(e);
        }		
		//_router = new MessageRouterStub(); 
        //_manager = new ConnectionManagerStub();
    }
    
    /**
     * a constructor which allows the stub to report different values for
     * its host and port.
     */
    public ManagedConnectionStub(String host, int port) {
    	super(host,port);
    	_host=host;
    	_port = port;
    	
    	try{
    		_addr = InetAddress.getByName(host);
    	}catch(Exception tough) {}
    }

    /**
     * override the getters to report the fake values
     */
    public InetAddress getInetAddress() {return _addr;}
    
    public void initialize() {
    	_inited=true;
    }
    
    public boolean isInitialized() {return _inited;}
}
