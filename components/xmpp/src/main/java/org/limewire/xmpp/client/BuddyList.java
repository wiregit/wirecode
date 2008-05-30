package org.limewire.xmpp.client;

import java.awt.Insets;
import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.file.FileContentHandler;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.limewire.xmpp.client.commands.LibraryCommand;

public class BuddyList {
    private JPanel myPanel;
    private JTree tree1;
    private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final XMPPConnection connection;

    public static void create(XMPPConnection connection) {
        JFrame frame = new JFrame("BuddyList");
        frame.setContentPane(new BuddyList(connection).myPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public BuddyList(final XMPPConnection connection) {
        this.connection = connection;
        $$$setupUI$$$();
        new Thread(new Runnable() {
            public void run() {
                try {
                    getRoster(connection);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getRoster(XMPPConnection connection) throws Exception {
        Thread.sleep(5 * 1000);

        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);

        Roster roster = connection.getRoster();
        //roster.addRosterListener(new RosterListenerImpl(connection));

        HashSet<String> limewireClients = new HashSet<String>();
        for (RosterEntry rosterEntry : roster.getEntries()) {
//            root.add(new DefaultMutableTreeNode(rosterEntry.getName()));
//            tree1.updateUI();
            Iterator<Presence> presences = roster.getPresences(rosterEntry.getUser());
            while (presences.hasNext()) {
                Presence presence = presences.next();
                if (presence.getType() == Presence.Type.available) {
                    System.out.println("found: " + rosterEntry.getName());
                    root.add(new DefaultMutableTreeNode(rosterEntry.getName()));
                    tree1.updateUI();
                    try {
                        if (serviceDiscoveryManager.discoverInfo(presence.getFrom()).containsFeature("http://www.limewire.org/")) {
                            limewireClients.add(presence.getFrom());
                            System.out.println("found lw client: " + presence.getFrom());
                        }
                    } catch (XMPPException exception) {
                        //exception.printStackTrace();
                    }
                }
            }
        }

        LibraryCommand libraryCommand = new LibraryCommand(connection, limewireClients);
        libraryCommand.execute(null);

        //jingleIN();
        jingleOUT("tim.julien@gmail.com/limewire56AF93C1");
    }

    private void createUIComponents() {
        tree1 = new JTree(root);
        tree1.setToggleClickCount(1);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        myPanel = new JPanel();
        myPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        myPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(tree1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myPanel;
    }

    private class RosterListenerImpl implements RosterListener {
        private final XMPPConnection connection;

        public RosterListenerImpl(XMPPConnection connection) {
            this.connection = connection;
        }

        public void entriesAdded(Collection<String> strings) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void entriesUpdated(Collection<String> strings) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void entriesDeleted(Collection<String> strings) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void presenceChanged(Presence presence) {
            Roster roster = connection.getRoster();
            if (presence.getType() == Presence.Type.available) {
                RosterEntry entry = roster.getEntry(presence.getFrom());
                String name = entry.getName();
                if (name == null || name.trim().length() == 0) {
                    name = presence.getFrom();
                }
                root.add(new DefaultMutableTreeNode(name));
                tree1.updateUI();
                try {
                    if (ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(presence.getFrom()).containsFeature("http://www.limewire.org/")) {
                        System.out.println("found lw client: " + presence.getFrom());
                    }
                } catch (XMPPException exception) {
                    //exception.printStackTrace();
                }
            } else if (presence.getType() == Presence.Type.unavailable) {
                Enumeration children = root.children();
                while (children.hasMoreElements()) {
                    RosterEntry entry = roster.getEntry(presence.getFrom());
                    String name = entry.getName();
                    if (name == null || name.trim().length() == 0) {
                        name = presence.getFrom();
                    }
                    DefaultMutableTreeNode mutableTreeNode = (DefaultMutableTreeNode) children.nextElement();
                    String treeNodeName = (String) mutableTreeNode.getUserObject();
                    if (name.equals(treeNodeName)) {
                        root.remove(mutableTreeNode);
                    }
                }
            }
        }
    }

    private void jingleIN() {
        //"jstun.javawi.de", 3478
        JingleManager manager = new JingleManager(connection);
        manager.addJingleSessionRequestListener(new JingleSessionRequestListener() {
            public void sessionRequested(JingleSessionRequest request) {

                try {
                    // Accept the call
                    IncomingJingleSession session = request.accept();
                    // Start the call
                    session.start();
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void jingleOUT(String to) throws InterruptedException {
        JingleManager manager = new JingleManager(connection);

        try {
            FileContentHandler fileContentHandler = new FileContentHandler(new File("C:\\ChocolateEggThings\\IMG_0089.JPG"), true);
            OutgoingJingleSession out = manager.createOutgoingJingleSession(to, fileContentHandler);

            out.start();

            while (out.getJingleMediaSession() == null) {
                Thread.sleep(500);
            }

            //out.terminate();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }
}
