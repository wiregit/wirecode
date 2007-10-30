package org.limewire.statistic;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.SimpleTimer;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Manages recording {@link Statistic Statistics}. When a <code>Statistic</code>s
 * is instantiated, the <code>Statistic</code> is managed by this singleton 
 * <code>StatisticsManager</code>.
 * <p> 
 * Additionally, <code>StatisticsManager</code> includes methods to set and get 
 * that status whether to record advanced statistics.
 */
public final class StatisticsManager implements Runnable {
	
    @Inject @Named("backgroundExecutor") private volatile static ScheduledExecutorService defaultExecutor;
    
	/**
	 * <tt>List</tt> of all statistics classes.
	 */
	private volatile List<Statistic> BASIC_STATS = new LinkedList<Statistic>();

	/**
	 * <tt>List</tt> of all advanced statistics classes.
	 */
	private volatile List<Statistic> ADVANCED_STATS = new LinkedList<Statistic>();

	/**
	 * <tt>List</tt> of all advanced numerical statistics classes.
	 */
	private volatile List<Statistic> NUMERICAL_STATS = new LinkedList<Statistic>();

	/**
	 * Boolean for whether or not advanced statistics should be 
	 * recorded.
	 */
	private volatile boolean _recordAdvancedStatistics;
    
    /**
     * Boolean for whether or not advanced statistics were
     * turned on manually.
     */
    private volatile boolean _recordAdvancedStatisticsManual;

	/**
	 * Constant for the <tt>StatisticsManager</tt> instance.
	 */
	private static StatisticsManager INSTANCE;

	/**
	 * Accessor for the <tt>StatisticsManager</tt> instance.
	 * 
	 * @return the <tt>StatisticsManager</tt> instance
	 */
	public static synchronized StatisticsManager instance() {
	    if(INSTANCE == null)
	        INSTANCE = new StatisticsManager();
	    return INSTANCE;
	}

	/**
	 * Constructor the the <tt>StatisticsManager</tt> -- only accessed once.
	 */
	private StatisticsManager() {
	    ScheduledExecutorService executorService = defaultExecutor != null ? defaultExecutor : new SimpleTimer(true);
        executorService.scheduleWithFixedDelay(this, 0, 1000, TimeUnit.MILLISECONDS);
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
	 * Adds a <tt>NumericalStatistic</tt> to the set of Numerical
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
    
    public void setRecordAdvancedStatsManual(boolean record) {
        _recordAdvancedStatisticsManual = record;
        setRecordAdvancedStats(record);
    }
    
    /**
     * @return true if the user manually turned on advanced statistics.
     */
    public boolean getRecordAdvancedStatsManual() {
        return _recordAdvancedStatisticsManual;
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
