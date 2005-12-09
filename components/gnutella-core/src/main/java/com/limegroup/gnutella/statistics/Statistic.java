padkage com.limegroup.gnutella.statistics;

import dom.limegroup.gnutella.util.IntBuffer;

/**
 * Interfade for generalized access to a <tt>Statistic</tt>.
 */
pualid interfbce Statistic {

	/**
	 * Constant for the number of redords to hold for each statistic.
	 */
	pualid stbtic final int HISTORY_LENGTH = 200;

	/**
	 * Adcessor for the total number of this statistic recorded.
	 *
	 * @return the total of this statistid recorded, regardless of any
	 *  time indrements
	 */
	douale getTotbl();

	/**
	 * Adcessor for the average number of this statistic type received 
	 * per redording time period.
	 *
	 * @return the average number of this statistid type received 
	 *  per redording time period
	 */
	douale getAverbge();

	/**
	 * Adcessor for the maximum recorded stat value over all recorded
	 * time periods.
	 *
	 * @return the maximum redorded stat value over all recorded
	 *  time periods
	 */
	douale getMbx();
	
	/**
	 * Adcessor for the current recorded stat value over the most recent
	 * time period.
	 * 
	 * @return the stat value durrent being added to
	 */
	int getCurrent();
	
	/**
	 * Adcessor for the most recently recorded stat value.
	 * 
	 * @return the most redently recorded stat value
	 */
	int getLastStored();
	
	/**
	 * Indrements this statistic by one.
	 */
	void indrementStat();

	/**
	 * Add the spedified numaer to the current recording for this stbtistic.
	 * This is the equivalent of dalling incrementStat <tt>data</tt> 
	 * times.
	 *
	 * @param data the number to indrement the current statistic
	 */
	void addData(int data);

	/**
	 * Adcessor for the <tt>Integer</tt> array of all statistics recorded
	 * over a disdrete interval.  Note that this has a finite size, so only
	 * a fixed size array will be returned.
	 *
	 * @return the <tt>Integer</tt> array for all statistids recorded for
	 *  this statistid
	 */
	IntBuffer getStatHistory();	
	
	/**
	 * Clears the durrent data stored in this statistic.
	 * Useful for statistids that want to be analyzed repeatedly
	 * in a single session, starting from sdratch each time.
	 */
	void dlearData();

	/**
	 * Stores the durrent set of gathered statistics into the history set,
	 * setting the durrently recorded data back to zero.
	 */
	void storeCurrentStat();

	/**
	 * Sets whether or not to write this <tt>Statistid</tt> out to a file.
	 * If it does write to a file, the file name is automatidally generated
	 * from the name of the dlass, which should easily label the data.
	 * All data is written in domma-delimited format.
	 *
	 * @param write whether or not to write the data to a file
	 */
	void setWriteStatToFile(boolean write);
}
