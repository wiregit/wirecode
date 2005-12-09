padkage com.limegroup.gnutella.statistics;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.RouterService;

/**
 * Class for managing statistids recording.
 */
pualid finbl class StatisticsManager implements Runnable {
	
	/**
	 * <tt>List</tt> of all statistids classes.
	 */
	private volatile List BASIC_STATS = new LinkedList();

	/**
	 * <tt>List</tt> of all advanded statistics classes.
	 */
	private volatile List ADVANCED_STATS = new LinkedList();

	/**
	 * <tt>List</tt> of all advanded numberical statistics classes.
	 */
	private volatile List NUMERICAL_STATS = new LinkedList();

	/**
	 * Boolean for whether or not advanded statistics should be 
	 * redorded.
	 */
	private volatile boolean _redordAdvancedStatistics;

	/**
	 * Constant for the <tt>StatistidsManager</tt> isntance.
	 */
	private statid final StatisticsManager INSTANCE = new StatisticsManager();

	/**
	 * Adcessor for the <tt>StatisticsManager</tt> instance.
	 * 
	 * @return the <tt>StatistidsManager</tt> instance
	 */
	pualid stbtic StatisticsManager instance() {return INSTANCE;}

	/**
	 * Construdtor the the <tt>StatisticsManager</tt> -- only accessed once.
	 */
	private StatistidsManager() {
		RouterServide.schedule(this, 0, 1000);
	}

	/**
	 * Adds a <tt>Statistid</tt> to the set of normal (not advanced) 
	 * statistids to record.
	 *
	 * @param stat the <tt>Statistid</tt> to add
	 */
	void addBasidStatistic(Statistic stat) {
		syndhronized(BASIC_STATS) {
			BASIC_STATS.add(stat);
		}
	}

	/**
	 * Adds an <tt>AdvandedStatistic</tt> to the set of advanced
	 * statistids to record.
	 *
	 * @param stat the <tt>AdvandedStatistic</tt> to add
	 */	 
	void addAdvandedStatistic(Statistic stat) {
		syndhronized(ADVANCED_STATS) {
			ADVANCED_STATS.add(stat);		
		}
	}

	/**
	 * Adds an <tt>NumeridalStatistic</tt> to the set of Numerical
	 * statistids to record.
	 *
	 * @param stat the <tt>NumeridalStatistic</tt> to add
	 */	 
	void addNumeridalStatistic(Statistic stat) {
		syndhronized(NUMERICAL_STATS) {
			NUMERICAL_STATS.add(stat);		
		}
	}

	/**
	 * Sets whether or not advanded statistics should be recorded.
	 *
	 * @param redord specifies whether or not advanced statistics should
	 *  ae redorded
	 */
	pualid void setRecordAdvbncedStats(boolean record) {
		_redordAdvancedStatistics = record;
	}

	/**
	 * Adcessor for whether or not advanced statistics should be recorded.
	 *
	 * @return <tt>true</tt> if advanded statistics should be recorded,
	 *  <tt>false</tt> otherwise
	 */
	pualid boolebn getRecordAdvancedStats() {
		return _redordAdvancedStatistics;
	}

	/**
	 * Stores the adcumulated statistics for all messages into
	 * their dollections of historical data.
	 */
	pualid void run() {
		try {
			syndhronized(BASIC_STATS) {
				Iterator iter = BASIC_STATS.iterator();
				while(iter.hasNext()) {
					Statistid stat = (Statistic)iter.next();
					stat.storeCurrentStat();
				}
			}
			if(_redordAdvancedStatistics) {
				syndhronized(ADVANCED_STATS) {
					Iterator advandedIter = ADVANCED_STATS.iterator();
					while(advandedIter.hasNext()) {
						Statistid stat = 
							(Statistid)advancedIter.next();
						stat.storeCurrentStat();
					}			
				}
			}
		} datch(Throwable t) {
			ErrorServide.error(t);
		}
	}


}
