package org.limewire.core.impl.mojito;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.mojito.MojitoDHT;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;

public class MojitoManagerImplTest extends BaseTestCase {

    public MojitoManagerImplTest(String name) {
        super(name);
    }

    public void testGetName() {
        Mockery context = new Mockery();
        final DHTManager dhtManager = context.mock(DHTManager.class);

        context.checking(new Expectations() {
            {
                one(dhtManager).addEventListener(with(any(DHTEventListener.class)));
            }
        });
        MojitoManagerImpl managerImpl = new MojitoManagerImpl(dhtManager);

        final MojitoDHT mojitoDHT = context.mock(MojitoDHT.class);

        final String mojitoName = "mojito";

        context.checking(new Expectations() {
            {
                one(dhtManager).getMojitoDHT();
                will(returnValue(mojitoDHT));
                one(mojitoDHT).getName();
                will(returnValue(mojitoName));
            }
        });

        assertEquals(mojitoName, managerImpl.getName());
        context.assertIsSatisfied();
    }

    public void testIsRunning() {
        Mockery context = new Mockery();
        final DHTManager dhtManager = context.mock(DHTManager.class);

        context.checking(new Expectations() {
            {
                one(dhtManager).addEventListener(with(any(DHTEventListener.class)));
            }
        });
        MojitoManagerImpl managerImpl = new MojitoManagerImpl(dhtManager);

        context.checking(new Expectations() {
            {
                one(dhtManager).isRunning();
                will(returnValue(true));
            }
        });

        assertTrue(managerImpl.isRunning());

        context.checking(new Expectations() {
            {
                one(dhtManager).isRunning();
                will(returnValue(false));
            }
        });

        assertFalse(managerImpl.isRunning());

        context.assertIsSatisfied();
    }
}
