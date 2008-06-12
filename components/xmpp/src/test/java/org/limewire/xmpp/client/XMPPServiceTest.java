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
                        return "limebuddy2@gmail.com";
                    }

                    public String getPassword() {
                        return "limebuddy234";
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
                
                bind(RosterListener.class).toInstance(new RosterListener() {
                    public void userAdded(User user) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void userUpdated(User user) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void userDeleted(User user) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void presenceChanged(User user) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }
                });
            }
        };
        return Arrays.asList(new LimeWireXMPPModule(), m);
    }

    public void test() throws InterruptedException {
        Thread.sleep(5* 60 * 1000);            
    }
}
