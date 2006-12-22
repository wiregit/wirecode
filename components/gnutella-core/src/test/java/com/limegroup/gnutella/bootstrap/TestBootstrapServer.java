package com.limegroup.gnutella.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;


/**
 * Simulates a GWebCache HTTP server.  Listens on a port, accepts a single
 * connection, records request and writes result.
 */
public class TestBootstrapServer {
    
    
    private static final Log LOG =
        LogFactory.getLog(TestBootstrapServer.class);
    
    ServerSocket _ss;
    List _sockets = new LinkedList();

    volatile String _request;
    volatile String _response;
    volatile String _responseData="";
    
    boolean _allowConnectionReuse = false;
    
    int _numConnections = 0;
    int _numRequests = 0;

    /** Starts a single bootstrap server listening on the given port
     *  Call setResponse() to set abnormal HTTP responses.
     *  Call setResponseData() to modify the response data. 
     *  @exception IOException this couldn't list on the port */    
    public TestBootstrapServer(int port) throws IOException { 
        setResponse("HTTP/1.0 200 OK");
        _ss=new ServerSocket();
        _ss.setReuseAddress(true);
        _ss.bind(new InetSocketAddress(port));
        Thread runner=new RunnerThread();
        runner.setName("TBS, Port: " + port);
        runner.start();
    }
    
    /**
     * Returns the number of connection attempts this simple server received.
     */
    public int getConnectionAttempts() {
        return _numConnections;
    }
    
    /**
     * Returns the number of requests this simple server recieved.
     */
    public int getRequestAttempts() {
        return _numRequests;
    }
    
    /**
     * Sets whether or not this simple server should allow the connection
     * to be reused for multiple requests.
     */
    public void setAllowConnectionReuse(boolean reuse) {
        _allowConnectionReuse = reuse;
    }
    
    /** Sets what this should send for any HTTP response, without any newline
     *  characters.  Default value: "HTTP/1.0 200 OK".*/
    public void setResponse(String httpResponse) {
        this._response=httpResponse;  //add EOL and blank line
    }

    /** Sets the data this should send for any HTTP response.  Default value:
     *  "".  Example value: "18.239.0.144:6346\r\n1.2.3.4\r\n\r\n.*/
    public void setResponseData(String data) {
        this._responseData=data;
    }
    
    /** Returns the request line received, or null if none. */
    public String getRequest() {
        return _request;
    }

    /** Frees any resources. */
    public void shutdown() {
        try {
            _ss.close();
        } catch (IOException e) {
        }

        for(Iterator i = _sockets.iterator(); i.hasNext(); ) {
            try {
                Socket s = (Socket)i.next();
                if(s != null )
                    s.close();
            } catch (IOException e) {}
        }
    }

    private class RunnerThread extends Thread {
        public void run() {
            try {
                run2();
            } catch (IOException e) {
            } catch(Throwable e) {
                ErrorService.error(e);
            }
        }
        
        public void run2() throws IOException {
            while(true) {
                LOG.debug("waiting to accept new connection");
                Socket s = _ss.accept();
                LOG.debug("accepted new connection");
                _numConnections++;
                _sockets.add(s);
                BufferedReader in=
                    new BufferedReader(
                        new InputStreamReader(s.getInputStream()));
                OutputStream out = s.getOutputStream();
                while(true) {
                    LOG.debug("reading new request");
                    _request=in.readLine();
                    LOG.debug("read: " + _request);
                    if(_request == null)
                        break;
                    
                    // gobble up headers.
                    String restOfLine = _request;
                    while(!restOfLine.equals("")) {
                        restOfLine = in.readLine();
                        if(restOfLine == null)
                            break;
                        LOG.debug("continued read: " + restOfLine);
                    }
                    LOG.debug("finished reading request.");
                    _numRequests++;
                    out.write((_response + "\r\n\r\n").getBytes());
                    out.write(_responseData.getBytes());
                    out.flush();
                    if(!_allowConnectionReuse)
                        break;
                }
                out.close();
                if(!_allowConnectionReuse)
                    break;
            }
        }
    }
}

