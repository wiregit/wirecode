package org.limewire.store.server;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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
 * This is a composite element of the demo servers. They are each a remote or local server, but this
 * takes care of the debugging stuff and sending messages.
 */
final class LocalServerDelegate {

  private final DispatcherSupport dispatcher;
  private final int port;
  private final DispatcherSupport.OpensSocket openner;
  private final String host;

  public LocalServerDelegate(final DispatcherSupport server, String host, final int port, final DispatcherSupport.OpensSocket openner) {
    this.dispatcher = server;
    this.host = host;
    this.port = port;
    this.openner = openner;
  }
  
  public LocalServerDelegate(final DispatcherSupport server, final String host, final int port) {
      this(server, host, port, new URLSocketOpenner());
  }

  /** We want to tile the send windows. */
  private static int numSendWindows = 0;

  void showSendWindow() {
    JFrame f = new JFrame(dispatcher.toString());
    f.getContentPane().setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));
    f.getContentPane().add(new JLabel(dispatcher.toString()));
    JPanel p = new JPanel();
    
    /** A simple panel. */
    class P extends JPanel {
      private final JTextField l = new JTextField(10);
      private final JTextField r = new JTextField(10);

      P() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(l);
        add(r);
      }

      String getKey() {
        return l.getText();
      }

      String getVal() {
        return r.getText();
      }

      void clear() {
        l.setText("");
        r.setText("");
      }
    }
    
    JPanel midPanel = new JPanel();
    midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
    final P[] ps = new P[10];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = new P();
      midPanel.add(ps[i]);
    }
    final JTextField t = new JTextField(30);
    final JButton send = new JButton(new AbstractAction("Send") {
      public void actionPerformed(final ActionEvent e) {
        String s = t.getText();
        Map<String, String> args = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
          P p = ps[i];
          String key = p.getKey();
          String val = p.getVal();
          if (Util.isEmpty(key)) continue;
          if (Util.isEmpty(val)) continue;
          args.put(key, val);
        }
        String res = dispatcher.sendMsgToRemoteServer(s, args);
        dispatcher.note("have response:\n" + res);
        t.setText("");
        for (int i = 0; i < ps.length; i++) {
          ps[i].clear();
        }
      }
    });
    p.add(t);
    p.add(send);
    f.getContentPane().add(p);
    f.getContentPane().add(midPanel);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Toolkit tk = Toolkit.getDefaultToolkit();
    int x = tk.getScreenSize().width * 3 / 5;
    int y = numSendWindows == 1 ? tk.getScreenSize().height / 2 : 0;

    f.setLocation(x, y);

    f.pack();
    f.setVisible(true);

    numSendWindows++;
  }

  public String sendMsg(final String msg, final Map<String, String> args) {
    try {
      dispatcher.note("sending {0} : {1}", msg, args);
      Socket sock = openner.open(host, port);
      dispatcher.note("have socket: " + sock);
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
      if (params != null)
        message += params;
      dispatcher.note("message: " + message);
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
