package com.limegroup.gnutella.bootstrap;

import java.io.*;
import java.net.*;

/**
 * Simulates a GWebCache HTTP server.  Listens on a port, accepts a single
 * connection, records request and writes result.
 */
public class TestBootstrapServer {
    ServerSocket _ss;
    Socket _socket;

    String _request;
    String _response;
    String _responseData="";

    /** Starts a single bootstrap server listening on the given port
     *  Call setResponse() to set abnormal HTTP responses.
     *  Call setResponseData() to modify the response data. 
     *  @exception IOException this couldn't list on the port */    
    public TestBootstrapServer(int port) throws IOException { 
        setResponse("HTTP/1.0 200 OK");
        _ss=new ServerSocket(port);
        Thread runner=new RunnerThread();
        runner.start();
    }
    
    /** Sets what this should send for any HTTP response, without any newline
     *  characters.  Default value: "HTTP/1.0 200 OK".*/
    public void setResponse(String httpResponse) {
        this._response=httpResponse+"\r\n\r\n";  //add EOL and blank line
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

        try {
            if (_socket!=null)
                _socket.close();
        } catch (IOException e) {
        }
    }

    private class RunnerThread extends Thread {
        public void run() {
            try {
                run2();
            } catch (IOException e) { }
        }
        
        public void run2() throws IOException {
            _socket=_ss.accept();
            BufferedReader in=
                new BufferedReader(
                    new InputStreamReader(_socket.getInputStream()));
            _request=in.readLine();
            OutputStream out=_socket.getOutputStream();
            out.write(_response.getBytes());
            out.write(_responseData.getBytes());
            out.flush();
            out.close();
        }
    }
}

