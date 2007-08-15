package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedKilobytesStatistic;
import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.Statistic;
import org.limewire.statistic.StatisticsManager;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.Message;

/**
 * Abstract class that is a general implementation of a message statistics
 * handling class.  These classes track multiple statistics at once.  For a 
 * given message, this includes keeping track of the raw number of messages
 * past, the number of bytes past, and whether or not that message was 
 * from another LimeWire.
 */
public abstract class AbstractStatHandler {

	/**
	 * The <tt>Statistic</tt> that should be incremented for each new 
	 * message.
	 */
	public final Statistic NUMBER_STAT;

	/**
	 * The <tt>Statistic</tt> for the number of bytes for this message
	 * type.  For each new message added, the number of bytes are added
	 * to this <tt>Statistic</tt>.
	 */
	public final Statistic BYTE_STAT;


	/**
	 * <tt>Statistic</tt> for the number of the given message that came 
	 * from other LimeWires.
	 */
	public final Statistic LIME_NUMBER_STAT;

	/**
	 * <tt>Statistic</tt> for the bytes of the given message that came 
	 * from other LimeWires.
	 */
	public final Statistic LIME_BYTE_STAT;

	/**
	 * <tt>Statistic</tt> for the bandwidth stat to also record message
	 * data to.
	 */
	public final Statistic BANDWIDTH_BYTE_STAT;

	/**
	 * Constant for the <tt>StatisticsManager</tt> for use in subclasses.
	 */
	protected static final StatisticsManager STATS_MANAGER = 
		StatisticsManager.instance();


	/**
	 * Stub class for the bandwidth data-gatherer.  This turns off the
	 * bandwidth data gathering, particularly for cases where leaving it
	 * on would result in an overcount (counting the same bytes multiple
	 * times).
	 */
	private static final Statistic BANDWIDTH_BYTE_STAT_STUB =
		new AdvancedStatistic() {          
            @Override
            public void addData(int data) {
            }
        
    };

	/**
	 * No argument constructor simply creates new stats classes for all
	 * required fields.  For the bandwidth stat, it uses the stub that
	 * does not record bandwidth data.
	 */
	protected AbstractStatHandler(String fileName) {
		NUMBER_STAT      = new AdvancedStatistic(fileName);	
		BYTE_STAT        = new AdvancedKilobytesStatistic(fileName);		
		LIME_NUMBER_STAT = new AdvancedStatistic(fileName);		
		LIME_BYTE_STAT   = new AdvancedKilobytesStatistic(fileName);	
		BANDWIDTH_BYTE_STAT = BANDWIDTH_BYTE_STAT_STUB;	
	}

	/**
	 * Creates a new <tt>ReceivedMessageStatHandler</tt> instance.  
	 * Private constructor to ensure that no other classes can
	 * construct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistic that is simply incremented with
	 *  each new message
	 * @param byteStat the statistic for keeping track of the total bytes
	 */
	protected AbstractStatHandler(Statistic numberStat, 
								  Statistic byteStat,
								  Statistic limeNumberStat,
								  Statistic limeByteStat,
								  Statistic bandwidthByteStat) {
		NUMBER_STAT = numberStat;
		BYTE_STAT = byteStat;
		LIME_NUMBER_STAT = limeNumberStat;
		LIME_BYTE_STAT = limeByteStat;
		BANDWIDTH_BYTE_STAT = bandwidthByteStat;
	} 

	/**
	 * Creates a new <tt>ReceivedMessageStatHandler</tt> instance.  
	 * Private constructor to ensure that no other classes can
	 * construct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistic that is simply incremented with
	 *  each new message
	 * @param byteStat the statistic for keeping track of the total bytes
	 */
	protected AbstractStatHandler(Statistic numberStat, 
								  Statistic byteStat,
								  Statistic limeNumberStat,
								  Statistic limeByteStat) {
		this(numberStat, byteStat, limeNumberStat, limeByteStat, 
			 BANDWIDTH_BYTE_STAT_STUB);
	} 


	/**
	 * Adds the specified <tt>Message</tt> to the stored data
	 *
	 * @param msg the received <tt>Message</tt> to add to the data
	 */
	public void addMessage(Message msg) {		
		BANDWIDTH_BYTE_STAT.addData(msg.getTotalLength());

		// if we're not recording advanced stats, ignore the call
		if(!STATS_MANAGER.getRecordAdvancedStats()) return;
		NUMBER_STAT.incrementStat();
		BYTE_STAT.addData(msg.getTotalLength());
		if(new GUID(msg.getGUID()).isLimeGUID()) {
			LIME_NUMBER_STAT.incrementStat();
			LIME_BYTE_STAT.addData(msg.getTotalLength());
		}
	}
}
