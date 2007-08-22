package org.limewire.statistic;

/**
 * Specialized subclass for recording advanced data in kilobytes instead of bytes.
 * In order to preserve data accuracy, data is stored in bytes and converted to 
 * kilobytes; otherwise, data would be lost. Advanced statistics are only recorded if 
 * {@link StatisticsManager StatisticsManager.instance().setRecordAdvancedStats(true)}
 * is called. 
 * <p>
 * A sample, aka a cycle of data, is all the data collected between calls to 
 * {@link #storeCurrentStat()}. 
 * Therefore, the {@link #getMax()} is the largest sample and the 
 * {@link #getAverage()} is the total size / the number of samples.
 * <p>
 * An example of using <code>AdvancedKilobytesStatistic</code>:
<pre>
    StatisticsManager.instance().setRecordAdvancedStats(true);
    Statistic s = new AdvancedKilobytesStatistic();
            
    for(int i = 0; i < 1024; i++)
        s.incrementStat();
    s.storeCurrentStat();

    s.addData(1024 * 2);
    s.storeCurrentStat();
    
    s.addData(1024 * 3);
    s.storeCurrentStat();
    
    s.addData(1024 * 4);
    s.storeCurrentStat();
    
    s.addData(1024 * 5);
    s.storeCurrentStat();

    StatisticsManager.instance().setRecordAdvancedStats(false);

    //Ignored because Advanced Stats is turned off
    s.addData(1024 * 50);
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
public class AdvancedKilobytesStatistic extends AbstractKilobytesStatistic {

	/**
	 * Adds the statistic to the list of advanced statistics.
	 */
    public AdvancedKilobytesStatistic() {
		StatisticsManager.instance().addAdvancedStatistic(this);
	}

	/**
	 * Adds the statistic to the list of advanced statistics, with
	 * a specified file name to write to.
	 *
	 * @param fileName the file name to write to
	 */
    public AdvancedKilobytesStatistic(String fileName) {
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
//      if we're not recording advanced stats, ignore the call
        if(!STATS_MANAGER.getRecordAdvancedStats()) return;
        super.storeCurrentStat();
    }

}
