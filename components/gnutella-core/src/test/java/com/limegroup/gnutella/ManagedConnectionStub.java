package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.messages.*;

import java.net.*;

/** 
 * A stubbed-out ManagedConnection that does nothing.  Useful for testing, since
 * ManagedConnection has no public-access constructors.  ManagedConnectionStub
 * is in this package instead of com.limegroup.gnutella.stubs because it
 * requires package-access to ManagedConnection.
 * 
 * Added ability to report fake remote ip:port and to wait for specific types of
 * messages to be sent.
 */
public class ManagedConnectionStub extends ManagedConnection {
	
	String _host;
	int _port;
	InetAddress _addr;
	boolean _inited;
	
	/**
	 * monitor to lock when waiting for messages.  
	 * Protects _expectedMessage and _lastSent fields.
	 */
	Object _waitLock;
	
	/**
	 * the type of the message we are expecting to receive
	 * LOCKING: _waitLock
	 */
	String _expectedMessage;
	
	/**
	 * the last message we have received.
	 * LOCKING: _waitLock
	 */
	private Message _lastSent;
	
    public ManagedConnectionStub() {
        super("1.2.3.4", 6346);
		_host="1.2.3.4";
		_port=6346;
		
		try {
            PrivilegedAccessor.setValue(this, "_manager", new ConnectionManagerStub());
        } catch(Exception e) {
            ErrorService.error(e);
        }		
		//_router = new MessageRouterStub(); 
        //_manager = new ConnectionManagerStub();
        _waitLock = new Object();
        
        try{
    		_addr = InetAddress.getByName("1.2.3.4");
    	}catch(Exception tough) {
    		tough.printStackTrace();
    	}
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
    	}catch(Exception tough) {
    		tough.printStackTrace();
    	}
    	_waitLock = new Object();
    }

    /**
     * override the getters to report the fake values
     */
    public InetAddress getInetAddress() {return _addr;}
    
    public void initialize() {
    	_inited=true;
    }
    
    public boolean isInitialized() {return _inited;}
    
    /**
     * waits for another thread to call send() on this object.
     */
    public void waitForSend(int timeout) {
    	 waitForSend("Message",timeout);
    }
    
    /**
     * waits for another method to call send() with a specified message type
     * @param messageType name of the class of the message we're waiting to receive.
     */
    public void waitForSend(String messageType, int timeout) {
    	synchronized(_waitLock) {
    		_expectedMessage=messageType;
    		try{
    			_waitLock.wait(timeout);
    		}catch(InterruptedException iex) {}
    	}
    }
    
    /**
     * overriden to notify the listener.
     */
    public void send(Message m) {
    	synchronized(_waitLock) {
    		if (_expectedMessage ==null)
    			_expectedMessage = "Message";
    		if (_expectedMessage.equals("Message"))
    			_waitLock.notifyAll();
    		else if (m.getClass().equals(_expectedMessage))
    			_waitLock.notifyAll();
    		_lastSent=m;
    	}
    }
    
    public Message getLastSent() {
    	return _lastSent;
    }
    
    public void setLastSent(Message m) {
    	synchronized(_waitLock) {
    		_lastSent =m;
    	}
    }
}
