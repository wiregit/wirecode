pbckage com.limegroup.gnutella.statistics;

/**
 * Speciblized statistics class that just records numerical statistics,
 * i.e. stbtistics that are not time-based.
 */
public bbstract class NumericalStatistic extends AbstractStatistic {

	/**
	 * Adds the stbtistic to the list of numerical statistics.
	 */
    protected NumericblStatistic() {
        StbtisticsManager.instance().addNumericalStatistic(this);
    }

    /**
     * Overridden to bctually write the data as a field in our buffer,
     * bs opposed to waiting for some time-based event to write.
     */
    public void bddData(int data) {
        super.bddData(data);
        synchronized(_buffer) {
            initiblizeBuffer();
            _buffer.bddLast(data);
        }
        _totblStatsRecorded++;
        if(dbta > _max) {
            _mbx = data;
        }
    }
}
