package com.limegroup.gnutella.spam;

/**
 * Like a KeywordToken but also holds part of the meta-data name which this
 * token belongs to, so the filter can specifically rate for example videos with
 * "Type: Adult" as spam
 */
public class XMLKeywordToken extends AbstractToken {
	private static final long serialVersionUID = 3617573808026760503L;

	/**
	 * must be positive
	 * 
	 * This value determines how dynamic the filter is. A low MAX value will
	 * allow this Token to get a bad rating after occourring only a few times in
	 * spam, a high value will make it very improbable that the filter will
	 * change its mind about a certain token without user-intervention
	 */
	private static final int MAX = 100;

	private String xmlField, keyword;

	private byte _good, _bad;
    
	XMLKeywordToken(String xmlField, String keyword) {
		_good = 90; // give every keyword initial credit
		_bad = 0;
		this.xmlField = xmlField;
        this.keyword = keyword;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public float getRating() {
		return (float) Math.pow(1.f * _bad / (_good + _bad + 1), 2);
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public void rate(Rating rating) {
		_age = 0;
		switch (rating) {
		case USER_MARKED_GOOD:
			_bad = 0;
			break;
		case USER_MARKED_SPAM:
			_bad = (byte) Math.min(10 + _bad, MAX);
			break;
		case CLEARED:
			_bad = 0;
			_good = 90;
			break;
		default:
			throw new IllegalArgumentException("unknown type of rating");
		}

		if (_good >= MAX || _bad >= MAX) {
			_good = (byte) (_good * 9 / 10);
			_bad = (byte) (_bad * 9 / 10);
		}
	}

    @Override
    public final int hashCode() {
        return xmlField.hashCode() + keyword.hashCode();
    }
    
    @Override
    public final boolean equals(Object o) {
        if(o == null)
            return false;
        if(!(o instanceof XMLKeywordToken))
            return false;
        XMLKeywordToken t = (XMLKeywordToken)o;
        return xmlField.equals(t.xmlField) && keyword.equals(t.keyword);
    }
    
	@Override
    public String toString() {
		return xmlField + "::" + keyword + " " + _good + " " + _bad;
	}
}
