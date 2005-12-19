package com.limegroup.gnutella.spam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Set;
import java.util.Iterator;

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
	 * a map&lt;Token, Token&gt; containing all tokens.  
     * We happen to use a HashMap, but there is no need to 
     * be so restrictive in the declaration.
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
			map = new HashMap();
		}
		_tokenMap = map;

		if (LOG.isDebugEnabled())
			LOG.debug("size of tokenSet " + _tokenMap.size());

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

		if (rating > SpamManager.SPAM_THRESHOLD && rating < SpamManager.MAX_THRESHOLD)
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
	 * @return Map of <tt>Token</tt> to <tt>Token</tt> as read from disk
	 * @throws IOException
	 */
	private Map readData() throws IOException {
		Map tokens;

		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(getSpamDat())));
			tokens = (Map) is.readObject();
		} catch (ClassNotFoundException cnfe) {
			if (LOG.isDebugEnabled())
				LOG.debug(cnfe);
			return new HashMap();
		} finally {
            IOUtils.close(is);
		}
		return tokens;
	}
    
    /**
     * Save data from this table to disk.
     */
    public void save() {
        HashMap copy;
        
        synchronized(this) {
            if (_tokenMap.size() > MAX_SIZE)
                pruneEntries();
            copy = new HashMap(_tokenMap);
        }
        
        if (LOG.isDebugEnabled())
            LOG.debug("size of tokenMap " + copy.size());
        
        try {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(getSpamDat())));
                oos.writeObject(copy);
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
     * Marks that the table will be serialized to disc and not accessed for
     * a long time (i.e. LimeWire is about to get shut down)
     */
    public synchronized void ageAndSave() {
        for (Iterator iter = _tokenMap.keySet().iterator(); iter.hasNext();)
            ((Token) iter.next()).incrementAge();
        save();
    }
    
	/**
	 * check size of _tokenMap and clears old entries if necessary
	 */
	private synchronized void checkSize() {
		if (_tokenMap.size() < MAX_SIZE * 2)
			return;
		pruneEntries();
	}

	/**
	 * removes lowest importance elements from _tokenSet until there
     * are at most MAX_SIZE entries.
     * 
     * LOCKING: MUST hold monitor (synchronize) of "this" when calling 
     * this method.
	 */
	private void pruneEntries() {

		if (LOG.isDebugEnabled())
			LOG.debug("pruning unimportant entries from RatingTable");

        int tokensToRemove = _tokenMap.size() - MAX_SIZE;
        if (tokensToRemove <= 0) {
            return;
        }
        
        // Make a set of sorted tokens, low importance first
        TreeSet tokenSet = new TreeSet(_tokenMap.values());
        Iterator it = tokenSet.iterator();
        while (tokensToRemove > 0) {
            // If an exception is thrown here, there is a
            // race condition
            _tokenMap.remove(it.next());
            --tokensToRemove;
        }
	}
    
	private static File getSpamDat() {
	    return new File(CommonUtils.getUserSettingsDir(),"spam.dat");
	}
}
