package com.limegroup.gnutella.spam;

import java.util.Collections;
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
 * This class is an important part of the spam filter. It splits a
 * RemoteFileDesc (or a QueryRequest) into tokens will than be put into the
 * RatingTable.
 * 
 * Currently, it extracts the following data from the RemoteFileDesc to build a
 * token:
 * 
 * <ul>
 * <li>file size</li>
 * <li>file urn</li>
 * <li>address:port of the sender</li>
 * <li>all keywords in the LimeXMLDocuments that are longer than 2 bytes and
 * the fields they were found in</li>
 * <li>all keywords that are longer than 2 bytes and the first 3 bytes of the
 * following keyword</li>
 * </ul>
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
	 * split a <tt>RemoteFileDesc</tt> into an array of <tt>Token</tt>
	 * 
	 * @param desc
	 *            the RemoteFileDesc, that should be tokenized
	 * @return an array of Tokens, will never be empty
	 */
	public Token[] getTokens(RemoteFileDesc desc) {
		if (LOG.isDebugEnabled())
			LOG.debug("tokenizing: " + desc);
		Set<Token> set = new HashSet<Token>();
		set.addAll(getKeywordTokens(desc));
		if (desc.getSHA1Urn() != null)
			set.add(getUrnToken(desc));
		set.add(getSizeToken(desc));
        set.add(getVendorToken(desc));
		set.add(getAddressToken(desc));
		Token[] tokens = new Token[set.size()];
		tokens = set.toArray(tokens);
		return tokens;
	}

	/**
	 * split an array of <tt>RemoteFileDesc</tt> into an array of unique
	 * <tt>Token</tt>. This is a very useful class, if the user wants to mark
	 * multiple RFDs from a TableLine as spam, which should rate every token for
	 * this table line only once as spam.
	 * 
	 * @param descs
	 *            the array of RemoteFileDesc, that should be tokenized
	 * @return an array of Tokens, will never be empty
	 */
	public Token[] getTokens(RemoteFileDesc[] descs) {
		Set<Token> set = new HashSet<Token>();
		for (int i = 0; i < descs.length; i++) {
			if (LOG.isDebugEnabled())
				LOG.debug("tokenizing: " + descs[i]);
			set.addAll(getKeywordTokens(descs[i]));
			if (descs[i].getSHA1Urn() != null)
				set.add(getUrnToken(descs[i]));
			set.add(getSizeToken(descs[i]));
            set.add(getVendorToken(descs[i]));
			set.add(getAddressToken(descs[i]));
		}
		Token[] tokens = new Token[set.size()];
		tokens = set.toArray(tokens);
		return tokens;
	}

	/**
	 * tokenizes a QueryRequest, - used to clear all ratings for keywords the
	 * user issues a query for.
	 * 
	 * @param qr
	 *            the <tt>QueryRequest</tt> to tokenize
	 * @return an array of <tt>Token</tt>
	 */
	public Token[] getTokens(QueryRequest qr) {
		if (LOG.isDebugEnabled())
			LOG.debug("tokenizing: " + qr);
		Set<Token> set = new HashSet<Token>();
		set.addAll(getKeywordTokens(qr));
		set.addAll(getUrnTokens(qr));
		Token[] tokens = new Token[set.size()];
		tokens = set.toArray(tokens);
		return tokens;
	}

	/**
	 * Builds an UrnToken for a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RFD we are tokenizing
	 * @return a new UrnToken, built from the SHA1 urn or null, if the RFD did
	 *         not contain a URN
	 */
	private Token getUrnToken(RemoteFileDesc desc) {
		if (desc.getSHA1Urn() != null)
			return new UrnToken(desc.getSHA1Urn());
		return null;
	}

	/**
	 * Builds an UrnToken for a QueryRequest
	 * 
	 * @param qr
	 *            the QueryRequest we are tokenizing
	 * @return a Set of UrnToken, built from the query urns
	 */
	private Set<Token> getUrnTokens(QueryRequest qr) {
		if (qr.getQueryUrns().isEmpty())
			return Collections.emptySet();
		Set<Token> ret = new HashSet<Token>();
        for(URN urn : qr.getQueryUrns())
            ret.add(new UrnToken(urn));
		return ret;
	}

	/**
	 * Builds a SizeToken for a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc we are tokenizing
	 * @return a new SizeToken
	 */
	private Token getSizeToken(RemoteFileDesc desc) {
		return new SizeToken(desc.getSize());
	}
    
    /**
     * Returns a (most often) previously cached token for the specific
     * vendor
     */
    private Token getVendorToken(RemoteFileDesc desc) {
        return VendorToken.getToken(desc.getVendor());
    }

	/**
	 * Builds an AddressToken for a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc we are tokenizing
	 * @return a new AddressToken
	 */
	private Token getAddressToken(RemoteFileDesc desc) {
		return new AddressToken(desc.getAddress());
	}

	/**
	 * Builds a Set of KeywordToken for a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc we are tokenizing
	 * @return a Set of KeywordToken and XMLKeywordToken
	 */
	private Set<Token> getKeywordTokens(RemoteFileDesc desc) {
		return getKeywordTokens(desc.getFileName(), desc.getXMLDocument());
	}

	/**
	 * Builds a Set of KeywordToken for a QueryRequest
	 * 
	 * @param qr
	 *            the QueryRequest we are tokenizing
	 * @return a Set of KeywordToken and XMLKeywordToken
	 */
	private Set<Token> getKeywordTokens(QueryRequest qr) {
		return getKeywordTokens(qr.getQuery(), qr.getRichQuery());
	}

	/**
	 * Builds a Set of KeywordToken
	 * 
	 * @param fname
	 *            the filename that should be split into KeywordToken
	 * @param doc
	 *            the LimeXMLDocument that should be split into XMLKeywordToken
	 * @return a Set of XMLKeywordToken
	 */
	private Set<Token> getKeywordTokens(String fname, LimeXMLDocument doc) {
		Set<Token> tokens = getKeywordTokens(fname.toLowerCase(Locale.US));
		if (doc != null) {
            for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
				tokens.addAll(getXMLKeywords(entry.getKey().toString()
						.toLowerCase(Locale.US), entry.getValue().toString()
						.toLowerCase(Locale.US)));
			}
		}
		return tokens;
	}

	/**
	 * Extracts XMLKeywordTokens from the field-name and value of an XML
	 * metadata item
	 * 
	 * @param name the field-name as a String (eg audios_audio_bitrate)
	 * @param value the value as a String
	 * @return a Set of XMLKeywordTokens
	 */
	private Set<Token> getXMLKeywords(String name, String value) {
		name = extractSimpleFieldName(name);
		Set<Token> tokens = new HashSet<Token>();
		for(String keyword : QueryUtils.extractKeywords(value, false)) {
            if(keyword.length() > MAX_KEYWORD_LENGTH)
                keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
            tokens.add(new XMLKeywordToken(name, keyword));
        }
        return tokens;
	}

	/**
	 * Extract the last part of the field name for a canonical field name
	 * (eg audios_audio_bitrate becomes bitrate)
	 * 
	 * @param canonicalField the canonical field name
	 * @return the last part of the canonical field name
	 */
	private String extractSimpleFieldName(String canonicalField) {
		int idx1 = canonicalField.lastIndexOf(XMLStringUtils.DELIMITER);
		int idx2 = canonicalField.lastIndexOf(XMLStringUtils.DELIMITER,
				idx1 - 1);
		return (canonicalField.substring(idx2
				+ XMLStringUtils.DELIMITER.length(), idx1));
	}

	/**
	 * Splits a String into keyword tokens using QueryUtils.extractKeywords()
	 * 
	 * @param str the String to tokenize
	 * @return a Set of KeywordToken
	 */
	private Set<Token> getKeywordTokens(String str) {
		Set<Token> tokens = new HashSet<Token>();
        for(String keyword : QueryUtils.extractKeywords(str, false))
            tokens.add(new KeywordToken(keyword));
        return tokens;
	}
}
