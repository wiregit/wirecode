package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.messages.Message;

/**
 * This class is a convenient wrapper for any class that keeps track of
 * ttl and hops for a Gnutella message.
 */
public final class TTLHopsRecorder {

	/**
	 * Constructs a new <tt>TTLHopsRecorder</tt> instance with the specified
	 * message identifier name for deriving the file name to use when
	 * writing statistics data out to files.
	 */
	TTLHopsRecorder(final String NAME) {
		TTL0 = new GeneralStatHandler(NAME+"_TTL0");
		TTL1 = new GeneralStatHandler(NAME+"_TTL1");
		TTL2 = new GeneralStatHandler(NAME+"_TTL2");
		TTL3 = new GeneralStatHandler(NAME+"_TTL3");
		TTL4 = new GeneralStatHandler(NAME+"_TTL4");
		TTL5 = new GeneralStatHandler(NAME+"_TTL5");
		TTL6 = new GeneralStatHandler(NAME+"_TTL6");
		TTL7 = new GeneralStatHandler(NAME+"_TTL7");
		TTL8 = new GeneralStatHandler(NAME+"_TTL8");

		HOPS0 = new GeneralStatHandler(NAME+"_HOPS0");
		HOPS1 = new GeneralStatHandler(NAME+"_HOPS1");
		HOPS2 = new GeneralStatHandler(NAME+"_HOPS2");
		HOPS3 = new GeneralStatHandler(NAME+"_HOPS3");
		HOPS4 = new GeneralStatHandler(NAME+"_HOPS4");
		HOPS5 = new GeneralStatHandler(NAME+"_HOPS5");
		HOPS6 = new GeneralStatHandler(NAME+"_HOPS6");
		HOPS7 = new GeneralStatHandler(NAME+"_HOPS7");
		HOPS8 = new GeneralStatHandler(NAME+"_HOPS8");
	}

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 0
	 * messages.
	 */
	public final AbstractStatHandler TTL0;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 1 
	 * messages.
	 */
	public final AbstractStatHandler TTL1;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 2
	 * messages.
	 */
	public final AbstractStatHandler TTL2;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 3
	 * messages.
	 */
	public final AbstractStatHandler TTL3;


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 4
	 * messages.
	 */
	public final AbstractStatHandler TTL4;


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 5
	 * messages.
	 */
	public final AbstractStatHandler TTL5;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 6
	 * messages.
	 */
	public final AbstractStatHandler TTL6;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 7
	 * messages.
	 */
	public final AbstractStatHandler TTL7;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 8
	 * messages.
	 */
	public final AbstractStatHandler TTL8;


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 0
	 * messages.
	 */
	public final AbstractStatHandler HOPS0;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 1 
	 * messages.
	 */
	public final AbstractStatHandler HOPS1;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 2
	 * messages.
	 */
	public final AbstractStatHandler HOPS2;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 3
	 * messages.
	 */
	public final AbstractStatHandler HOPS3;


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 4
	 * messages.
	 */
	public final AbstractStatHandler HOPS4;


	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 5
	 * messages.
	 */
	public final AbstractStatHandler HOPS5;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 6
	 * messages.
	 */
	public final AbstractStatHandler HOPS6;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 7
	 * messages.
	 */
	public final AbstractStatHandler HOPS7;

	/**
	 * Constant for the <tt>AbstractStatHandler</tt> that keeps track of hops = 8
	 * messages.
	 */
	public final AbstractStatHandler HOPS8;


	public void addMessage(Message msg) {
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
		default:
			break;
		}

		int hops = msg.getHops();
		switch(hops) {
		case 0: 
			HOPS0.addMessage(msg);
			break;
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
		default:
			break;
		}
	}

}
