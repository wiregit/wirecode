package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.QueryRequest;

public interface SharedFilesKeywordIndex extends FileEventListener {

    /**
     * Returns an array of all responses matching the given request.  If there
     * are no matches, the array will be empty (zero size).
     *
     * Incomplete Files are returned in responses to queries that desire it.
     *
     * @return an empty array if not matching shared files were found
     */
    public abstract Response[] query(QueryRequest request);

}
