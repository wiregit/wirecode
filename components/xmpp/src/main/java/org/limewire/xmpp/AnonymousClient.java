package org.limewire.xmpp;

import java.util.Arrays;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;

public class AnonymousClient {
    public static void main(String [] args) throws XMPPException, ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        XMPPConnection.DEBUG_ENABLED = true;
        
        XMPPConnection conn = new XMPPConnection("tjulien-pc");        
        conn.connect();
        conn.loginAnonymously();

        NetworkMode mode = new NetworkMode(NetworkMode.Mode.LEAF);
        mode.setType(IQ.Type.SET);
        conn.sendPacket(mode);
        
        Ping ping = new Ping();
        ping.setType(IQ.Type.GET);
        conn.sendPacket(ping);

        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.add(new DownloadCommand(conn));
        dispatcher.add(new InitiateChatCommand(conn));
        dispatcher.add(new RosterCommand(conn));
        dispatcher.add(new SearchCommand(conn));
        dispatcher.add(new SendMessageCommand(conn));
        Thread t = new Thread(dispatcher);
        t.setDaemon(false);
        t.start();
    }
}
