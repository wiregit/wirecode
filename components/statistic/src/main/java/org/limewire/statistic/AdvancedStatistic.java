package org.limewire.statistic;


/**
 * Specialized subclass for recording advanced statistics.
 * Advanced statistics are only recorded if 
 * {@link StatisticsManager StatisticsManager.instance().setRecordAdvancedStats(true)}
 * is called. 
 * <p>
 * A sample, aka a cycle of data, is all the data collected between calls to 
 * {@link #storeCurrentStat()}. 
 * Therefore, the {@link #getMax()} is the largest sample and the 
 * {@link #getAverage()} is the total size / the number of samples.
 * <p>
 * An example of using <code>AdvancedStatistic</code>:
<pre>
    StatisticsManager.instance().setRecordAdvancedStats(true);
    Statistic s = new AdvancedStatistic();

    s.addData(1);
    s.storeCurrentStat();
    
    s.addData(2);
    s.storeCurrentStat();
    
    s.addData(3);
    s.storeCurrentStat();
    
    for(int i = 0; i < 4; i++)
        s.incrementStat();
    s.storeCurrentStat();

    for(int i = 0; i < 5; i++)
        s.incrementStat();
    s.storeCurrentStat();

    StatisticsManager.instance().setRecordAdvancedStats(false);     
    
    //Ignored because Advanced Stats is turned off
    for(int i = 0; i < 15; i++)
        s.incrementStat();
    s.storeCurrentStat();

    s.addData(300);
    s.storeCurrentStat();               
    
    System.out.println("Total: " + s.getTotal());
    System.out.println("Max: " + s.getMax());
    System.out.println("Average: " + s.getAverage());
    
    Output:
        Total: 15.0
        Max: 5.0
        Average: 3.0
</pre> 
 * 
 */
public class AdvancedStatistic extends AbstractStatistic {

	/**
	 * Adds the statistic to the list of advanced statistics.
	 */
    public AdvancedStatistic() {
		StatisticsManager.instance().addAdvancedStatistic(this);
	}

	/**
	 * Adds the statistic to the list of advanced statistics, with
	 * a specified file name to write to.
	 *
	 * @param fileName the file name to write to
	 */
	public AdvancedStatistic(String fileName) {
		StatisticsManager.instance().addAdvancedStatistic(this);
		_fileName = fileName;
	}

	public void incrementStat() {
		// if we're not recording advanced stats, ignore the call
		if(!STATS_MANAGER.getRecordAdvancedStats()) return;
		super.incrementStat();
	}

	// override to only record
	public void addData(int data) {
		// if we're not recording advanced stats, ignore the call
		if(!STATS_MANAGER.getRecordAdvancedStats()) return;
		super.addData(data);
	}
    //override to only store if advanced stats
    @Override
    public void storeCurrentStat() {
        if(!STATS_MANAGER.getRecordAdvancedStats()) return;
        super.storeCurrentStat();
    }

}
