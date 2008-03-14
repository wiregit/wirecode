package org.limewire.xmpp.client;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.xmpp.client.commands.*;

public class AnonymousClient {
    public static void main(String [] args) throws XMPPException, ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        XMPPConnection.DEBUG_ENABLED = true;
        
        // TODO read connection host info from config OR cmd-line OR UHCs
        XMPPConnection conn = new XMPPConnection("tjulien-pc");        
        conn.connect();
        conn.loginAnonymously();

        NetworkMode mode = new NetworkMode(NetworkMode.Mode.LEAF);
        mode.setType(IQ.Type.SET);
        conn.sendPacket(mode);
        
        Ping ping = new Ping();
        ping.setType(IQ.Type.GET);
        conn.sendPacket(ping);
        
        // TODO listen for pongs

        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.add(new DownloadCommand(conn));
        dispatcher.add(new InitiateChatCommand(conn));
        dispatcher.add(new RosterCommand(conn));
        dispatcher.add(new SearchCommand(conn, null));
        dispatcher.add(new SendMessageCommand(conn));
        Thread t = new Thread(dispatcher);
        t.setDaemon(false);
        t.start();
    }
}
