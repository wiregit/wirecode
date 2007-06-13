package org.limewire.store.server;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.limewire.service.ErrorService;

/**
 * This is a composite element of the demo servers. They are each a remote or
 * local server, but this takes care of the debugging stuff and sending
 * messages.
 */
final class LocalServerDelegate implements SendsMessagesToServer {

    private final int port;
    private final DispatcherSupport.OpensSocket openner;
    private final String host;

    public LocalServerDelegate(String host, int port, DispatcherSupport.OpensSocket openner) {
        this.host = host;
        this.port = port;
        this.openner = openner;
    }

    public LocalServerDelegate(final String host, final int port) {
        this(host, port, new URLSocketOpenner());
    }

    public String sendMsgToRemoteServer(final String msg, final Map<String, String> args) {
        try {
            int tmpPort = port;
            Socket tmpSock = null;
            for (; tmpPort < port+10; tmpPort++) {
                tmpSock = openner.open(host, tmpPort);
                if (tmpSock != null) break;
            }
            final Socket sock = tmpSock;
            System.out.println("sending on " + tmpPort + ":" + sock);
            System.out.println(" --- " + msg + ":" + args + " ---");
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock
                    .getOutputStream()));
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
            if (params != null)
                message += params;
            wr.write("GET /" + message + " HTTP/1.1\n\r\n\r");
            wr.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String ln;
            while ((ln = in.readLine()) != null) {
                sb.append(ln);
                sb.append("\n");
            }
            String res = sb.toString();
            in.close();
            wr.close();
            sock.close();
            return res;
        } catch (IOException e) {
            ErrorService.error(e, host + ":" + port);
        }
        return null;
    }

}
