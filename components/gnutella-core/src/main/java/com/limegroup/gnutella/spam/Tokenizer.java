package com.limegroup.gnutella.spam;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.XMLStringUtils;

/**
 * This class splits a RemoteFileDesc or a QueryRequest into tokens that will
 * be put into the RatingTable.
 * 
 * Currently, it extracts the following data from the RemoteFileDesc to build a
 * token:
 * 
 * <ul>
 * <li>keywords from the file name</li>
 * <li>name/value pairs from the XML metadata (if any)</li>
 * <li>file urn (if any)</li>
 * <li>file size</li>
 * <li>address (but not port) of the sender</li>
 * </ul>
 * 
 * The vendor string is no longer used, since it's too easy for spammers
 * to forge.
 */
@Singleton
public class Tokenizer {
	private static final Log LOG = LogFactory.getLog(Tokenizer.class);

	/**
	 * The maximum length of a keyword in chars; keywords longer than this
     * will be truncated. We use chars rather than bytes to avoid corrupting
     * multi-byte chars when truncating
	 */
	private int MAX_KEYWORD_LENGTH = 8;

	@Inject
	Tokenizer(Provider<IPFilter> ipFilter) {
        AddressToken.setIpFilter(ipFilter);
	}
	
	/**
	 * Extracts an array of unique Tokens from a RemoteFileDesc
	 * 
	 * @param desc the RemoteFileDesc that should be tokenized
	 * @return an array of Tokens, will never be empty
	 */
	public Token[] getTokens(RemoteFileDesc desc) {
        Set<Token> set = new HashSet<Token>();
        tokenize(desc, set);
        Token[] tokens = new Token[set.size()];
        tokens = set.toArray(tokens);
        return tokens;
    }
    
    /**
     * Extracts an array of unique tokens from an array of RemoteFileDescs -
     * useful if the user wants to mark multiple RFDs from a TableLine as
     * spam, which should rate each token only once
     * 
     * @param descs the array of RemoteFileDescs that should be tokenized
     * @return an array of Tokens, will never be empty
     */
    public Token[] getTokens(RemoteFileDesc[] descs) {
        Set<Token> set = new HashSet<Token>();
        for(RemoteFileDesc desc : descs)
            tokenize(desc, set);
        Token[] tokens = new Token[set.size()];
        tokens = set.toArray(tokens);
        return tokens;
    }

    /**
     * Extracts a set of tokens from a RemoteFileDesc
     * 
     * @param desc the RemoteFileDesc that should be tokenized
     * @param set the extracted tokens, may be empty
     */
    private void tokenize(RemoteFileDesc desc, Set<Token> set) {
		if(LOG.isDebugEnabled())
			LOG.debug("Tokenizing " + desc);
        set.addAll(getKeywordTokens(desc.getFileName()));
        LimeXMLDocument xml = desc.getXMLDocument();
        if(xml != null)
		    set.addAll(getKeywordTokens(xml));
        URN urn = desc.getSHA1Urn();
		if(urn != null)
			set.add(new UrnToken(urn));
		set.add(new SizeToken(desc.getSize()));
		set.add(new AddressToken(desc.getAddress()));
	}

	/**
	 * Tokenizes a QueryRequest, including the search terms, XML metadata and
     * URN - used to reduce the ratings of keywords the user searches for
	 * 
	 * @param qr the QueryRequest that should be tokenized
	 * @return an array of Tokens, may be empty
	 */
	public Token[] getTokens(QueryRequest qr) {
		if(LOG.isDebugEnabled())
			LOG.debug("Tokenizing " + qr);
		Set<Token> set = getKeywordTokens(qr.getQuery());
        LimeXMLDocument xml = qr.getRichQuery();
        if(xml != null)
            set.addAll(getKeywordTokens(xml));
        Set<URN> urns = qr.getQueryUrns();
        for(URN urn : urns)
            set.add(new UrnToken(urn));
		Token[] tokens = new Token[set.size()];
		tokens = set.toArray(tokens);
		return tokens;
	}

	/**
	 * Extracts KeywordTokens from an XML metadata document
	 * 
	 * @param doc the LimeXMLDocument that should be tokenized
	 * @return a Set of XMLKeywordTokens, may be empty
	 */
	private Set<Token> getKeywordTokens(LimeXMLDocument doc) {
        Set<Token> tokens = new HashSet<Token>();
        for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
            String name = entry.getKey().toString();
            String value = entry.getValue().toString(); 
            tokens.addAll(getXMLKeywords(name, value));
		}
		return tokens;
	}

	/**
	 * Extracts XMLKeywordTokens from the field name and value of an XML
	 * metadata item
	 * 
	 * @param name the field name as a String (eg audios_audio_bitrate)
	 * @param value the value as a String
	 * @return a Set of XMLKeywordTokens
	 */
	private Set<Token> getXMLKeywords(String name, String value) {
        name = extractSimpleFieldName(name);
        name.toLowerCase(Locale.US);
        value.toLowerCase(Locale.US);
		Set<Token> tokens = new HashSet<Token>();
		for(String keyword : QueryUtils.extractKeywords(value, false)) {
            if(keyword.length() > MAX_KEYWORD_LENGTH)
                keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
            tokens.add(new XMLKeywordToken(name, keyword));
        }
        return tokens;
	}

	/**
	 * Extracts the last part of the field name for a canonical field name
	 * (eg audios_audio_bitrate becomes bitrate)
	 * 
	 * @param name the canonical field name
	 * @return the last part of the canonical field name
	 */
	private String extractSimpleFieldName(String name) {
		int idx1 = name.lastIndexOf(XMLStringUtils.DELIMITER);
		int idx2 = name.lastIndexOf(XMLStringUtils.DELIMITER, idx1 - 1);
		return name.substring(idx2 + XMLStringUtils.DELIMITER.length(), idx1);
	}

	/**
	 * Splits a String into keyword tokens using QueryUtils.extractKeywords()
	 * 
	 * @param str the String to tokenize
	 * @return a Set of KeywordTokens, may be empty
	 */
	private Set<Token> getKeywordTokens(String str) {
        str.toLowerCase(Locale.US);
		Set<Token> tokens = new HashSet<Token>();
        for(String keyword : QueryUtils.extractKeywords(str, false)) {
            if(keyword.length() > MAX_KEYWORD_LENGTH)
                keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
            tokens.add(new KeywordToken(keyword));
        }
        return tokens;
	}
}
