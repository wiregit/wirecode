package org.limewire.statistic;

/**
 * Specialized subclass for recording advanced data in kilobits instead of bytes.
 * In order to preserve data accuracy, data is stored in bytes and converted to 
 * kilobits; otherwise, data would be lost.
 * Advanced statistics are only recorded if the Singleton record advanced 
 * statistics flag (within {@link StatisticsManager}) is on. For example, to 
 * check and turn on the flag if off:
<pre>
   if(! StatisticsManager.instance().getRecordAdvancedStats())
        StatisticsManager.instance().setRecordAdvancedStats(true);
</pre>
 * <p>
 * {@link #getMax()} and {@link #getAverage()} are effected by 
 * {@link #storeCurrentStat()}. 
 * <p>
 * For example, if you make four calls of <code>addData(1024)</code> but only 
 * make one call to <code>storeCurrentStat</code>, then: <br>
<pre>
       average = total (4 K) / storeCurrentStat calls (1) = 4 K,
       max = 1024 + 1024 + 1024 + 1024 = 4 K. 
</pre>
 * <p>
 * However if you make three <code>addData(1024)</code> calls, a call to
 * <code>storeCurrentStat</code>, and then another <code>addData(1024)</code> followed
 * by <code>storeCurrentStat</code>, the total is still 4 K, but the average 
 * and max is different: <br>
<pre>
       average = total (4 K) / storeCurrentStat calls (2) = 2 K,
       max = 1024 + 1024 + 1024 = 3 K.
 </pre>
 * An example of using <code>AdvancedKilobytesStatistic</code>:
<pre>
   if(! StatisticsManager.instance().getRecordAdvancedStats())
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
    public void storeCurrentStat() {
//      if we're not recording advanced stats, ignore the call
        if(!STATS_MANAGER.getRecordAdvancedStats()) return;
        super.storeCurrentStat();
    }

}
