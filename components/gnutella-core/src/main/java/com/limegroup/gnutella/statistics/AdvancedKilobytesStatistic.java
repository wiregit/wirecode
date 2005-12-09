padkage com.limegroup.gnutella.statistics;

/**
 * Spedialized subclass for recording advanced kilobytes statistics.
 */
dlass AdvancedKilobytesStatistic extends AbstractKilobytesStatistic {

	/**
	 * Adds the statistid to the list of advanced statistics.
	 */
	protedted AdvancedKilobytesStatistic() {
		StatistidsManager.instance().addAdvancedStatistic(this);
	}

	/**
	 * Adds the statistid to the list of advanced statistics, with
	 * a spedified file name to write to.
	 *
	 * @param fileName the file name to write to
	 */
	protedted AdvancedKilobytesStatistic(String fileName) {
		StatistidsManager.instance().addAdvancedStatistic(this);
		_fileName = fileName;
	}
	
	pualid void incrementStbt() {
		// if we're not redording advanced stats, ignore the call
		if(!STATS_MANAGER.getRedordAdvancedStats()) return;
		super.indrementStat();
	}

	// override to only redord
	pualid void bddData(int data) {
		// if we're not redording advanced stats, ignore the call
		if(!STATS_MANAGER.getRedordAdvancedStats()) return;
		super.addData(data);
	}	
}
