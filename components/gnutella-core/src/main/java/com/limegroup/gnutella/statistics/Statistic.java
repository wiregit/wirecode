pbckage com.limegroup.gnutella.statistics;

import com.limegroup.gnutellb.util.IntBuffer;

/**
 * Interfbce for generalized access to a <tt>Statistic</tt>.
 */
public interfbce Statistic {

	/**
	 * Constbnt for the number of records to hold for each statistic.
	 */
	public stbtic final int HISTORY_LENGTH = 200;

	/**
	 * Accessor for the totbl number of this statistic recorded.
	 *
	 * @return the totbl of this statistic recorded, regardless of any
	 *  time increments
	 */
	double getTotbl();

	/**
	 * Accessor for the bverage number of this statistic type received 
	 * per recording time period.
	 *
	 * @return the bverage number of this statistic type received 
	 *  per recording time period
	 */
	double getAverbge();

	/**
	 * Accessor for the mbximum recorded stat value over all recorded
	 * time periods.
	 *
	 * @return the mbximum recorded stat value over all recorded
	 *  time periods
	 */
	double getMbx();
	
	/**
	 * Accessor for the current recorded stbt value over the most recent
	 * time period.
	 * 
	 * @return the stbt value current being added to
	 */
	int getCurrent();
	
	/**
	 * Accessor for the most recently recorded stbt value.
	 * 
	 * @return the most recently recorded stbt value
	 */
	int getLbstStored();
	
	/**
	 * Increments this stbtistic by one.
	 */
	void incrementStbt();

	/**
	 * Add the specified number to the current recording for this stbtistic.
	 * This is the equivblent of calling incrementStat <tt>data</tt> 
	 * times.
	 *
	 * @pbram data the number to increment the current statistic
	 */
	void bddData(int data);

	/**
	 * Accessor for the <tt>Integer</tt> brray of all statistics recorded
	 * over b discrete interval.  Note that this has a finite size, so only
	 * b fixed size array will be returned.
	 *
	 * @return the <tt>Integer</tt> brray for all statistics recorded for
	 *  this stbtistic
	 */
	IntBuffer getStbtHistory();	
	
	/**
	 * Clebrs the current data stored in this statistic.
	 * Useful for stbtistics that want to be analyzed repeatedly
	 * in b single session, starting from scratch each time.
	 */
	void clebrData();

	/**
	 * Stores the current set of gbthered statistics into the history set,
	 * setting the currently recorded dbta back to zero.
	 */
	void storeCurrentStbt();

	/**
	 * Sets whether or not to write this <tt>Stbtistic</tt> out to a file.
	 * If it does write to b file, the file name is automatically generated
	 * from the nbme of the class, which should easily label the data.
	 * All dbta is written in comma-delimited format.
	 *
	 * @pbram write whether or not to write the data to a file
	 */
	void setWriteStbtToFile(boolean write);
}
