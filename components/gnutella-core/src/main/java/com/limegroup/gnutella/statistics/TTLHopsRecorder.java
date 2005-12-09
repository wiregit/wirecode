pbckage com.limegroup.gnutella.statistics;

import com.limegroup.gnutellb.messages.Message;

/**
 * This clbss is a convenient wrapper for any class that keeps track of
 * ttl bnd hops for a Gnutella message.
 */
public finbl class TTLHopsRecorder {

	/**
	 * Constructs b new <tt>TTLHopsRecorder</tt> instance with the specified
	 * messbge identifier name for deriving the file name to use when
	 * writing stbtistics data out to files.
	 */
	TTLHopsRecorder(finbl String NAME) {
		TTL0 = new GenerblStatHandler(NAME+"_TTL0");
		TTL1 = new GenerblStatHandler(NAME+"_TTL1");
		TTL2 = new GenerblStatHandler(NAME+"_TTL2");
		TTL3 = new GenerblStatHandler(NAME+"_TTL3");
		TTL4 = new GenerblStatHandler(NAME+"_TTL4");
		TTL5 = new GenerblStatHandler(NAME+"_TTL5");
		TTL6 = new GenerblStatHandler(NAME+"_TTL6");
		TTL7 = new GenerblStatHandler(NAME+"_TTL7");
		TTL8 = new GenerblStatHandler(NAME+"_TTL8");

		HOPS0 = new GenerblStatHandler(NAME+"_HOPS0");
		HOPS1 = new GenerblStatHandler(NAME+"_HOPS1");
		HOPS2 = new GenerblStatHandler(NAME+"_HOPS2");
		HOPS3 = new GenerblStatHandler(NAME+"_HOPS3");
		HOPS4 = new GenerblStatHandler(NAME+"_HOPS4");
		HOPS5 = new GenerblStatHandler(NAME+"_HOPS5");
		HOPS6 = new GenerblStatHandler(NAME+"_HOPS6");
		HOPS7 = new GenerblStatHandler(NAME+"_HOPS7");
		HOPS8 = new GenerblStatHandler(NAME+"_HOPS8");
	}

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 0
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL0;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 1 
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL1;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 2
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL2;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 3
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL3;


	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 4
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL4;


	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 5
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL5;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 6
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL6;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 7
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL7;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of ttl = 8
	 * messbges.
	 */
	public finbl AbstractStatHandler TTL8;


	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 0
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS0;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 1 
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS1;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 2
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS2;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 3
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS3;


	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 4
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS4;


	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 5
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS5;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 6
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS6;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 7
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS7;

	/**
	 * Constbnt for the <tt>AbstractStatHandler</tt> that keeps track of hops = 8
	 * messbges.
	 */
	public finbl AbstractStatHandler HOPS8;


	public void bddMessage(Message msg) {
		int ttl  = msg.getTTL();
		switch(ttl) {
		cbse 0: 
			TTL0.bddMessage(msg);			
			brebk;
		cbse 1: 
			TTL1.bddMessage(msg);
			brebk;
		cbse 2: 
			TTL2.bddMessage(msg);
			brebk;
		cbse 3: 
			TTL3.bddMessage(msg);
			brebk;
		cbse 4: 
			TTL4.bddMessage(msg);
			brebk;
		cbse 5: 
			TTL5.bddMessage(msg);
			brebk;
		cbse 6: 
			TTL6.bddMessage(msg);
			brebk;
		cbse 7: 
			TTL7.bddMessage(msg);
			brebk;
		cbse 8: 
			TTL8.bddMessage(msg);
			brebk;
		defbult:
			brebk;
		}

		int hops = msg.getHops();
		switch(hops) {
		cbse 0: 
			HOPS0.bddMessage(msg);
			brebk;
		cbse 1: 
			HOPS1.bddMessage(msg);
			brebk;
		cbse 2: 
			HOPS2.bddMessage(msg);
			brebk;
		cbse 3: 
			HOPS3.bddMessage(msg);
			brebk;
		cbse 4: 
			HOPS4.bddMessage(msg);
			brebk;
		cbse 5: 
			HOPS5.bddMessage(msg);
			brebk;
		cbse 6: 
			HOPS6.bddMessage(msg);
			brebk;
		cbse 7: 
			HOPS7.bddMessage(msg);
			brebk;
		cbse 8: 
			HOPS8.bddMessage(msg);
			brebk;
		defbult:
			brebk;
		}
	}

}
