package org.limewire.statistic;


/**
 * Abstract class that records numerical statistics, i.e. statistics that are 
 * not time-based.
 * <pre>
    class Stats extends NumericalStatistic{}       
    Statistic s = new Stats();

    s.addData(1);
    s.addData(2);
    s.addData(3);
    s.addData(4);       
    s.addData(5);
    
    System.out.println("Total: " + s.getTotal());
    System.out.println("Max: " + s.getMax());
    System.out.println("Average: " + s.getAverage());
    
    Output:
        Total: 15.0
        Max: 5.0
        Average: 3.0
 * </pre>
 */
public abstract class NumericalStatistic extends AbstractStatistic {

	/**
	 * Adds the statistic to the list of numerical statistics.
	 */
    protected NumericalStatistic() {
        StatisticsManager.instance().addNumericalStatistic(this);
    }

    /**
     * Overridden to actually write the data as a field in our buffer,
     * as opposed to waiting for some time-based event to write.
     */
    public void addData(int data) {
        super.addData(data);
        synchronized(_buffer) {
            initializeBuffer();
            _buffer.addLast((double)data);
        }
        _totalStatsRecorded++;
        if(data > _max) {
            _max = data;
        }
    }
}
