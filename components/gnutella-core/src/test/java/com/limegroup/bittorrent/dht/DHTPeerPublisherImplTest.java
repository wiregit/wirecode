package com.limegroup.bittorrent.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.FixedDHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
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
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

public class DHTPeerPublisherImplTest extends BaseTestCase {
    private static String HASH1 = "urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private static String HASH2 = "urn:sha1:BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

    private static final byte[] IP = (new String("124.0.0.1")).getBytes();

    private static final byte[] ID = (new String("123")).getBytes();

    private static final int PORT = 4444;

    private TorrentLocation torLoc = null;

    private Mockery context;

    private ManagedTorrent managedTorrentOne;

    private ManagedTorrent managedTorrentTwo;

    private DHTManager dhtManager;

    private ApplicationServices applicationServices;

    private NetworkManager networkManager;

    private MojitoDHT dht;

    private BTMetaInfo btMetaInfoOne;

    private BTMetaInfo btMetaInfoTwo;

    private Contact contact;

    private Module module;

    private DHTPeerPublisher dhtPeerPublisher;

    private KUID kuidOne;

    private EntityKey eKey;

    private URN urnOne;

    private URN urnTwo;

    private TorrentManager torrentManager;

    public DHTPeerPublisherImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DHTPeerPublisherImplTest.class);
    }

    public void setUp() throws Exception {
        context = new Mockery();
        managedTorrentOne = context.mock(ManagedTorrent.class);
        managedTorrentTwo = context.mock(ManagedTorrent.class);
        dhtManager = context.mock(DHTManager.class);
        applicationServices = context.mock(ApplicationServices.class);
        networkManager = context.mock(NetworkManager.class);
        dht = context.mock(MojitoDHT.class);
        btMetaInfoOne = context.mock(BTMetaInfo.class);
        btMetaInfoTwo = context.mock(BTMetaInfo.class);
        contact = context.mock(Contact.class);
        torrentManager = context.mock(TorrentManager.class);

        try {
            urnOne = URN.createSHA1Urn(HASH1);
            urnTwo = URN.createSHA1Urn(HASH2);
        } catch (IOException ie) {
            fail(ie);
        }

        module = new AbstractModule() {
            public void configure() {
                bind(ManagedTorrent.class).toInstance(managedTorrentOne);
                bind(ManagedTorrent.class).toInstance(managedTorrentTwo);
                bind(DHTManager.class).toInstance(dhtManager);
                bind(ApplicationServices.class).toInstance(applicationServices);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(MojitoDHT.class).toInstance(dht);
                bind(BTMetaInfo.class).toInstance(btMetaInfoOne);
                bind(BTMetaInfo.class).toInstance(btMetaInfoTwo);
                bind(TorrentManager.class).toInstance(torrentManager);
            }
        };

        Injector inj = LimeTestUtils.createInjector(module);

        dhtPeerPublisher = inj.getInstance(DHTPeerPublisher.class);
        kuidOne = KUIDUtils.toKUID(urnOne);
        eKey = EntityKey.createEntityKey(kuidOne, DHTPeerLocatorUtils.BT_PEER_TRIPLE);
    }

    // Tests if publishYourself properly stores the torrents in waiting list if
    // a DHT was not available or did not support bootstrapping. It also test to
    // ensure duplicate torrents do not get stored in the waiting list.
    public void testpublishYourselfWhichShouldPutTorrentsInWaitingList() {

        context.checking(new Expectations() {
            {
                exactly(2).of(dhtManager).getMojitoDHT();
                will(returnValue(dht));

                exactly(2).of(dht).isBootstrapped();
                will(returnValue(false));

                exactly(2).of(managedTorrentOne).isActive();
                will(returnValue(true));
                exactly(2).of(managedTorrentOne).getMetaInfo();
                will(returnValue(btMetaInfoOne));

                one(managedTorrentTwo).isActive();
                will(returnValue(true));
                one(managedTorrentTwo).getMetaInfo();
                will(returnValue(btMetaInfoTwo));

                exactly(2).of(btMetaInfoOne).getURN();
                will(returnValue(urnOne));

                one(btMetaInfoTwo).getURN();
                will(returnValue(urnTwo));

                exactly(3).of(networkManager).acceptedIncomingConnection();
                will(returnValue(true));

                never(dht).get(with(any(EntityKey.class)));
            }
        });

        // should store these torrents in the waiting list
        dhtPeerPublisher.publishYourself(managedTorrentOne);
        dhtPeerPublisher.publishYourself(managedTorrentTwo);
        // should not pass the initial checks as it is already in the waiting
        // list
        dhtPeerPublisher.publishYourself(managedTorrentOne);
        context.assertIsSatisfied();

    }

    // Tests if publishYourself properly publishes the local host in DHT.
    public void testpublishYourselfWhichShouldPubilshAPeerInDHT() {

        context.checking(new Expectations() {
            {
                exactly(2).of(networkManager).getAddress();
                will(returnValue(IP));
                exactly(2).of(networkManager).getPort();
                will(returnValue(PORT));
                exactly(2).of(applicationServices).getMyBTGUID();
                will(returnValue(ID));

                exactly(2).of(dht).getLocalNode();
                will(returnValue(contact));

                one(contact).getNodeID();
                will(returnValue(kuidOne));
            }
        });

        byte[] msg = null;

        try {
            torLoc = new TorrentLocation(InetAddress.getByName(NetworkUtils
                    .ip2string((networkManager.getAddress()))), networkManager.getPort(),
                    applicationServices.getMyBTGUID());
            msg = DHTPeerLocatorUtils.encode(torLoc);
        } catch (UnknownHostException uhe) {
            fail(uhe);
        } catch (IllegalArgumentException iae) {
            fail(iae);
        }

        final DHTValue dhtValue = new DHTValueImpl(DHTPeerLocatorUtils.BT_PEER_TRIPLE,
                Version.ZERO, msg);

        DHTValueEntity entity = DHTValueEntity.createFromValue(dht, kuidOne, dhtValue);

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

                exactly(2).of(managedTorrentOne).isActive();
                will(returnValue(true));

                exactly(2).of(managedTorrentOne).getMetaInfo();
                will(returnValue(btMetaInfoOne));

                exactly(2).of(btMetaInfoOne).getURN();
                will(returnValue(urnOne));

                one(dht).put(with(any(KUID.class)), with(any(DHTValue.class)));
                will(returnValue(future));

                exactly(2).of(networkManager).acceptedIncomingConnection();
                will(returnValue(true));

                one(torrentManager).getTorrentForURN(with(equal(urnOne)));
                will(returnValue(managedTorrentOne));
            }
        });

        // store in waiting list
        dhtPeerPublisher.publishYourself(managedTorrentOne);

        // call made to ensure the torrent got stored as published
        dhtPeerPublisher.publishYourself(managedTorrentOne);

        context.assertIsSatisfied();
    }
}