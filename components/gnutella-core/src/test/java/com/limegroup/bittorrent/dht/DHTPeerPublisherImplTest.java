package com.limegroup.bittorrent.dht;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.FixedDHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

public class DHTPeerPublisherImplTest extends BaseTestCase {
    private static String HASH = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";

    private static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("LimeBT Peer Triple",
            "PEER");

    final byte[] ip = (new String("124.0.0.1")).getBytes();

    final byte[] id = (new String("123")).getBytes();

    public DHTPeerPublisherImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DHTPeerPublisherImplTest.class);
    }

    private Mockery context;

    private ManagedTorrent managedTorrentOne;

    private ManagedTorrent managedTorrentTwo;

    private DHTManager dhtManager;

    private ApplicationServices applicationServices;

    private NetworkManager networkManager;

    private MojitoDHT dht;

    private BTMetaInfo btMetaInfo;

    private Contact contact;

    private URN urn;

    private Module module;

    private DHTPeerPublisher dhtPeerPublisher;

    private KUID kuid;

    private EntityKey eKey;

    public void setUp() throws Exception {
        context = new Mockery();
        managedTorrentOne = context.mock(ManagedTorrent.class);
        managedTorrentTwo = context.mock(ManagedTorrent.class);
        dhtManager = context.mock(DHTManager.class);
        applicationServices = context.mock(ApplicationServices.class);
        networkManager = context.mock(NetworkManager.class);
        dht = context.mock(MojitoDHT.class);
        btMetaInfo = context.mock(BTMetaInfo.class);
        contact = context.mock(Contact.class);
        try {
            urn = URN.createSHA1Urn(HASH);
        } catch (IOException ioe) {
            fail(ioe);
        }

        module = new AbstractModule() {
            public void configure() {
                bind(ManagedTorrent.class).toInstance(managedTorrentOne);
                bind(ManagedTorrent.class).toInstance(managedTorrentTwo);
                bind(DHTManager.class).toInstance(dhtManager);
                bind(ApplicationServices.class).toInstance(applicationServices);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(MojitoDHT.class).toInstance(dht);
                bind(BTMetaInfo.class).toInstance(btMetaInfo);
            }
        };

        Injector inj = LimeTestUtils.createInjector(module);

        dhtPeerPublisher = inj.getInstance(DHTPeerPublisher.class);
        kuid = KUIDUtils.toKUID(urn);
        eKey = EntityKey.createEntityKey(kuid, BT_PEER_TRIPLE);
    }

    // Tests if publishYourself properly stores the torrents in waiting list if
    // a DHT was not available or did not support bootstrapping. It also test to
    // ensure duplicate torrents do not get stored in the waiting list.
    public void testpublishYourselfWhichShouldPutTorrentsInWaitingList() throws Exception {

        context.checking(new Expectations() {
            {
                exactly(2).of(dhtManager).getMojitoDHT();
                will(returnValue(dht));

                exactly(2).of(dht).isBootstrapped();
                will(returnValue(false));

                exactly(2).of(managedTorrentOne).isActive();
                will(returnValue(true));
                one(managedTorrentOne).getMetaInfo();
                will(returnValue(btMetaInfo));

                one(managedTorrentTwo).isActive();
                will(returnValue(true));
                one(managedTorrentTwo).getMetaInfo();
                will(returnValue(btMetaInfo));

                exactly(2).of(btMetaInfo).getURN();
                will(returnValue(urn));

                exactly(3).of(networkManager).acceptedIncomingConnection();
                will(returnValue(true));

                never(dht).get(with(any(EntityKey.class)));
            }
        });

        // should store these torrents in the waiting list
        dhtPeerPublisher.publishYourself(managedTorrentOne);
        dhtPeerPublisher.publishYourself(managedTorrentTwo);
        // should not store this in waiting list as it should already be stored
        dhtPeerPublisher.publishYourself(managedTorrentOne);
        context.assertIsSatisfied();

    }

    // Tests if publishYourself properly publishes the local host in DHT.
    public void testpublishYourselfWhichShouldPubilshAPeerInDHT() throws Exception {

        context.checking(new Expectations() {
            {
                atLeast(2).of(networkManager).getAddress();
                will(returnValue(ip));
                atLeast(1).of(networkManager).getPort();
                will(returnValue(5555));
                atLeast(1).of(applicationServices).getMyBTGUID();
                will(returnValue(id));

                exactly(2).of(dht).getLocalNode();
                will(returnValue(contact));

                exactly(1).of(contact).getNodeID();
                will(returnValue(kuid));
            }
        });

        BTConnectionTriple btct = new BTConnectionTriple(networkManager.getAddress(), 4444,
                applicationServices.getMyBTGUID());

        byte[] msg = btct.getEncoded();

        final DHTValue dhtValue = new DHTValueImpl(BT_PEER_TRIPLE, Version.ZERO, msg);

        DHTValueEntity entity = DHTValueEntity.createFromValue(dht, kuid, dhtValue);

        Collection<DHTValueEntity> entities = new ArrayList<DHTValueEntity>();
        entities.add(entity);

        Collection<EntityKey> entityKeys = new ArrayList<EntityKey>();
        entityKeys.add(eKey);

        StoreResult result = new StoreResult(null, entities);
        final DHTFuture<StoreResult> future = new FixedDHTFuture<StoreResult>(result);

        context.checking(new Expectations() {
            {
                one(dhtManager).getMojitoDHT();
                will(returnValue(dht));

                one(dht).isBootstrapped();
                will(returnValue(true));

                atLeast(2).of(managedTorrentOne).isActive();
                will(returnValue(true));
                one(managedTorrentOne).getMetaInfo();
                will(returnValue(btMetaInfo));

                one(btMetaInfo).getURN();
                will(returnValue(urn));

                atLeast(1).of(dht).put(with(any(KUID.class)), with(any(DHTValue.class)));
                will(returnValue(future));

                atLeast(2).of(networkManager).acceptedIncomingConnection();
                will(returnValue(true));
            }
        });

        // store in waiting list
        dhtPeerPublisher.publishYourself(managedTorrentOne);

        // call made to ensure the torrent got stored as published
        dhtPeerPublisher.publishYourself(managedTorrentOne);

        context.assertIsSatisfied();
    }
}