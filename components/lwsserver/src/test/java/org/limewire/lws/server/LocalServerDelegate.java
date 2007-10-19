package org.limewire.lws.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;

import org.limewire.service.ErrorService;

/**
 * This is a composite element of the demo servers. They are each a remote or
 * local server, but this takes care of the debugging stuff and sending
 * messages.
 */
final class LocalServerDelegate implements SenderOfMessagesToServer {

    private final int port;
    private final LWSDispatcherSupport.OpensSocket openner;
    private final String host;

    public LocalServerDelegate(String host, int port, LWSDispatcherSupport.OpensSocket openner) {
        this.host = host;
        this.port = port;
        this.openner = openner;
    }

    public LocalServerDelegate(final String host, final int port) {
        this(host, port, new URLSocketOpenner());
    }

    public void sendMessageToServer(final String msg, final Map<String, String> args, StringCallback cb) {
        try {
            int tmpPort = port;
            Socket tmpSock = null;
            for (; tmpPort < port+10; tmpPort++) {
                tmpSock = openner.open(host, tmpPort);
                if (tmpSock != null) break;
            }
            final Socket sock = tmpSock;
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            String message = msg;
            String params = null;
            for (String key : args.keySet()) {
                if (params == null) {
                    params = "?";
                } else {
                    params += "&";
                }
                params += key + "=" + args.get(key);
            }
            if (params != null) message += params;
            wr.write("GET /" + message + " HTTP/1.1\n\r\n\r");
            wr.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            StringBuffer sb = new StringBuffer();
            //
            // We will have a similar response and can only give
            // StringCallbacks the actual response
            //
            //   HTTP/1.1 200 OK
            //   Last-modified: Fri, 19-10-2007 14:38:54 GMT
            //   Server: null
            //   Date: Fri Oct 19 14:38:54 EDT 2007
            //   Content-length: 2
            //   
            //   ok
            //
            // So just extract the 'ok'
            //
            String ln;
            boolean reading = false;
            while ((ln = in.readLine()) != null) {
                if (reading) {
                    sb.append(ln);
                    sb.append("\n");                    
                } else if (ln.startsWith("Content-length")) {
                    in.readLine();
                    reading = true;
                }
            }
            String res = sb.toString();
            in.close();
            wr.close();
            sock.close();
            cb.process(res);
        } catch (IOException e) {
            ErrorService.error(e, host + ":" + port);
        }
    }

}    
