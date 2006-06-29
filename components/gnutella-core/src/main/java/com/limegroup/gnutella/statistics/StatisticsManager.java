package com.limegroup.gnutella.statistics;

import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.RouterService;

/**
 * Class for managing statistics recording.
 */
public final class StatisticsManager implements Runnable {
	
	/**
	 * <tt>List</tt> of all statistics classes.
	 */
	private volatile List<Statistic> BASIC_STATS = new LinkedList<Statistic>();

	/**
	 * <tt>List</tt> of all advanced statistics classes.
	 */
	private volatile List<Statistic> ADVANCED_STATS = new LinkedList<Statistic>();

	/**
	 * <tt>List</tt> of all advanced numberical statistics classes.
	 */
	private volatile List<Statistic> NUMERICAL_STATS = new LinkedList<Statistic>();

	/**
	 * Boolean for whether or not advanced statistics should be 
	 * recorded.
	 */
	private volatile boolean _recordAdvancedStatistics;

	/**
	 * Constant for the <tt>StatisticsManager</tt> isntance.
	 */
	private static final StatisticsManager INSTANCE = new StatisticsManager();

	/**
	 * Accessor for the <tt>StatisticsManager</tt> instance.
	 * 
	 * @return the <tt>StatisticsManager</tt> instance
	 */
	public static StatisticsManager instance() {return INSTANCE;}

	/**
	 * Constructor the the <tt>StatisticsManager</tt> -- only accessed once.
	 */
	private StatisticsManager() {
		RouterService.schedule(this, 0, 1000);
	}

	/**
	 * Adds a <tt>Statistic</tt> to the set of normal (not advanced) 
	 * statistics to record.
	 *
	 * @param stat the <tt>Statistic</tt> to add
	 */
	void addBasicStatistic(Statistic stat) {
		synchronized(BASIC_STATS) {
			BASIC_STATS.add(stat);
		}
	}

	/**
	 * Adds an <tt>AdvancedStatistic</tt> to the set of advanced
	 * statistics to record.
	 *
	 * @param stat the <tt>AdvancedStatistic</tt> to add
	 */	 
	void addAdvancedStatistic(Statistic stat) {
		synchronized(ADVANCED_STATS) {
			ADVANCED_STATS.add(stat);		
		}
	}

	/**
	 * Adds an <tt>NumericalStatistic</tt> to the set of Numerical
	 * statistics to record.
	 *
	 * @param stat the <tt>NumericalStatistic</tt> to add
	 */	 
	void addNumericalStatistic(Statistic stat) {
		synchronized(NUMERICAL_STATS) {
			NUMERICAL_STATS.add(stat);		
		}
	}

	/**
	 * Sets whether or not advanced statistics should be recorded.
	 *
	 * @param record specifies whether or not advanced statistics should
	 *  be recorded
	 */
	public void setRecordAdvancedStats(boolean record) {
		_recordAdvancedStatistics = record;
	}

	/**
	 * Accessor for whether or not advanced statistics should be recorded.
	 *
	 * @return <tt>true</tt> if advanced statistics should be recorded,
	 *  <tt>false</tt> otherwise
	 */
	public boolean getRecordAdvancedStats() {
		return _recordAdvancedStatistics;
	}

	/**
	 * Stores the accumulated statistics for all messages into
	 * their collections of historical data.
	 */
	public void run() {
		synchronized(BASIC_STATS) {
            for(Statistic stat : BASIC_STATS)
				stat.storeCurrentStat();
		}
		if(_recordAdvancedStatistics) {
			synchronized(ADVANCED_STATS) {
                for(Statistic stat : ADVANCED_STATS)
					stat.storeCurrentStat();
			}
		}
	}


}
