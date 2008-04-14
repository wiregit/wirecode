package com.limegroup.gnutella.spam;

import java.util.Locale;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.spam.Token.Rating;

@Singleton
public class SpamManager {
	//private static final Log LOG = LogFactory.getLog(SpamManager.class);

	/**
	 * If an RFDs spam rating is > MAX_THRESHOLD we will not remember the rating
	 * for the Tokens of the RFD because it e.g. a spammer very frequently
	 * sending a bad UrnToken with varying keywords, sizes and addresses may be
	 * able to pollute the filter data
	 */
	public static final float MAX_THRESHOLD = 0.995f;

	/**
	 * inverse rating (1 - probability) for an RFD without SHA1 urn. 
	 */
	private static final float NO_SHA1_URN_RATING = 0.5f;

	/**
	 * incomplete file... save the user the trouble of downloading it, if he has
	 * his spam filter enabled
	 */
	private static final float INCOMPLETE_FILE_RATING = 0.2f;

	private final RatingTable ratingTable;
	
	@Inject
	SpamManager(RatingTable ratingTable) {
	    this.ratingTable = ratingTable;
	}
	
	/**
	 * informs the SpamManager of any query that was started and clears bad
	 * ratings for the keywords in the query
	 * 
	 * @param qr
	 *            the QueryRequest for the query.
	 */
	public void startedQuery(QueryRequest qr) {
		if (SearchSettings.ENABLE_SPAM_FILTER.getValue())
			ratingTable.mark(qr, Rating.CLEARED);
	}

	/**
	 * This method will rate a given rfd and return whether or not the
	 * SpamManager believes this is spam
	 * 
	 * @param rfd
	 *            the RemoteFileDesc to rate
	 * @modifies rfd
	 * @return true if the SpamManager internally rated it as spam and false if
	 *         the SpamManager did not rate it as spam
	 */
	public boolean isSpam(RemoteFileDesc rfd) {
		if (!SearchSettings.ENABLE_SPAM_FILTER.getValue())
			return false;

		// rate simple spam...
		float rating = 0.f;
		if (rfd.getSHA1Urn() == null && 
                rfd.getXMLDocument() != null &&
                rfd.getXMLDocument().getAction().length() == 0)
			rating = 1 - (1 - rating) * NO_SHA1_URN_RATING;
        
		if (isIncompleteFile(rfd.getFileName().toLowerCase(Locale.US))) {
			rating = 1 - (1 - rating) * INCOMPLETE_FILE_RATING;
		}

		// apply bayesian filter
		rating = 1 - (1 - rating) * (1 - ratingTable.getRating(rfd));
		rfd.setSpamRating(rating);
		return rating >= Math.max(SearchSettings.FILTER_SPAM_RESULTS.getValue(),
                SearchSettings.QUERY_SPAM_CUTOFF.getValue());
	}

	/**
	 * this method is called if the user marked some RFDs as being spam
	 * 
	 * @param rfds
	 *            an array of RemoteFileDesc that should be marked as good
	 */
	public void handleUserMarkedSpam(RemoteFileDesc[] rfds) {
		for (int i = 0; i < rfds.length; i++)
			rfds[i].setSpamRating(1.f);

		ratingTable.mark(rfds, Rating.USER_MARKED_SPAM);
	}

	/**
	 * this method is called if the user marked some RFDs as not being spam
	 * 
	 * @param rfds
	 *            an array of RemoteFileDesc that should be marked as good
	 */
	public void handleUserMarkedGood(RemoteFileDesc[] rfds) {
		for (int i = 0; i < rfds.length; i++)
			rfds[i].setSpamRating(0.f);

		ratingTable.mark(rfds, Rating.USER_MARKED_GOOD);
	}

	/**
	 * clears all collected filter data
	 */
	public void clearFilterData() {
		ratingTable.clear();
	}
    
	/**
	 * look for
	 * <ul>
	 * <li>__INCOMPLETE</li>
	 * <li>___ARESTRA</li>
	 * <li>___INCOMPLETED</li>
	 * <li>PREVIEW-T-</li>
	 * <li>CORRUPT-(number)-</li>
	 * <li>T-(number)-</li>
	 * 
	 * @param name
	 *            the name of the file from a search result
	 * @return true if we think that this is an incomplete file
	 */
	private boolean isIncompleteFile(String name) {
	    if (name.startsWith("incomplete_"))
            return true;
	    if (name.startsWith("incomplete~"))
            return true;
	    if (name.startsWith("inacheve_"))
            return true;       
	    if (name.startsWith("in_"))
            return true;
	    if (name.startsWith("__incomplete"))
			return true;
		if (name.startsWith("___incompleted"))
			return true;
		if (name.startsWith("___arestra"))
			return true;
		if (name.startsWith("preview-t-"))
			return true;
		if (name.startsWith("t-")) {
			for (int i = 2; i < name.length(); i++) {
				if (Character.isDigit(name.charAt(i)))
					continue;
				else
					return name.charAt(i) == '-';
			}
		}
		if (name.startsWith("corrupt-")) {
			for (int i = 8; i < name.length(); i++) {
				if (Character.isDigit(name.charAt(i)))
					continue;
				else
					return name.charAt(i) == '-';
			}
		}
		return false;
	}
}
