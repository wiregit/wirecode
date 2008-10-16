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
import org.limewire.core.settings.SearchSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.Service;
import org.limewire.util.CommonUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.spam.Token.Rating;

@Singleton
public class RatingTable implements Service {
	private static final Log LOG = LogFactory.getLog(Tokenizer.class);

	/**
	 * Don't hold more than twice this many entries in memory and don't save
     * more than this many entries to disk
	 */
	private static final int MAX_SIZE = 50000;
    
	/**
	 * A Map containing a limited number of Tokens. We use a Map rather than
     * a Set so that we can retrieve a stored Token by using an equivalent
     * Token as a key. This allows us to use a blank Token without rating
     * data to retrieve an equivalent Token that has rating data.
	 */
	private final Map<Token, Token> _tokenMap = new HashMap<Token, Token>();
	
	private final Tokenizer tokenizer;
	
	@Inject
	RatingTable(Tokenizer tokenizer) {
	    this.tokenizer = tokenizer;
	}
	
	@Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
	
	public String getServiceName() {
	    return org.limewire.i18n.I18nMarker.marktr("Spam Management");
	}
	
	public void initialize() {
	}
	
	public synchronized void start() {
        _tokenMap.putAll(load());
        if(LOG.isDebugEnabled())
            LOG.debug(_tokenMap.size() + " tokens loaded");
    }
	
	/**
	 * Clears the filter data
	 */
	protected synchronized void clear() {
        LOG.debug("Clearing ratings");
		_tokenMap.clear();
	}

	/**
	 * Returns the rating for a RemoteFileDesc
	 * 
	 * @param desc the RemoteFileDesc to rate
	 * @return the rating for the RemoteFileDesc
	 */
	protected synchronized float getRating(RemoteFileDesc desc) {
		float rating = getRating(lookup(tokenizer.getTokens(desc)));
		if(LOG.isDebugEnabled())
			LOG.debug(desc + " rated " + rating);
		return rating;
	}

	/**
	 * Returns the rating for a tokenized RemoteFileDesc
	 * 
	 * @param tokens an array of Tokens taken from a RemoteFileDesc
	 * @return the rating for the RemoteFileDesc
	 */
    private float getRating(Token[] tokens) {
        float rating = 1;
        for(Token t : tokens)
            rating *= 1 - t.getRating();
        return 1 - rating;
    }

	/**
	 * Assigns the given rating to an array of RemoteFileDescs
	 * 
	 * @param descs an array of RemoteFileDescs to be rated
	 * @param rating a rating as defined by the Token interface
	 */
	protected synchronized void rate(RemoteFileDesc[] descs, Rating rating) {
		rateInternal(lookup(tokenizer.getTokens(descs)), rating);
	}

	/**
     * Clears the ratings of the tokens associated with a QueryRequest
     * 
	 * @param qr the QueryRequest to clear
	 */
	protected synchronized void clear(QueryRequest qr) {
		rateInternal(lookup(tokenizer.getTokens(qr)), Rating.CLEARED);
	}

	/**
	 * Assigns the given rating to an array of tokens
	 * 
	 * @param tokens the Tokens to rate
	 * @param rating a rating as defined by the Token interface
	 */
	private void rateInternal(Token[] tokens, Rating rating) {
		for(Token t : tokens) {
            if(LOG.isDebugEnabled())
                LOG.debug("Rating " + t + " as " + rating);
			t.rate(rating);
        }
	}

	/**
	 * Replaces each token with an equivalent previously stored token, or
     * stores the token if no equivalent exists
	 * 
	 * @param tokens an array of tokens to be replaced
	 * @return the same array, with each element replaced by an equivalent
     *         previously stored token if any such token exists
	 */
	private Token[] lookup(Token[] tokens) {
		for (int i = 0; i < tokens.length; i++) {
			// lookup stored token
			tokens[i] = lookup(tokens[i]);
		}
		return tokens;
	}

	/**
	 * Replaces a token with an equivalent previously stored token if
     * any such token exists, otherwise stores the token
	 * 
	 * @param token the token to look up
	 * @return the same token or a previously stored equivalent
	 */
	private Token lookup(Token token) {
        Token stored = _tokenMap.get(token);
        if(stored == null) {
            _tokenMap.put(token, token);
            pruneEntries (MAX_SIZE * 2);
            stored = token;
        }
        return stored;
	}

	/**
	 * Load ratings from disk
	 * 
	 * @return Map of <tt>Token</tt> to <tt>Token</tt> as read from disk
	 */
	private Map<Token, Token> load() {
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(getSpamDat())));
            return GenericsUtils.scanForMap(is.readObject(),
                    Token.class, Token.class, GenericsUtils.ScanMode.REMOVE);
		} catch(Throwable t) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error loading spam ratings: " + t);
			return new HashMap<Token, Token>();
		} finally {
            IOUtils.close(is);
		}
	}
    
    /**
     * Save ratings to disk
     */
    public synchronized void save() {
        pruneEntries (MAX_SIZE);
        Map<Token, Token> copy = new HashMap<Token, Token>(_tokenMap);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Saving " + copy.size() + " entries");

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(getSpamDat())));
            oos.writeObject(copy);
            oos.flush();
        } catch (IOException iox) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error saving spam ratings: ", iox);
        } finally {
            IOUtils.close(oos);
        }
	}
    
    /**
     * Increments the age of each token and saves the ratings to disk
     * (called when LimeWire shuts down)
     */
    public synchronized void stop() {
        for(Token token : _tokenMap.values()) 
            token.incrementAge();
        save();
    }
    
	/**
	 * Removes the least important entries from the rating table
     *
     * @param size the size to which the table should be pruned
	 */
	private void pruneEntries(int size) {
        
        int tokensToRemove = _tokenMap.size() - size;
        if(tokensToRemove <= 0)
            return;
        
		if(LOG.isDebugEnabled())
			LOG.debug("Pruning rating table to " + size + " entries");

        // Make a set of sorted tokens, low importance first
        Set<Token> sortedTokens = new TreeSet<Token>(_tokenMap.values());
        for(Token token : sortedTokens) {
            // Keys and values of the map are identical, so we can iterate
            // over the values and use them as keys
            _tokenMap.remove(token);
            --tokensToRemove;
            if(tokensToRemove == 0)
                break;
        }
	}
    
	private static File getSpamDat() {
	    return new File(CommonUtils.getUserSettingsDir(), "spam.dat");
	}
    
	/** Inspectable that returns a hash and rating of the tokens */
	@InspectionPoint("spam rating table token hashes")
	@SuppressWarnings("unused")
	private final Inspectable TOKEN_HASH = new Inspectable() {
	    public Object inspect() {
	        synchronized(RatingTable.this) {
	            Map<String, Object> ret = new HashMap<String, Object>();
	            ret.put("ver",1);
                final float spamThreshold = SearchSettings.FILTER_SPAM_RESULTS.getValue();
	            ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            DataOutputStream daos = new DataOutputStream(baos);
	            try {
	                for (Token t : _tokenMap.values()) {
	                    // 8 bytes per entry
	                    float rating = t.getRating();
	                    if (rating < spamThreshold)
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
