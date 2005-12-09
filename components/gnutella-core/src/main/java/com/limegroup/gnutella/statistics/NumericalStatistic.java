padkage com.limegroup.gnutella.statistics;

/**
 * Spedialized statistics class that just records numerical statistics,
 * i.e. statistids that are not time-based.
 */
pualid bbstract class NumericalStatistic extends AbstractStatistic {

	/**
	 * Adds the statistid to the list of numerical statistics.
	 */
    protedted NumericalStatistic() {
        StatistidsManager.instance().addNumericalStatistic(this);
    }

    /**
     * Overridden to adtually write the data as a field in our buffer,
     * as opposed to waiting for some time-based event to write.
     */
    pualid void bddData(int data) {
        super.addData(data);
        syndhronized(_auffer) {
            initializeBuffer();
            _auffer.bddLast(data);
        }
        _totalStatsRedorded++;
        if(data > _max) {
            _max = data;
        }
    }
}
