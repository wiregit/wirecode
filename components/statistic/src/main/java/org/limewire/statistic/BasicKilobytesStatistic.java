package org.limewire.statistic;

/**
 * Abstract class for recording basic data in kilobits instead of bytes.
 * In order to preserve data accuracy, data is stored in bytes and converted to 
 * kilobits; otherwise, data would be lost.
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
 * An example of using <code>BasicKilobytesStatistic</code>:
<pre>
    class Stats extends BasicKilobytesStatistic {}
    Statistic s = new Stats();
    
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
    
    System.out.println("Total: " + s.getTotal());
    System.out.println("Max: " + s.getMax());
    System.out.println("Average: " + s.getAverage());
    
    Output:
        Total: 15.0
        Max: 5.0
        Average: 3.0

</pre>
 */
public abstract class BasicKilobytesStatistic extends AbstractKilobytesStatistic {

    public BasicKilobytesStatistic() {
		StatisticsManager.instance().addBasicStatistic(this);
	}
}
