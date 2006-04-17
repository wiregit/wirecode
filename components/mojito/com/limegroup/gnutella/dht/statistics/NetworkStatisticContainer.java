package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;

import de.kapsi.net.kademlia.Context;

public class NetworkStatisticContainer extends StatisticContainer {

    public NetworkStatisticContainer(Context context) {
        super(context);
    }
    
    public void writeStats(Writer writer) throws IOException {
        writer.write("Network Stats:\n");
        super.writeStats(writer);
    }
    
    /**
     * <tt>Statistic</tt> for all outgoing messages
     */
    public Statistic SENT_MESSAGES_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the size of all outgoing messages
     */
    public Statistic SENT_MESSAGES_SIZE =
        new MessageSizeStatistic();
    
    /**
     * <tt>Statistic</tt> for all incoming messages
     */
    public Statistic RECEIVED_MESSAGES_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the size of all incoming messages
     */
    public Statistic RECEIVED_MESSAGES_SIZE =
        new MessageSizeStatistic();
    
    /**
     * <tt>Statistic</tt> for late coming responses
     */
    public Statistic LATE_MESSAGES_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for sent PING messages
     */
    public Statistic PINGS_SENT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for incoming PONG messages
     */
    public Statistic PINGS_OK =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for failed PING messages
     */
    public Statistic PINGS_FAILED =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for received PING messages
     */
    public Statistic PING_REQUESTS =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for sent PONG messages
     */
    public Statistic PONGS_SENT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for sent signed PONG messages
     */
    public Statistic SIGNED_PONGS_SENT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for filtered messages
     */
    public Statistic FILTERED_MESSAGES =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for receipts timeouts
     */
    public Statistic RECEIPTS_TIMEOUT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for evicted receipts
     */
    public Statistic RECEIPTS_EVICTED =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for bootstrap time
     */
    public Statistic BOOTSTRAP_TIME =
        new SimpleStatistic();
    
    
    protected class MessageSizeStatistic extends AbstractStatistic{
        public void addData(int data) {
            super.addData(data);
            super.storeCurrentStat();
        }
    }

}
