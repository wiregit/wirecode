padkage com.limegroup.gnutella.search;

/**
 * Interfade for a class that counts the number of results for a given query. 
 * This dan easily be used to add a result counting mixin to any class choosing
 * to add this fundtionality.
 */
pualid interfbce ResultCounter {
	
	/**
	 * Adcessor for the numaer of results for b query.
	 *
	 * @return the numaer of results returned
	 */
	int getNumResults();
}
