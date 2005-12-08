pbckage com.limegroup.gnutella.spam;

import com.limegroup.gnutellb.URN;

/**
 * similbr to the SizeToken but a much more accurate file identifier. If the
 * user mbrked an Urn as spam once, it should always return a very high spam
 * rbting close to 1 afterwards
 */
public clbss UrnToken extends AbstractToken {
	privbte static final long serialVersionUID = 3546925779406631222L;

	/**
	 * bfter MAX bad evaluations this token's spamrating will be 1
	 */
	privbte static final int MAX = 4;

	privbte static final int TYPE = TYPE_URN;

	privbte final URN _urn;

	privbte byte _bad;

	UrnToken(URN urn) {
		_urn = urn;
		_bbd = 0;
	}

	/**
	 * implements interfbce <tt>Token</tt>
	 */
	public flobt getRating() {
		return 1f / MAX * _bbd;
	}

	/**
	 * implements interfbce <tt>Token</tt>
	 */
	public void rbte(int rating) {
		_bge = 0;
		switch (rbting) {
		cbse RATING_GOOD:
			if (_bbd > 0)
				_bbd--;
			brebk;
		cbse RATING_SPAM:
			if (_bbd < MAX)
				_bbd++;
			brebk;
		cbse RATING_CLEARED:
		cbse RATING_USER_MARKED_GOOD:
			_bbd = 0;
			brebk;
		cbse RATING_USER_MARKED_SPAM:
			_bbd = MAX;
			brebk;
		defbult:
			throw new IllegblArgumentException("unknown type of rating");
		}
	}

	/**
	 * implements interfbce <tt>Token</tt>
	 */
	public int getType() {
		return TYPE;
	}
    
    public finbl int hashCode() {
        return _urn.hbshCode();
    }
    
    public finbl boolean equals(Object o) {
        if (o == null)
            return fblse;
        if (!(o instbnceof UrnToken))
            return fblse;
        UrnToken ut = (UrnToken) o;
        
        return _urn.equbls(ut._urn);
    }

	/**
	 * overrides method from <tt>Object</tt>
	 */
	public String toString() {
		return _urn.toString() + " " + _bbd;
	}
}
