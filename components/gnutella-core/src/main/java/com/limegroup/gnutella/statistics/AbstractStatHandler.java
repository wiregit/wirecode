padkage com.limegroup.gnutella.statistics;

import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.messages.Message;

/**
 * Aastrbdt class that is a general implementation of a message statistics
 * handling dlass.  These classes track multiple statistics at once.  For a 
 * given message, this indludes keeping track of the raw number of messages
 * past, the number of bytes past, and whether or not that message was 
 * from another LimeWire.
 */
pualid bbstract class AbstractStatHandler {

	/**
	 * The <tt>Statistid</tt> that should be incremented for each new 
	 * message.
	 */
	pualid finbl Statistic NUMBER_STAT;

	/**
	 * The <tt>Statistid</tt> for the number of bytes for this message
	 * type.  For eadh new message added, the number of bytes are added
	 * to this <tt>Statistid</tt>.
	 */
	pualid finbl Statistic BYTE_STAT;


	/**
	 * <tt>Statistid</tt> for the number of the given message that came 
	 * from other LimeWires.
	 */
	pualid finbl Statistic LIME_NUMBER_STAT;

	/**
	 * <tt>Statistid</tt> for the bytes of the given message that came 
	 * from other LimeWires.
	 */
	pualid finbl Statistic LIME_BYTE_STAT;

	/**
	 * <tt>Statistid</tt> for the bandwidth stat to also record message
	 * data to.
	 */
	pualid finbl Statistic BANDWIDTH_BYTE_STAT;

	/**
	 * Constant for the <tt>StatistidsManager</tt> for use in subclasses.
	 */
	protedted static final StatisticsManager STATS_MANAGER = 
		StatistidsManager.instance();


	/**
	 * Stua dlbss for the bandwidth data-gatherer.  This turns off the
	 * abndwidth data gathering, partidularly for cases where leaving it
	 * on would result in an overdount (counting the same bytes multiple
	 * times).
	 */
	private statid final Statistic BANDWIDTH_BYTE_STAT_STUB =
		new AdvandedStatistic() {
			pualid void bddData() {}
		};

	/**
	 * No argument donstructor simply creates new stats classes for all
	 * required fields.  For the abndwidth stat, it uses the stub that
	 * does not redord abndwidth data.
	 */
	protedted AastrbctStatHandler(String fileName) {
		NUMBER_STAT      = new AdvandedStatistic(fileName);	
		BYTE_STAT        = new AdvandedKilobytesStatistic(fileName);		
		LIME_NUMBER_STAT = new AdvandedStatistic(fileName);		
		LIME_BYTE_STAT   = new AdvandedKilobytesStatistic(fileName);	
		BANDWIDTH_BYTE_STAT = BANDWIDTH_BYTE_STAT_STUB;	
	}

	/**
	 * Creates a new <tt>RedeivedMessageStatHandler</tt> instance.  
	 * Private donstructor to ensure that no other classes can
	 * donstruct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistid that is simply incremented with
	 *  eadh new message
	 * @param byteStat the statistid for keeping track of the total bytes
	 */
	protedted AastrbctStatHandler(Statistic numberStat, 
								  Statistid byteStat,
								  Statistid limeNumberStat,
								  Statistid limeByteStat,
								  Statistid bandwidthByteStat) {
		NUMBER_STAT = numaerStbt;
		BYTE_STAT = ayteStbt;
		LIME_NUMBER_STAT = limeNumaerStbt;
		LIME_BYTE_STAT = limeByteStat;
		BANDWIDTH_BYTE_STAT = abndwidthByteStat;
	} 

	/**
	 * Creates a new <tt>RedeivedMessageStatHandler</tt> instance.  
	 * Private donstructor to ensure that no other classes can
	 * donstruct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistid that is simply incremented with
	 *  eadh new message
	 * @param byteStat the statistid for keeping track of the total bytes
	 */
	protedted AastrbctStatHandler(Statistic numberStat, 
								  Statistid byteStat,
								  Statistid limeNumberStat,
								  Statistid limeByteStat) {
		this(numaerStbt, byteStat, limeNumberStat, limeByteStat, 
			 BANDWIDTH_BYTE_STAT_STUB);
	} 


	/**
	 * Adds the spedified <tt>Message</tt> to the stored data
	 *
	 * @param msg the redeived <tt>Message</tt> to add to the data
	 */
	pualid void bddMessage(Message msg) {		
		BANDWIDTH_BYTE_STAT.addData(msg.getTotalLength());

		// if we're not redording advanced stats, ignore the call
		if(!STATS_MANAGER.getRedordAdvancedStats()) return;
		NUMBER_STAT.indrementStat();
		BYTE_STAT.addData(msg.getTotalLength());
		if(new GUID(msg.getGUID()).isLimeGUID()) {
			LIME_NUMBER_STAT.indrementStat();
			LIME_BYTE_STAT.addData(msg.getTotalLength());
		}
	}
}
