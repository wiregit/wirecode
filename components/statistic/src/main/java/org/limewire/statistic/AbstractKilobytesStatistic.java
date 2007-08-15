package org.limewire.statistic;

/**
 * Abstract class for recording data in kilobits instead of bytes. In order to 
 * preserve data accuracy, data is stored in bytes and converted to kilobits; 
 * otherwise, data would be lost.
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
 * An example of using <code>AbstractKilobytesStatistic</code>:
 <pre>
    class Stats extends AbstractKilobytesStatistic {}
    Statistic s = new Stats();
            
    for(int i = 0; i < 1024; i++)
        s.incrementStat();
    s.storeCurrentStat();
    
    s.addData(2 * 1024);
    s.storeCurrentStat();
    
    s.addData(3 * 1024);
    s.storeCurrentStat();
    
    s.addData(4 * 1024);
    s.storeCurrentStat();

    s.addData(5 * 1024);
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
 * 
 */
public abstract class AbstractKilobytesStatistic extends AbstractStatistic {

	/**
	 * Bytes per kilobyte for conversion convenience.
	 */
	private static final int BYTES_PER_KILOBYTE = 1024;

	/** 
	 * Overridden to report the average for this statistic in kilobyes.
	 *
	 * @return the average for this statistic in kilobytes per unit of
	 *  measurement (KB/s)
	 */
	public double getAverage() {
		if(_totalStatsRecorded == 0) return 0;
		return (_total/_totalStatsRecorded)/BYTES_PER_KILOBYTE;
	}

	/** 
	 * Overridden to report the maximum for this statistic in kilobyes.
	 *
	 * @return the maximum for a recorded time period for this statistic 
	 *  in kilobytes 
	 */
	public double getMax() {
		return _max/BYTES_PER_KILOBYTE;
	}

	/** 
	 * Overridden to report the total for this statistic in kilobytes.
	 *
	 * @return the total for this statistic in kilobytes 
	 */
	public double getTotal() {
		return _total/BYTES_PER_KILOBYTE;
	}
}
