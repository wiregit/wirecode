package org.limewire.xmpp.client;

import java.util.Arrays;
import java.util.List;

import org.limewire.inject.AbstractModule;
import org.limewire.lifecycle.ServiceTestCase;

import com.google.inject.Module;

public class XMPPServiceTest extends ServiceTestCase {

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
                bind(XMPPServiceConfiguration.class).toInstance(new XMPPServiceConfiguration() {
                    public boolean isDebugEnabled() {
                        return true;
                    }

                    public String getUsername() {
                        return "limebuddy1@gmail.com";
                    }

                    public String getPassword() {
                        return "limebuddy123";
                    }

                    public String getHost() {
                        return "talk.google.com";
                    }

                    public int getPort() {
                        return 5222;
                    }

                    public String getServiceName() {
                        return "gmail.com";
                    }
                });
                
                bind(XMPPListeners.class).toInstance(new XMPPListeners() {
                    public RosterListener getRosterListener() {
                        return new RosterListener(){};
                    }

                    public PresenceListener getPresenceListener() {
                        return new PresenceListener(){};
                    }

                    public LibraryListener getLibraryListener() {
                        return new LibraryListener(){};
                    }
                });
            }
        };
        return Arrays.asList(new LimeWireXMPPModule(), m);
    }

    public void test() {
        //            
    }
}
