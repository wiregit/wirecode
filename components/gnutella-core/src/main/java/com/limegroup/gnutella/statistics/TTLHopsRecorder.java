package com.limegroup.gnutella.statistics;


import com.limegroup.gnutella.*;

/**
 * This class is a convenient wrapper for any class that keeps track of
 * ttl and hops for a Gnutella message.
 */
public final class TTLHopsRecorder {

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 0
	 * messages.
	 */
	public final AbstractStatHandler TTL0 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 1 
	 * messages.
	 */
	public final AbstractStatHandler TTL1 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 2
	 * messages.
	 */
	public final AbstractStatHandler TTL2 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 3
	 * messages.
	 */
	public final AbstractStatHandler TTL3 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 4
	 * messages.
	 */
	public final AbstractStatHandler TTL4 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 5
	 * messages.
	 */
	public final AbstractStatHandler TTL5 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 6
	 * messages.
	 */
	public final AbstractStatHandler TTL6 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 7
	 * messages.
	 */
	public final AbstractStatHandler TTL7 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 8
	 * messages.
	 */
	public final AbstractStatHandler TTL8 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 9
	 * messages.
	 */
	public final AbstractStatHandler TTL9 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 10
	 * messages.
	 */
	public final AbstractStatHandler TTL10 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 1 
	 * messages.
	 */
	public final AbstractStatHandler HOPS1 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 2
	 * messages.
	 */
	public final AbstractStatHandler HOPS2 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 3
	 * messages.
	 */
	public final AbstractStatHandler HOPS3 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 4
	 * messages.
	 */
	public final AbstractStatHandler HOPS4 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 5
	 * messages.
	 */
	public final AbstractStatHandler HOPS5 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 6
	 * messages.
	 */
	public final AbstractStatHandler HOPS6 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 7
	 * messages.
	 */
	public final AbstractStatHandler HOPS7 = new GeneralStatHandler();

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 8
	 * messages.
	 */
	public final AbstractStatHandler HOPS8 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 9
	 * messages.
	 */
	public final AbstractStatHandler HOPS9 = new GeneralStatHandler();


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 10
	 * messages.
	 */
	public final AbstractStatHandler HOPS10 = new GeneralStatHandler();

	public synchronized void addMessage(Message msg) {
		int ttl  = msg.getTTL();
		switch(ttl) {
		case 0: 
			TTL0.addMessage(msg);			
			break;
		case 1: 
			TTL1.addMessage(msg);
			break;
		case 2: 
			TTL2.addMessage(msg);
			break;
		case 3: 
			TTL3.addMessage(msg);
			break;
		case 4: 
			TTL4.addMessage(msg);
			break;
		case 5: 
			TTL5.addMessage(msg);
			break;
		case 6: 
			TTL6.addMessage(msg);
			break;
		case 7: 
			TTL7.addMessage(msg);
			break;
		case 8: 
			TTL8.addMessage(msg);
			break;
		case 9: 
			TTL9.addMessage(msg);
			break;
		case 10: 
			TTL10.addMessage(msg);
			break;
		}

		int hops = msg.getHops();
		switch(hops) {
		case 1: 
			HOPS1.addMessage(msg);
			break;
		case 2: 
			HOPS2.addMessage(msg);
			break;
		case 3: 
			HOPS3.addMessage(msg);
			break;
		case 4: 
			HOPS4.addMessage(msg);
			break;
		case 5: 
			HOPS5.addMessage(msg);
			break;
		case 6: 
			HOPS6.addMessage(msg);
			break;
		case 7: 
			HOPS7.addMessage(msg);
			break;
		case 8: 
			HOPS8.addMessage(msg);
			break;
		case 9: 
			HOPS9.addMessage(msg);
			break;
		case 10: 
			HOPS10.addMessage(msg);
			break;
		}
	}

}
