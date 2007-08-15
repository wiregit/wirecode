package org.limewire.statistic;


/**
 * Specialized subclass for recording advanced statistics.
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
 * For example, if you make four calls of <code>addData(1)</code> but only 
 * make one call to <code>storeCurrentStat</code>, then: <br>
<pre>
       average = total (4) / storeCurrentStat calls (1) = 4,
       max = 1 + 1 + 1 + 1 = 4. 
</pre>
 * <p>
 * However if you make three <code>addData(1)</code> calls, a call to
 * <code>storeCurrentStat</code>, and then another <code>addData(1)</code> followed
 * by <code>storeCurrentStat</code>, the total is still 4, but the average 
 * and max is different: <br>
<pre>
       average = total (4) / storeCurrentStat calls (2) = 2,
       max = 1 + 1 + 1 = 3.
 </pre>
 * An example of using <code>AdvancedStatistic</code>:
<pre>
    if(! StatisticsManager.instance().getRecordAdvancedStats())
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
    public void storeCurrentStat() {
        if(!STATS_MANAGER.getRecordAdvancedStats()) return;
        super.storeCurrentStat();
    }

}
