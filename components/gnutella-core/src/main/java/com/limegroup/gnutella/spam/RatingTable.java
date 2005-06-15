package com.limegroup.gnutella.spam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;

public class RatingTable {
	private static final Log LOG = LogFactory.getLog(Tokenizer.class);

	/**
	 * don't hold more than this many entries * 2 and don't save more than this
	 * many entries...
	 */
	private static final int MAX_SIZE = 50000;

	private static final RatingTable INSTANCE = new RatingTable();

	/**
	 * @return single instance of this
	 */
	public static RatingTable instance() {
		return INSTANCE;
	}

	/**
	 * a map containing all tokens
	 */
	private final Map _tokenMap;

	/**
	 * constructor, tries to deserialize filter data from disc, which will fail
	 * silently, if it fails
	 */
	private RatingTable() {
		// deserialize
		Map map;
		try {
			map = readData();
		} catch (IOException ioe) {
			if (LOG.isDebugEnabled())
				LOG.debug(ioe);
			map = new TreeMap();
		}
		_tokenMap = map;

		if (LOG.isDebugEnabled())
			LOG.debug("size of tokenMap " + _tokenMap.size());

	}

	/**
	 * clears the filter data
	 */
	synchronized void clear() {
		_tokenMap.clear();
	}

	/**
	 * Returns the rating for a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc to rate
	 * @return the rating for the RemoteFileDesc
	 */
	float getRating(RemoteFileDesc desc) {
		float ret = getRating(lookup(Tokenizer.getTokens(desc)));
		if (LOG.isDebugEnabled())
			LOG.debug(desc.toString() + " rated " + ret);
		return ret;
	}

	/**
	 * Returns the cumulative rating for a RemoteFileDesc
	 * 
	 * @param tokens
	 *            an array of Token
	 * @return the cumulative rating
	 */
	float getRating(Token[] tokens) {
		float rating = 1;
		for (int i = 0; i < tokens.length && rating > 0; i++) {
			rating *= (1 - tokens[i].getRating());
		}

		rating = 1 - rating;

		if (rating > SpamManager.SPAM_THRESHOLD
				&& rating < SpamManager.MAX_THRESHOLD)
			// if rating == 1, we have encountered one really bad token.
			// don't let that f'up the ratings of all other tokens...
			markInternal(tokens, Token.RATING_SPAM);
		else if (rating <= SpamManager.GOOD_THRESHOLD)
			markInternal(tokens, Token.RATING_GOOD);

		return rating;
	}

	/**
	 * mark an array of RemoteFileDesc
	 * 
	 * @param descs
	 *            an array of RemoteFileDesc
	 * @param rating
	 *            must be a rating as defined by the Token interface
	 */
	void mark(RemoteFileDesc[] descs, int rating) {
		markInternal(lookup(Tokenizer.getTokens(descs)), rating);
	}

	/**
	 * mark a the Tokens of a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc to mark
	 * @param rating
	 *            must be a rating as defined by the Token interface
	 */
	void mark(RemoteFileDesc desc, int rating) {
		markInternal(lookup(Tokenizer.getTokens(desc)), rating);
	}

	/**
	 * mark a single QueryRequest, or rather the Tokens associated with it
	 * 
	 * @param qr
	 *            the QueryRequest to mark
	 * @param rating
	 *            must be a rating as defined by the Token interface
	 */
	void mark(QueryRequest qr, int rating) {
		markInternal(lookup(Tokenizer.getTokens(qr)), rating);
	}

	/**
	 * mark an array of Token
	 * 
	 * @param tokens
	 *            the Tokens to mark
	 * @param rating
	 *            must be a rating as defined by the Token interface
	 */
	private void markInternal(Token[] tokens, int rating) {
		for (int i = 0; i < tokens.length; i++)
			tokens[i].rate(rating);
	}

	/**
	 * Replaces all tokens with equal tokens from the _tokenMap
	 * 
	 * @param tokens
	 *            an array of Token
	 * @return an array of Token of equal length where all Tokens that are equal
	 *         to Tokens we have already seen before are replaced with the
	 *         matching Tokens we remember
	 */
	private Token[] lookup(Token[] tokens) {
		for (int i = 0; i < tokens.length; i++) {
			// lookup stored token
			tokens[i] = lookup(tokens[i]);
		}
		return tokens;
	}

	/**
	 * Replaces a Token with the copy stored in our internal _tokenMap if
	 * possible, stores the Token in the _tokenMap otherwise
	 * 
	 * @param token
	 *            the Token to look up in _tokenMap
	 * @return token or the matching copy of it from _tokenMap
	 */
	private synchronized Token lookup(Token token) {
		Token ret = (Token) _tokenMap.get(token);
		if (ret == null) {
			_tokenMap.put(token, token);
			checkSize();
			return token;
		}
		return ret;
	}

	/**
	 * read data from disk
	 * 
	 * @return Map of <tt>Token</tt> as read from disk
	 * @throws IOException
	 */
	private Map readData() throws IOException {
		TreeMap tokens;

		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(getSpamDat())));
			tokens = (TreeMap) is.readObject();
		} catch (ClassNotFoundException cnfe) {
			if (LOG.isDebugEnabled())
				LOG.debug(cnfe);
			return new TreeMap();
		} finally {
            IOUtils.close(is);
		}
		return tokens;
	}
    
    /**
     * Save data from this table to disk.
     */
    public synchronized void save() {
        
        if (LOG.isDebugEnabled())
            LOG.debug("size of tokenMap " + _tokenMap.size());
        
        try {
            // this blocks a while, may do it off-thread...
            if (_tokenMap.size() > MAX_SIZE)
                clearOldEntries();
            
            for (Iterator iter = _tokenMap.keySet().iterator(); iter.hasNext();)
                ((Token) iter.next()).age();
            
            File spamFile = getSpamDat();
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(getSpamDat())));
                oos.writeObject(_tokenMap);
                oos.flush();
            } finally {
                IOUtils.close(oos);
            }
        
        } catch (IOException iox) {
            if (LOG.isDebugEnabled())
                LOG.debug("saving rating table failed", iox);
        }
	}
    
	/**
	 * check size of _tokenMap and clears old entries if necessary
	 */
	private synchronized void checkSize() {
		if (_tokenMap.size() < MAX_SIZE * 2)
			return;
		clearOldEntries();
	}

	/**
	 * removes old entries from the _tokenMap
	 */
	private void clearOldEntries() {

		if (LOG.isDebugEnabled())
			LOG.debug("clearing ratingtable from old entries");

		ArrayList list = new ArrayList();
		list.addAll(_tokenMap.keySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Token) o1).getAge() - ((Token) o2).getAge();
			}
		});

		_tokenMap.clear();

		int i = 0;
		for (Iterator iter = list.iterator(); iter.hasNext() && i < MAX_SIZE; i++) {
			Object o = iter.next();
			_tokenMap.put(o, o);
		}
	}
    
	private static File getSpamDat() {
	    return new File(CommonUtils.getUserSettingsDir(),"spam.dat");
	}
}
