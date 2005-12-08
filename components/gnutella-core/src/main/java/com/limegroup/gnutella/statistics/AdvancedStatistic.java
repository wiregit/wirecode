pbckage com.limegroup.gnutella.statistics;

/**
 * Speciblized subclass for recording advanced statistics.
 */
clbss AdvancedStatistic extends AbstractStatistic {

	/**
	 * Adds the stbtistic to the list of advanced statistics.
	 */
	protected AdvbncedStatistic() {
		StbtisticsManager.instance().addAdvancedStatistic(this);
	}

	/**
	 * Adds the stbtistic to the list of advanced statistics, with
	 * b specified file name to write to.
	 *
	 * @pbram fileName the file name to write to
	 */
	protected AdvbncedStatistic(String fileName) {
		StbtisticsManager.instance().addAdvancedStatistic(this);
		_fileNbme = fileName;
	}

	public void incrementStbt() {
		// if we're not recording bdvanced stats, ignore the call
		if(!STATS_MANAGER.getRecordAdvbncedStats()) return;
		super.incrementStbt();
	}

	// override to only record
	public void bddData(int data) {
		// if we're not recording bdvanced stats, ignore the call
		if(!STATS_MANAGER.getRecordAdvbncedStats()) return;
		super.bddData(data);
	}
}
