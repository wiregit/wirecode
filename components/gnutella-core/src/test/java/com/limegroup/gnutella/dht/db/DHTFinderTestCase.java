package com.limegroup.gnutella.dht.db;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.LimeWireIOTestModule;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.util.IoUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.dht.DHTTestUtils;
import com.limegroup.gnutella.dht2.DHTManager;
import com.limegroup.gnutella.util.MockUtils;

public abstract class DHTFinderTestCase extends DHTTestCase {

    
    protected Mockery context;
    protected DHTManager dhtManager;
    protected List<MojitoDHT> dhts;
    protected MojitoDHT mojitoDHT;
    protected NetworkManagerStub networkManager;
    protected Injector injector;
    protected PushEndpointFactory pushEndpointFactory;
    protected AlternateLocationFactory alternateLocationFactory;

    public DHTFinderTestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
             
        context = new Mockery();
        dhtManager = context.mock(DHTManager.class);
        
        networkManager = new NetworkManagerStub();

        // to have non-empty push proxies to send
        final ConnectionManager connectionManager 
            = MockUtils.createConnectionManagerWithPushProxies(context);
        
        injector = LimeTestUtils.createInjectorNonEagerly(
                new LimeWireIOTestModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).toInstance(dhtManager);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(ConnectionManager.class).toInstance(connectionManager);
            }
        });
        DHTTestUtils.setLocalIsPrivate(injector, false);
        
        alternateLocationFactory 
            = injector.getInstance(AlternateLocationFactory.class);
        pushEndpointFactory 
            = injector.getInstance(PushEndpointFactory.class);
        
        
        dhts = MojitoUtils.createBootStrappedDHTs(1);
        
        mojitoDHT = dhts.get(0);
        context.checking(new Expectations() {{
            allowing(dhtManager).get(with(any(EntityKey.class)));
            will(new CustomAction("Mojito Get") {
                public Object invoke(Invocation invocation) throws Throwable {
                    return mojitoDHT.get((EntityKey)invocation.getParameter(0));
                }                
            });
            
            allowing(dhtManager).getAll(with(any(EntityKey.class)));
            will(new CustomAction("Mojito Get-All") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    return mojitoDHT.getAll((EntityKey)invocation.getParameter(0));
                }
            });
            
            allowing(dhtManager).getMojitoDHT();
            will(returnValue(mojitoDHT));
        }});
        assertTrue(mojitoDHT.isReady());
    }
    
    @Override
    protected void tearDown() throws Exception {
        IoUtils.closeAll(dhts);
    }
}
