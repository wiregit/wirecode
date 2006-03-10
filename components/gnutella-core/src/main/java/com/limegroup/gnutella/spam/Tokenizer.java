package com.limegroup.gnutella.spam;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
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
public class Tokenizer {
	private static final Log LOG = LogFactory.getLog(Tokenizer.class);

	/**
	 * the min number of bytes for a keyword, keywords shorter than this will be
	 * ignored
	 * 
	 * NOTE: we use the number of bytes not chars to decide whether or not to
	 * block because e.g. in japanese many words will be only one or two chars
	 * long.
	 */
	private static int MIN_KEYWORD_LENGTH = 3;

	/**
	 * the max number of bytes for a keyword, keywords longer than this will be
	 * truncated
	 */
	private static int MAX_KEYWORD_LENGTH = 8;

	/**
	 * these are the characters used to split file names and meta-data fields
	 * into keyword tokens.
	 */
	private static final String KEYWORD_DELIMITERS = " -._+/*()\\,\t";

	private Tokenizer() {
	}

	/**
	 * split a <tt>RemoteFileDesc</tt> into an array of <tt>Token</tt>
	 * 
	 * @param desc
	 *            the RemoteFileDesc, that should be tokenized
	 * @return an array of Tokens, will never be empty
	 */
	public static Token[] getTokens(RemoteFileDesc desc) {
		if (LOG.isDebugEnabled())
			LOG.debug("tokenizing: " + desc);
		Set set = new HashSet();
		set.addAll(getKeywordTokens(desc));
		if (desc.getSHA1Urn() != null)
			set.add(getUrnToken(desc));
		set.add(getSizeToken(desc));
        set.add(getVendorToken(desc));
		set.add(getAddressToken(desc));
		Token[] tokens = new Token[set.size()];
		tokens = (Token[]) set.toArray(tokens);
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
	public static Token[] getTokens(RemoteFileDesc[] descs) {
		Set set = new HashSet();
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
		tokens = (Token[]) set.toArray(tokens);
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
	public static Token[] getTokens(QueryRequest qr) {
		if (LOG.isDebugEnabled())
			LOG.debug("tokenizing: " + qr);
		Set set = new HashSet();
		set.addAll(getKeywordTokens(qr));
		set.addAll(getUrnTokens(qr));
		Token[] tokens = new Token[set.size()];
		tokens = (Token[]) set.toArray(tokens);
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
	private static Token getUrnToken(RemoteFileDesc desc) {
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
	private static Set getUrnTokens(QueryRequest qr) {
		if (qr.getQueryUrns().isEmpty())
			return Collections.EMPTY_SET;
		Set urns = qr.getQueryUrns();
		Set ret = new HashSet();
		for (Iterator iter = urns.iterator(); iter.hasNext();)
			ret.add(new UrnToken((URN) iter.next()));
		return ret;
	}

	/**
	 * Builds a SizeToken for a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc we are tokenizing
	 * @return a new SizeToken
	 */
	private static Token getSizeToken(RemoteFileDesc desc) {
		return new SizeToken(desc.getSize());
	}
    
    /**
     * Returns a (most often) previously cached token for the specific
     * vendor
     */
    private static Token getVendorToken(RemoteFileDesc desc) {
        return VendorToken.getToken(desc.getVendor());
    }

	/**
	 * Builds an AddressToken for a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc we are tokenizing
	 * @return a new AddressToken
	 */
	private static Token getAddressToken(RemoteFileDesc desc) {
		return new AddressToken(desc.getInetAddress().getAddress(), desc
				.getPort());
	}

	/**
	 * Builds a Set of KeywordToken for a RemoteFileDesc
	 * 
	 * @param desc
	 *            the RemoteFileDesc we are tokenizing
	 * @return a Set of KeywordToken and XMLKeywordToken
	 */
	private static Set getKeywordTokens(RemoteFileDesc desc) {
		return getKeywordTokens(desc.getFileName(), desc.getXMLDocument());
	}

	/**
	 * Builds a Set of KeywordToken for a QueryRequest
	 * 
	 * @param qr
	 *            the QueryRequest we are tokenizing
	 * @return a Set of KeywordToken and XMLKeywordToken
	 */
	private static Set getKeywordTokens(QueryRequest qr) {
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
	private static Set getKeywordTokens(String fname, LimeXMLDocument doc) {
		Set tokens = getKeywordTokens(fname.toLowerCase(Locale.US));
		if (doc != null) {
			for (Iterator iter = doc.getNameValueSet().iterator(); iter
					.hasNext();) {
				Map.Entry next = (Map.Entry) iter.next();
				tokens.addAll(getXMLKeywords(next.getKey().toString()
						.toLowerCase(Locale.US), next.getValue().toString()
						.toLowerCase(Locale.US)));
			}
		}
		return tokens;
	}

	/**
	 * Get an XMLKeywordToken for the field-name and the value of an XML
	 * meta-data item
	 * 
	 * @param name
	 *            the field name as String (something like audios_audio_bitrate)
	 * @param value
	 *            the value String
	 * @return a Set of XMLKeywordToken
	 */
	private static Set getXMLKeywords(String name, String value) {
		name = extractSimpleFieldName(name);

		Set ret = new HashSet();

		StringTokenizer tok = new StringTokenizer(value, KEYWORD_DELIMITERS);
		while (tok.hasMoreTokens()) {
			byte[] token = tok.nextToken().getBytes();
			if (token.length < MIN_KEYWORD_LENGTH)
				continue;
			if (token.length > MAX_KEYWORD_LENGTH)
				token = truncateArray(token, MAX_KEYWORD_LENGTH);
			ret.add(new XMLKeywordToken(name, token));
		}
		return ret;
	}

	/**
	 * truncate an array of bytes
	 * 
	 * @param array
	 *            the source array.
	 * @param length
	 *            the length of the truncated array
	 * @return array of the first length bytes of the source array
	 */
	private static byte[] truncateArray(byte[] array, int length) {
		byte[] ret = new byte[length];
		System.arraycopy(array, 0, ret, 0, length);
		return ret;
	}

	/**
	 * this method merges two byte arrays into one byte array, separating the
	 * two arrays only by a single 0x00 byte.
	 * 
	 * @param array1
	 * @param array2
	 * @return byte array
	 */
	private static byte[] mergeArrays(byte[] array1, byte[] array2) {
		byte[] ret = new byte[array1.length + array2.length + 1];
		System.arraycopy(array1, 0, ret, 0, array1.length);
		ret[array1.length] = 0;
		System.arraycopy(array2, 0, ret, array1.length + 1, array2.length);
		return ret;
	}

	/**
	 * Extract the last part of the field name for a canonical field name
	 * (audios_audio_bitrate becomes bitrate)
	 * 
	 * @param canonicalField
	 *            the canonical field name
	 * @return the last part of the canonical field name
	 */
	private static String extractSimpleFieldName(String canonicalField) {
		int idx1 = canonicalField.lastIndexOf(XMLStringUtils.DELIMITER);
		int idx2 = canonicalField.lastIndexOf(XMLStringUtils.DELIMITER,
				idx1 - 1);
		return (canonicalField.substring(idx2
				+ XMLStringUtils.DELIMITER.length(), idx1));
	}

	/**
	 * splits a String into keyword tokens
	 * 
	 * @param str
	 *            the String to tokenize
	 * @return a Set of KeywordToken
	 */
	private static Set getKeywordTokens(String str) {
		Set ret = new HashSet();

		StringTokenizer tok = new StringTokenizer(str, KEYWORD_DELIMITERS);
		byte[] last = null;
		while (tok.hasMoreTokens()) {
			byte[] next = tok.nextToken().getBytes();

			if (next.length < MIN_KEYWORD_LENGTH) {
				if (last != null) {
					Token token = new KeywordToken(mergeArrays(last, next));
					ret.add(token);
				}
				last = next;
				continue;
			}

			if (next.length > MAX_KEYWORD_LENGTH)
				next = truncateArray(next, MAX_KEYWORD_LENGTH);

			Token token = new KeywordToken(next);
			ret.add(token);

			if (last != null) {
				token = new KeywordToken(mergeArrays(last, next));
				ret.add(token);
			}
			last = next;
		}
		return ret;
	}
}
