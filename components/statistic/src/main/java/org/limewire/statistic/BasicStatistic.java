package org.limewire.statistic;


/**
 * Abstract base class that records basic statistics. 
 * <p>
 * {@link #getMax()} and {@link #getAverage()} are effected by 
 * {@link #storeCurrentStat()}. 
 * <p>
 * For example, if you make four calls of <code>addData(1024)</code> but only 
 * make one call to <code>storeCurrentStat</code>, then: <br>
<pre>
       average = total (4 K) / storeCurrentStat calls (1) = 4 K,
       max = 1024 * 4 = 4 K. 
</pre>
 * <p>
 * However if you make three <code>addData(1024)</code> calls, a call to
 * <code>storeCurrentStat</code>, and then another <code>addData(1024)</code> followed
 * by <code>storeCurrentStat</code>, the total is still 4 K, but the average 
 * and max is different: <br>
<pre>
       average = total (4 K) / storeCurrentStat calls (2) = 2 K,
       max = 1 + 1 + 1 = 3 K.
 </pre>
 * An example of using <code>BasicStatistic</code>:
 * <pre>
    class Stats extends BasicStatistic{}       
    Statistic s = new Stats();

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
            
    System.out.println("Total: " + s.getTotal());
    System.out.println("Max: " + s.getMax());
    System.out.println("Average: " + s.getAverage());

    Output:
        Total: 15.0
        Max: 5.0
        Average: 3.0
 * </pre>
 */
public abstract class BasicStatistic extends AbstractStatistic {

	protected BasicStatistic() {
		StatisticsManager.instance().addBasicStatistic(this);
	}
}
