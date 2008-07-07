package org.limewire.xmpp.client;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.inject.AbstractModule;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.ServiceTestCase;
import org.limewire.xmpp.client.impl.XMPPException;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileTransferProgressListener;
import org.limewire.xmpp.client.service.IncomingChatListener;
import org.limewire.xmpp.client.service.IncomingFileAcceptor;
import org.limewire.xmpp.client.service.LibraryListener;
import org.limewire.xmpp.client.service.LibraryProvider;
import org.limewire.xmpp.client.service.LimePresence;
import org.limewire.xmpp.client.service.MessageReader;
import org.limewire.xmpp.client.service.MessageWriter;
import org.limewire.xmpp.client.service.Presence;
import org.limewire.xmpp.client.service.PresenceListener;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.User;
import org.limewire.xmpp.client.service.XMPPConnection;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.XMPPErrorListener;
import org.limewire.xmpp.client.service.XMPPService;

import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public class XMPPServiceTest extends ServiceTestCase {
    protected RosterListenerImpl rosterListener;
    protected LibraryProviderImpl libraryProvider;
    protected RosterListenerImpl rosterListener2;

    public XMPPServiceTest(String name) {
        super(name);
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    protected void setUp() throws Exception {
        super.setUp();  
        Thread.sleep(3 * 1000); // allow login, roster, presence, library messages to be
                                // sent, received   
                                // TODO wait()/notify()
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected List<Module> getServiceModules() {
        rosterListener = new RosterListenerImpl();
        rosterListener2 = new RosterListenerImpl();
        XMPPConnectionConfiguration configuration = new XMPPConnectionConfigurationImpl("limebuddy1@gmail.com", 
                "limebuddy123", "talk.google.com", 5222, "gmail.com", rosterListener);
        XMPPConnectionConfiguration configuration2 = new XMPPConnectionConfigurationImpl("limebuddy2@gmail.com", 
                "limebuddy234", "talk.google.com", 5222, "gmail.com", rosterListener2);
        try {
            libraryProvider = new LibraryProviderImpl(createMockLibrary());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Module m = new LimeWireXMPPModule(new XMPPConnectionConfigurationListProvider(configuration, configuration2), libraryProvider,
                new ProgressListener(), new IncomingFileAcceptorImpl());
        return Arrays.asList(m);
    }

    private FileMetaDataImpl toFile(File f) {
        return new FileMetaDataImpl(new Random().nextInt() + "", f.getName());
    }

    public void testStart() throws InterruptedException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("limebuddy2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(1, rosterListener.roster.get("limebuddy2@gmail.com").size());

        assertEquals(1, rosterListener2.roster.size());
        assertEquals("limebuddy1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(1, rosterListener2.roster.get("limebuddy1@gmail.com").size());
    }
    
    public void testUserLogout() throws InterruptedException {
        XMPPService service = injector.getInstance(XMPPService.class);
        List<XMPPConnection> connections = service.getConnections();
        assertTrue(connections.get(0).isLoggedIn());
        connections.get(0).logout();
        assertFalse(connections.get(0).isLoggedIn());
    }
    
    public void testDetectLimePresences() throws InterruptedException, IOException {
        HashMap<String, ArrayList<Presence>> roster1 = rosterListener.roster;
        HashMap<String, ArrayList<Presence>> roster2 = rosterListener2.roster;
        ArrayList<FileMetaData> remoteLibraries1 = rosterListener.files;
        ArrayList<FileMetaData> remoteLibraries2 = rosterListener2.files;
        
        assertEquals(1, roster2.size());
        assertEquals(1, roster2.get("limebuddy1@gmail.com").size());
        assertTrue(roster2.get("limebuddy1@gmail.com").get(0) instanceof LimePresence);
        assertEquals(Presence.Type.available, roster2.get("limebuddy1@gmail.com").get(0).getType());
        assertGreaterThan(0, remoteLibraries2.size());
        
        assertEquals(1, roster1.size());
        assertEquals(1, roster1.get("limebuddy2@gmail.com").size());
        assertTrue(roster1.get("limebuddy2@gmail.com").get(0) instanceof LimePresence);
        assertEquals(Presence.Type.available, roster1.get("limebuddy2@gmail.com").get(0).getType());
        assertGreaterThan(0, remoteLibraries1.size());

    }
    
    public void testChat() throws InterruptedException, XMPPException, IOException {
        MessageReaderImpl reader = new MessageReaderImpl();
        Presence limeBuddy2 = rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        MessageWriter writer = limeBuddy2.createChat(reader);
        writer.writeMessage("hello world");
       
        Thread.sleep(2 * 1000);
        
        IncomingChatListenerImpl incomingChatListener2 = rosterListener2.listener;
        MessageWriter writer2 = incomingChatListener2.writer;
        writer2.writeMessage("goodbye world");
        
        Thread.sleep(2 * 1000);
        
        assertEquals(1, incomingChatListener2.reader.messages.size());
        assertEquals("hello world", incomingChatListener2.reader.messages.get(0));
        
        assertEquals(1, reader.messages.size());
        assertEquals("goodbye world", reader.messages.get(0)); 
        
    }
    
    public void testSendFile() throws InterruptedException, IOException {
        ProgressListener progressListener = new ProgressListener();
        assertFalse(progressListener.started);
        
        HashMap<String, ArrayList<Presence>> roster1 = rosterListener.roster;
        
        LimePresence limebuddy2 = ((LimePresence)roster1.get("limebuddy2@gmail.com").get(0));
        File toSend = libraryProvider.lib.listFiles()[0];
        FileMetaDataImpl metaData = new FileMetaDataImpl(new Random().nextInt() + "", toSend.getName());
        metaData.setSize(toSend.length());
        metaData.setDate(new Date(toSend.lastModified()));
        metaData.setDescription("cool file");
        limebuddy2.sendFile(metaData, progressListener);        
        
        Thread.sleep(6 * 1000);

        assertTrue(progressListener.started);
        assertTrue(progressListener.completed);
        
        File receivedFile = null;
        File [] savedFiles2 = libraryProvider.saveDir.listFiles();
        for(File saved : savedFiles2) {
            if(saved.getName().equals(toSend.getName())) {
                receivedFile = saved;
                break;
            }
        }
        
        assertNotNull(receivedFile);
        // TODO compare contents

    }
    
    public void testRequestFile() throws InterruptedException, IOException {
        ProgressListener progressListener = new ProgressListener();
        assertFalse(progressListener.started);
        
        HashMap<String, ArrayList<Presence>> roster1 = rosterListener.roster;
        
        LimePresence limebuddy2 = ((LimePresence)roster1.get("limebuddy2@gmail.com").get(0));
        FileMetaData toRequest = rosterListener.files.get(0);
        limebuddy2.requestFile(toRequest, progressListener);        
        
        Thread.sleep(6 * 1000);
        
        assertTrue(progressListener.started);
        assertTrue(progressListener.completed);
        
        File receivedFile = null;
        File [] savedFiles = libraryProvider.saveDir.listFiles();
        for(File saved : savedFiles) {
            if(saved.getName().equals(toRequest.getName())) {
                receivedFile = saved;
                break;
            }
        }
        
        assertNotNull(receivedFile);
        // TODO compare contents              

    }
    
    private File createMockLibrary() throws IOException {
        File lib = new File(new File(System.getProperty("java.io.tmpdir")), "lib" + new Random().nextInt());
        assertTrue(lib.mkdirs());
        lib.deleteOnExit();
        for(int i = 0; i < 5; i++) {
            File file = new File(lib, "file" + i);
            assertTrue(file.createNewFile());
            file.deleteOnExit();  
            FileOutputStream fos = new FileOutputStream(file);
            writeMockData(fos);
            IOUtils.close(fos);
        }
        return lib;
    }

    private void writeMockData(FileOutputStream fos) throws IOException {
        for(int i = 0; i < 10; i++) {
            fos.write(new Random().nextInt());
        }
    }

    class XMPPConnectionConfigurationImpl implements XMPPConnectionConfiguration {
        String userName;
        String pw;
        String host;
        int port;
        String serviceName;
        private final RosterListener rosterListener;

        XMPPConnectionConfigurationImpl(String userName, String pw, String host, int port, String serviceName, RosterListener rosterListener) {
            this.userName = userName;
            this.pw = pw;
            this.host = host;
            this.port = port;
            this.serviceName = serviceName;
            this.rosterListener = rosterListener;
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

        public boolean isAutoLogin() {
            return true;
        }

        public RosterListener getRosterListener() {
            return rosterListener;
        }

        public XMPPErrorListener getErrorListener() {
            return new XMPPErrorListener() {
                public void error(XMPPException exception) {
                    exception.printStackTrace();
                }
            };
        }
    }
    
    class RosterListenerImpl implements RosterListener {
        HashMap<String, ArrayList<Presence>> roster = new HashMap<String, ArrayList<Presence>>();
        ArrayList<FileMetaData> files = new ArrayList<FileMetaData>();
        IncomingChatListenerImpl listener = new IncomingChatListenerImpl();
        
        public void userAdded(User user) {
            System.out.println("user added: " + user.getId());            
            if(roster.get(user.getId()) == null) {
                roster.put(user.getId(), new ArrayList<Presence>());
            }
            final String name = user.getName();
            user.addPresenceListener(new PresenceListener() {
                public void presenceChanged(Presence presence) {
                    String id = StringUtils.parseBareAddress(presence.getJID());
                    if(presence.getType().equals(Presence.Type.available)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<Presence>());
                        }
                        roster.get(id).add(presence);
                        presence.setIncomingChatListener(listener);
                        if(presence instanceof LimePresence) {
                            System.out.println("lime user " + presence.getJID() + " (" + name + ") available");
                            ((LimePresence)presence).setLibraryListener(new LibraryListener() {
                                 public void fileAdded(FileMetaData f){
                                    System.out.println(f.getName() + ": " + f.getId());
                                    files.add(f);
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
    
    class LibraryProviderImpl implements LibraryProvider, Provider<LibraryProvider>{
        File lib;
        File saveDir;
        
        LibraryProviderImpl(File lib) {
            this.lib = lib;
            saveDir = new File(System.getProperty("java.io.tmpdir"), "saveDir" + new Random().nextInt());
            saveDir.mkdirs();
            saveDir.deleteOnExit();
        }
        
        public Iterator<FileMetaData> getFiles() {
            ArrayList<FileMetaData> files = new ArrayList<FileMetaData>();
            File [] toAdd = lib.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            });
            for(File f : toAdd) {
                files.add(toFile(f));
            }
            return files.iterator();
        }

        public InputStream readFile(FileMetaData file) throws FileNotFoundException {
            File toRead = new File(lib, file.getName());
            return new FileInputStream(toRead);
        }

        public OutputStream writeFile(FileMetaData file) throws IOException {
            File toWrite = new File(saveDir, file.getName());
            toWrite.createNewFile();
            toWrite.deleteOnExit();
            return new FileOutputStream(toWrite);
        }

        public LibraryProvider get() {
            return this;
        }
    }
    
    class IncomingFileAcceptorImpl implements IncomingFileAcceptor, Provider<IncomingFileAcceptor> {
        public boolean accept(FileMetaData f) {
            return true;
        }

        public IncomingFileAcceptor get() {
            return this;
        }
    }

    private class MessageReaderImpl implements MessageReader{
        ArrayList<String> messages = new ArrayList<String>();
        public void readMessage(String message) {
            messages.add(message);
        }
    }

    private class IncomingChatListenerImpl implements IncomingChatListener {
        MessageWriter writer;
        MessageReaderImpl reader;
        
        public MessageReader incomingChat(MessageWriter writer) {
            System.out.println("new chat");
            this.writer = writer;
            this.reader = new MessageReaderImpl();
            return reader;
        }
    }
    
    private class ProgressListener implements FileTransferProgressListener, Provider<FileTransferProgressListener> {
        boolean started;
        boolean completed;
        boolean errored;
        
        public void started(FileMetaData file) {
            started = true;
        }

        public void completed(FileMetaData file) {
            completed = true;
        }

        public void updated(FileMetaData file, int percentComplete) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void errored(FileMetaData file) {
            errored = true;
        }

        public FileTransferProgressListener get() {
            return this;
        }
    }
    
    class XMPPConnectionConfigurationListProvider implements Provider<List<XMPPConnectionConfiguration>> {
        private final XMPPConnectionConfiguration[] configurations;

        XMPPConnectionConfigurationListProvider(XMPPConnectionConfiguration ... configurations) {
            this.configurations = configurations;
        }
        
        public List<XMPPConnectionConfiguration> get() {
            return Arrays.asList(configurations);
        }
    }
    
    class FileMetaDataImpl implements FileMetaData {
        private String id;
        private String name;
        private long size;
        private Date date;
        private String description;
    
        FileMetaDataImpl(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

         void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        public long getSize() {
            return size;
        }

        void setSize(long size) {
            this.size = size;
        }

        public Date getDate() {
            return date;
        }

        void setDate(Date date) {
            this.date = date;
        }

        public String getDescription() {
            return description;
        }

        void setDescription(String description) {
            this.description = description;
        }
    }
    
}
