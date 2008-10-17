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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.limegroup.gnutella.spam.Token;

@Singleton
public class RatingTable implements Service {
	private static final Log LOG = LogFactory.getLog(RatingTable.class);

	/**
	 * Don't hold more than this many entries in memory or save more than
     * this many entries to disk. The size is a tradeoff - tokens should be
     * discarded when they become irrelevant, but not before.
	 */
	private static final int MAX_SIZE = 10000;
    
    /**
     * Initial size of the rating table - should not be too large as many
     * users probably don't use the spam filter.
     */
    private static final int INITIAL_SIZE = 1000;
    
	/**
	 * A map containing a limited number of tokens. We use a map rather than
     * a set so that we can retrieve a stored token by using an equivalent
     * token as a key. This allows us to use a token without rating data to
     * retrieve an equivalent token that has rating data.
     * 
     * The size of the map is limited. Entries are discarded in
     * least-recently-used order when the map if full, on the assumption that
     * the least-recently-used token is the least important to keep.
     * TODO: check that the order is preserved during serialization.
	 */
	private final Map<Token, Token> tokenMap
        = new LinkedHashMap<Token, Token>(INITIAL_SIZE, 0.75f, true) {
            // This method will be called on every get(), put(), and putAll()
            protected boolean removeEldestEntry(Map.Entry<Token, Token> e) {
                if(size() > MAX_SIZE) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Discarding token " + e.getValue());
                    return true;
                }
                return false;
            }
        };
	
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
        load();
    }
	
    public synchronized void stop() {
        save();
    }
    
	/**
	 * Clears the filter data
	 */
	protected synchronized void clear() {
        LOG.debug("Clearing ratings");
		tokenMap.clear();
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
	 * @param rating a rating between 0 (not spam) and 1 (spam)
	 */
	protected synchronized void rate(RemoteFileDesc[] descs, float rating) {
		rateInternal(lookup(tokenizer.getTokens(descs)), rating);
	}

	/**
     * Clears the ratings of the tokens associated with a QueryRequest
     * 
	 * @param qr the QueryRequest to clear
	 */
	protected synchronized void clear(QueryRequest qr) {
		rateInternal(lookup(tokenizer.getTokens(qr)), 0);
	}

	/**
	 * Assigns the given rating to an array of tokens
	 * 
	 * @param tokens the Tokens to rate
	 * @param rating a rating as defined by the Token interface
	 */
	private void rateInternal(Token[] tokens, float rating) {
		for(Token t : tokens) {
            if(LOG.isDebugEnabled())
                LOG.debug("Rating " + t + " as " + rating);
			t.updateRating(rating);
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
        Token stored = tokenMap.get(token);
        if(stored == null) {
            tokenMap.put(token, token);
            stored = token;
        }
        return stored;
	}

	/**
	 * Loads ratings from disk
	 */
	private void load() {
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(getSpamDat())));
            List<Token> list
                = GenericsUtils.scanForList(is.readObject(),
                    Token.class, GenericsUtils.ScanMode.REMOVE);
            // Most-recently-used token will be at the head, but we want to
            // add it to the map last so it's still most-recently-used
            Collections.reverse(list);
            for(Token t : list)
                tokenMap.put(t, t);
            if(LOG.isDebugEnabled())
                LOG.debug("Loaded " + tokenMap.size() + " entries");
		} catch(Throwable t) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error loading spam ratings: " + t);
		} finally {
            IOUtils.close(is);
		}
	}
    
    /**
     * Saves ratings to disk (called whenever the user marks a search result)
     */
    public synchronized void save() {
        // Don't save ratings that have default scores
        ArrayList<Token> list = new ArrayList<Token>(tokenMap.size());
        for(Token t : tokenMap.keySet())
            if(t.getRating() > 0f)
                list.add(t);
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(getSpamDat())));
            oos.writeObject(list);
            oos.flush();
            if(LOG.isDebugEnabled())
                LOG.debug("Saved " + tokenMap.size() + " entries");
        } catch (IOException iox) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error saving spam ratings: ", iox);
        } finally {
            IOUtils.close(oos);
        }
        // DEBUG: dump the ratings to a human-readable file
        PrintWriter dump = null;
        try {
            dump = new PrintWriter(
                    new File(CommonUtils.getUserSettingsDir(), "spam.dump"));
            for(Token t : list) dump.println(t);
        } catch (Exception x) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error dumping spam ratings: ", x);
        } finally {
            IOUtils.close(dump);
        }
	}
    
	private static File getSpamDat() {
	    return new File(CommonUtils.getUserSettingsDir(), "spam1.dat");
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
	                for (Token t : tokenMap.values()) {
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
