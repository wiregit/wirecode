package com.limegroup.gnutella.messagehandlers;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.statistic.StatisticsManager;
import org.limewire.util.Base32;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.AdvancedStatsToggle;
import com.limegroup.gnutella.messages.vendor.RoutableGGEPMessage;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class AdvancedToggleHandlerTest extends LimeTestCase {

    public AdvancedToggleHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AdvancedToggleHandlerTest.class);
    }    
    private static InetSocketAddress addr;
    
    private static String s50 = "YUREXAX33UQQL7BIKGO2ISFCAAYQCACJAAAAATCJJVCSAAABADBQCVCBGKAVMQIBYMBFGQSAQNJUSR3OGAWAEFAJ6HNOWWYVD7ZAKIJW2GPTWIKDUVH6TBACCQ3VY5HCWCBIQXZ4652WQ7USNF3NCSW334";
    private static String s100 = "YZLP3AP75DG7OJCHXSLIVC5CAAYQCACJAAAAATCJJVCSAAABADBQCVCBMSAVMQIBYMBFGQSAQNJUSR3OGAWAEFAVP3COFPLJEVXXE2H6AJ4PDOXADSH7IXQCCQD6BDUXZGYORVOLJGNBHEDZQQLJX6BYVY";
    private static String s100V2 = "ROXY5H2T4C4WJ7XSRHNBNP2FAAYQCACKAAAAATCJJVCSAAABADBQCVCBMSAVMQICYMBFGQSAQNJUSR3PGAWQEFDLORMVC74ZWNFTBAGGRBW3KORVUS6K5NACCUAIRFUHEYPN6KH2XPA52744DE5EPV7DGMIQ";
    private static String s500 = "SIE5PGNB74AKWY67D2LES6MVAAYQCACLAAAAATCJJVCSAAABADBQCVCC6QAYCVSBAHBQEU2CICBVGSKHN4YC2AQVACBTPK5KWQOM46D5QCD6HSFWP24IEP3EIIBBIJPKDCVYMZ2K7BWN632F3VA7QZLVKFFQG";
    private static String sOffV2 = "E3UD6HDAMCJX5SVVVCGBOWCFAAYQCACLAAAAATCJJVCSAAABADBQGT2GIZAICVSBALBQEU2CICBVGSKHN4YC2AQUDIZJNEID3WJ6HEXMREY5F6U3DIKNFCAYAIKQBCAIVIH4G6U4HIKL7DEXZNK4UR64FZ3GW";
    private static String s1000 = "TVOK2STBPOEHYXXSA2YS5RX4AAYQCACKAAAAATCJJVCSAAABADBQCVCC5ABYCVSBAHBQEU2CICBVGSKHNYYCYAQUID6RM5MZTWCRZT7G563AQ7BW55HXPLN3AIKDHWOBMQFM5WTBG3L7W3SIBYK5IUK5QQHA";
    
    private Injector injector;
    private AdvancedToggleHandler advancedToggleHandler;
    private MessageFactory messageFactory;
    
    public void setUp() throws Exception {
        FilterSettings.INSPECTOR_IP_ADDRESSES.setValue(new String[]{"127.0.0.1"});
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ExecutorService.class).annotatedWith(Names.named("messageExecutor")).toInstance(new AbstractExecutorService() {

                    public boolean awaitTermination(long timeout, TimeUnit unit)
                            throws InterruptedException {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    public boolean isShutdown() {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    public boolean isTerminated() {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    public void shutdown() {
                        // TODO Auto-generated method stub
                        
                    }

                    public List<Runnable> shutdownNow() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    public void execute(Runnable command) {
                        command.run();                        
                    }
                    
                });
            }
        });
        advancedToggleHandler = injector.getInstance(AdvancedToggleHandler.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        
        ApplicationSettings.USAGE_STATS.setValue(true);
        addr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 1000);
        StatisticsManager.instance().setRecordAdvancedStatsManual(false);
        PrivilegedAccessor.setValue(AdvancedToggleHandler.class, "MAX_TIME", 60 * 60 * 1000);
    }
    
    private AdvancedStatsToggle getToggle(String source) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base32.decode(source));
        return (AdvancedStatsToggle) messageFactory.read(bais, Network.TCP);
    }
    /**
     * Tests that if the usage stats setting is off, the message does nothing.
     */
    public void testSettingRespected() throws Exception {
        ApplicationSettings.USAGE_STATS.setValue(false);
        AdvancedStatsToggle toggle = getToggle(s100);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        advancedToggleHandler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests turning on, and after a while shutting off of stats.
     */
    public void testTurnOn() throws Exception {
        AdvancedStatsToggle toggle = getToggle(s100);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        advancedToggleHandler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        Thread.sleep(120);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }

    /**
     * Tests immediately shutting off stats with a toggle message.
     */
    public void testImmediateShutOff() throws Exception {
        AdvancedStatsToggle toggle = getToggle(s500);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        advancedToggleHandler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        
        // send the shut offf toggle
        AdvancedStatsToggle toggleOff = getToggle(sOffV2);
        advancedToggleHandler.handleMessage(toggleOff, addr, new ReplyHandlerStub());
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests that if the user had turned the stats on, the stats will not
     * get shut off after the timeout
     */
    public void testUserOnNotSchedule() throws Exception {
        AdvancedStatsToggle toggle = getToggle(s50);
        StatisticsManager.instance().setRecordAdvancedStatsManual(true);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        advancedToggleHandler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());

        // time has expired, but stats are still on
        Thread.sleep(100);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests that if the user had turned the stats on, they will not get
     * shut off by shutoff message
     */
    public void testUserOnNotShut() throws Exception {
        AdvancedStatsToggle toggle = getToggle(sOffV2);
        StatisticsManager.instance().setRecordAdvancedStatsManual(true);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        advancedToggleHandler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests that the time to keep stats can be extended.
     */
    public void testExtend() throws Exception {
        AdvancedStatsToggle toggle = getToggle(s100);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        advancedToggleHandler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        
        // sleep some time, send another message
        Thread.sleep(80);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        advancedToggleHandler.handleMessage(getToggle(s100V2), addr, new ReplyHandlerStub());
        
        // now sleep more - it should not be off for another 100ms
        Thread.sleep(80);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        
        // now it should be off.
        Thread.sleep(40);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests that the stats cannot be turned on for more than the maximum
     * time.
     */
    public void testMaxTime() throws Exception {
        PrivilegedAccessor.setValue(AdvancedToggleHandler.class, "MAX_TIME", 100);
        AdvancedStatsToggle toggle = getToggle(s1000);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        advancedToggleHandler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        
        // the message asked for 1000ms, but after a 100 ms stats will be off.
        Thread.sleep(120);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    static class StubSigner implements RoutableGGEPMessage.GGEPSigner {
        public GGEP getSecureGGEP(GGEP original) {
            return original;
        }
    }
}
