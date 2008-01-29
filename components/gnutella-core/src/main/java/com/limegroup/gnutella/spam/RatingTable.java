package com.limegroup.gnutella.spam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.spam.Token.Rating;

@Singleton
public class RatingTable {
	private static final Log LOG = LogFactory.getLog(Tokenizer.class);

	/**
	 * don't hold more than this many entries * 2 and don't save more than this
	 * many entries...
	 */
	private static final int MAX_SIZE = 50000;
    
	/**
	 * a Map containing all tokens.
     * 
     * Although we stored the data as Token -> Token,
     * this is by design (and purposely not a Set), so that
     * we can retrieve the stored value from the Set, using
     * a Token as an identifier.  This way a third-party can
     * create a blank Token object without rating data and ask 
     * the RatingTable to return the actual Token that should
     * be used inplace of that Token (one that has rating data).
	 */
	private final Map<Token, Token> _tokenMap;
	
	private final Tokenizer tokenizer;
	
	/**
	 * constructor, tries to deserialize filter data from disc, which will fail
	 * silently, if it fails
	 */
	@Inject
	RatingTable(Tokenizer tokenizer) {
	    this.tokenizer = tokenizer;
	    
		// deserialize
		_tokenMap = readData();
		
		for(Token token : _tokenMap.values())
            tokenizer.initialize(token);

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
		float ret = getRating(lookup(tokenizer.getTokens(desc)));
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

        float bad = SearchSettings.FILTER_SPAM_RESULTS.getValue();
        if (rating >= bad && rating <= SpamManager.MAX_THRESHOLD)
            markInternal(tokens, Rating.PROGRAM_MARKED_SPAM);
        else if (rating <= 1f - bad)
            markInternal(tokens, Rating.PROGRAM_MARKED_GOOD);

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
	void mark(RemoteFileDesc[] descs, Rating rating) {
		markInternal(lookup(tokenizer.getTokens(descs)), rating);
	}

	/**
	 * mark a the Tokens of a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc to mark
	 * @param rating
	 *            must be a rating as defined by the Token interface
	 */
	void mark(RemoteFileDesc desc, Rating rating) {
		markInternal(lookup(tokenizer.getTokens(desc)), rating);
	}

	/**
	 * mark a single QueryRequest, or rather the Tokens associated with it
	 * 
	 * @param qr
	 *            the QueryRequest to mark
	 * @param rating
	 *            must be a rating as defined by the Token interface
	 */
	void mark(QueryRequest qr, Rating rating) {
		markInternal(lookup(tokenizer.getTokens(qr)), rating);
	}

	/**
	 * mark an array of Token
	 * 
	 * @param tokens
	 *            the Tokens to mark
	 * @param rating
	 *            must be a rating as defined by the Token interface
	 */
	private void markInternal(Token[] tokens, Rating rating) {
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
        Token stored = _tokenMap.get(token);
        
        if(stored == null) {
            _tokenMap.put(token, token);
            checkSize();
            stored = token;
        }
        
        return stored;
	}

	/**
	 * read data from disk
	 * 
	 * @return Map of <tt>Token</tt> to <tt>Token</tt> as read from disk
	 */
	private Map<Token, Token> readData() {
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(getSpamDat())));
            return GenericsUtils.scanForMap(is.readObject(), Token.class, Token.class, GenericsUtils.ScanMode.REMOVE);
		} catch(Throwable someKindOfError) {
			return new HashMap<Token, Token>();
		} finally {
            IOUtils.close(is);
		}
	}
    
    /**
     * Save data from this table to disk.
     */
    public void save() {
        Map<Token, Token> copy;
        
        synchronized(this) {
            if (_tokenMap.size() > MAX_SIZE)
                pruneEntries();
            copy = new HashMap<Token, Token>(_tokenMap);
        }
        
        if (LOG.isDebugEnabled())
            LOG.debug("size of tokenMap " + copy.size());

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(getSpamDat())));
            oos.writeObject(copy);
            oos.flush();
        } catch (IOException iox) {
            if (LOG.isDebugEnabled())
                LOG.debug("saving rating table failed", iox);
        } finally {
            IOUtils.close(oos);
        }
	}
    
    /**
     * Marks that the table will be serialized to disc and not accessed for a long time (i.e. LimeWire is about to get
     * shut down)
     */
    public synchronized void ageAndSave() {
        for(Token token : _tokenMap.values()) 
            token.incrementAge();
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
        Set<Token> sortedTokens = new TreeSet<Token>(_tokenMap.values());
        for(Token token : sortedTokens) {
            // Note: Although we are iterating over the sorted values or the map,
            // and then removing from it using those items (as opposed to the keys),
            // this works fine because the Map stores the same element in key/value.
            _tokenMap.remove(token);
            --tokensToRemove;
            if(tokensToRemove == 0)
                break;
        }
	}
    
	private static File getSpamDat() {
	    return new File(CommonUtils.getUserSettingsDir(),"spam.dat");
	}
    
   
        
	/** Inspectable that returns a hash and rating of the tokens */
	@InspectionPoint("spam rating table token hashes")
	@SuppressWarnings("unused")
	private final Inspectable TOKEN_HASH = new Inspectable() {
	    public Object inspect() {
	        synchronized(RatingTable.this) {
	            Map<String, Object> ret = new HashMap<String, Object>();
	            ret.put("ver",1);
	            final float spamTreshold = Math.max(SearchSettings.FILTER_SPAM_RESULTS.getValue(),
	                    SearchSettings.QUERY_SPAM_CUTOFF.getValue());
	            ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            DataOutputStream daos = new DataOutputStream(baos);
	            try {
	                for (Token t : _tokenMap.values()) {
	                    // 8 bytes per entry
	                    float rating = t.getRating();
	                    if (rating < spamTreshold)
	                        break;
	                    daos.writeFloat(rating);
	                    daos.writeInt(t.hashCode());
	                }
	                daos.flush();
	                daos.close();
	                ret.put("dump", baos.toByteArray());
	            } catch (IOException impossible) {
	                ret.put("error", impossible.toString());
	            }
	            return ret;
	        }
	    }
	};
}
