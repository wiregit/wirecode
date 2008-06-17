package org.limewire.xmpp.client;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.jivesoftware.smack.util.StringUtils;
import org.limewire.inject.AbstractModule;
import org.limewire.lifecycle.ServiceTestCase;

import com.google.inject.Module;

public class XMPPServiceTest extends ServiceTestCase {
    protected XMPPServiceTest.RosterListenerImpl rosterListener;

    public XMPPServiceTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();        
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected List<Module> getServiceModules() {
        Module m = new AbstractModule() {
            protected void configure() {
                bind(XMPPServiceConfiguration.class).toInstance(new XMPPServiceConfigurationImpl("limebuddy2@gmail.com", 
                        "limebuddy234", "talk.google.com", 5222, "gmail.com"));
                rosterListener = new RosterListenerImpl();
                bind(RosterListener.class).toInstance(rosterListener);                
                bind(LibrarySource.class).toInstance(new LibrarySourceImpl());                 
                bind(IncomingFileAcceptor.class).toInstance(new IncomingFileAcceptorImpl());
            }
        };
        return Arrays.asList(new LimeWireXMPPModule(), m);
    }

    private org.limewire.xmpp.client.File toFile(File f) {
        return new org.limewire.xmpp.client.File(new Random().nextInt() + "", f.getName());
    }

    public void test() throws InterruptedException {
        RosterListenerImpl rosterListener2 = new RosterListenerImpl();
        //XMPPService service2 = new XMPPService(new XMPPServiceConfigurationImpl("limebuddy2@gmail.com", 
        //                "limebuddy234", "talk.google.com", 5222, "gmail.com"),
        //       rosterListener2, new LibrarySourceImpl(), new IncomingFileAcceptorImpl());
        //service2.initialize();
        //service2.start();
        XMPPService service = injector.getInstance(XMPPService.class);
        
        Thread.sleep(3 * 1000);          
        
//        assertEquals(1, rosterListener2.roster.size());
//        assertEquals(1, rosterListener2.roster.get("limebuddy1@gmail.com").size());
//        assertTrue(rosterListener2.roster.get("limebuddy1@gmail.com").get(0) instanceof LimePresence);
//        assertEquals(Presence.Type.available, rosterListener2.roster.get("limebuddy1@gmail.com").get(0).getType());
//        
//        assertEquals(1, rosterListener.roster.size());
//        assertEquals(1, rosterListener.roster.get("limebuddy2@gmail.com").size());
//        assertTrue(rosterListener.roster.get("limebuddy2@gmail.com").get(0) instanceof LimePresence);
//        assertEquals(Presence.Type.available, rosterListener.roster.get("limebuddy2@gmail.com").get(0).getType());
        
        Thread.sleep(5* 60 * 1000);            
    }
    
    class XMPPServiceConfigurationImpl implements XMPPServiceConfiguration {
        String userName;
        String pw;
        String host;
        int port;
        String serviceName;
        
        XMPPServiceConfigurationImpl(String userName, String pw, String host, int port, String serviceName) {
            this.userName = userName;
            this.pw = pw;
            this.host = host;
            this.port = port;
            this.serviceName = serviceName;
        }
        
        public boolean isDebugEnabled() {
            return true;
        }

        public String getUsername() {
            return userName;
        }

        public String getPassword() {
            return pw;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getServiceName() {
            return serviceName;
        }    
    }
    
    class RosterListenerImpl implements RosterListener {
        HashMap<String, ArrayList<Presence>> roster = new HashMap<String, ArrayList<Presence>>();
        
        public void userAdded(User user) {
            System.out.println("user added: " +user.getId());
            final String name = user.getName();
            user.addPresenceListener(new PresenceListener() {
                public void presenceChanged(Presence presence) {
                    String id = StringUtils.parseBareAddress(presence.getJID());
                    if(presence.getType().equals(Presence.Type.available)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<Presence>());
                        }
                        roster.get(id).add(presence);
                        if(presence instanceof LimePresence) {
                            System.out.println("lime user " + presence.getJID() + " (" + name + ") available");
                            ((LimePresence)presence).setLibraryListener(new LibraryListener() {
                                 public void fileAdded(org.limewire.xmpp.client.File f){
                                    System.out.println(f.getName() + ": " + f.getId());
                                }
                            });
                        } else {                            
                            System.out.println("user " + presence.getJID() + " (" + name + ") available");
                        }
                    } else if(presence.getType().equals(Presence.Type.unavailable)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<Presence>());
                        }
                        remove(id, presence);
                        if(presence instanceof LimePresence) {
                            System.out.println("lime user " + presence.getJID() + " (" + name + ") unavailable");
                        } else {
                            
                            System.out.println("user " + presence.getJID() + " (" + name + ") unavailable");
                        }
                    } else {
                        System.out.println("user presence changed: " + presence.getType());
                    }
                }
            });
        }

        private void remove(String id, Presence p) {
            for(Presence presence : roster.get(id)) {
                if(presence.getJID().equals(p.getJID())) {
                    roster.remove(presence);
                }
            }
        }

        public void userUpdated(User user) {
            System.out.println("user updated: " + user.getId());
        }

        public void userDeleted(String id) {
            System.out.println("user deleted: " +id);
        }    
    }
    
    class LibrarySourceImpl implements LibrarySource {
        public Iterator<org.limewire.xmpp.client.File> getFiles() {
            ArrayList<org.limewire.xmpp.client.File> files = new ArrayList<org.limewire.xmpp.client.File>();
            java.io.File dir = new java.io.File("C://");
            File [] toAdd = dir.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            });
            for(File f : toAdd) {
                files.add(toFile(f));
            }
            return files.iterator();
        }    
    }
    
    class IncomingFileAcceptorImpl implements IncomingFileAcceptor {
        public boolean accept(org.limewire.xmpp.client.File f) {
            return true;
        }    
    }
}
