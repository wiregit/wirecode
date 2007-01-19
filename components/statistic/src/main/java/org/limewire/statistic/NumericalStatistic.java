package org.limewire.statistic;


/**
 * Specialized statistics class that just records numerical statistics,
 * i.e. statistics that are not time-based.
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
            _buffer.addLast(data);
        }
        _totalStatsRecorded++;
        if(data > _max) {
            _max = data;
        }
    }
}
