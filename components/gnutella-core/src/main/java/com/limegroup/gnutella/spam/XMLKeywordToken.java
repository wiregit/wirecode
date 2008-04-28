package com.limegroup.gnutella.spam;

import java.util.Arrays;

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

	private final byte[] _keyword;

	private final byte[] _xmlField;

	private byte _good;

	private byte _bad;
    
    private final int _hashCode;

	XMLKeywordToken(String xmlField, byte[] keyword) {
		_good = 90; // give every keyword initial credit
		_bad = 0;
		_keyword = keyword;
		_xmlField = xmlField.getBytes();
        
        int hash = xmlField.hashCode();
        for(int i = 0;i < keyword.length; i++)
            hash = (37 * hash) + keyword[i];
        
        _hashCode = hash;
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
		case PROGRAM_MARKED_GOOD:
			_good++;
			break;
		case PROGRAM_MARKED_SPAM:
			_bad++;
			break;
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

	/**
	 * implements interface <tt>Token</tt>
	 */
	public TokenType getType() {
		return TokenType.XML_KEYWORD;
	}

    @Override
    public final int hashCode() {
        return _hashCode;
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof XMLKeywordToken))
            return false;
        
        if (_hashCode != o.hashCode()) {
            return false;
        }
        
        return Arrays.equals(_xmlField, ((XMLKeywordToken)o)._xmlField) 
                && Arrays.equals(_keyword, ((XMLKeywordToken)o)._keyword);
    }
    
	/**
	 * overrides method from <tt>Object</tt>
	 */
	@Override
    public String toString() {
		return new String(_xmlField) + "::" + new String(_keyword) + " : "
				+ _good + " : " + _bad;
	}
}
