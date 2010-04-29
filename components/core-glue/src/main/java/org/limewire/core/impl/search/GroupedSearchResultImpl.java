package org.limewire.core.impl.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.util.Objects;

/**
 * An implementation of GroupedSearchResult for grouping search results.  The
 * instance values in GroupedSearchResultImpl are updated in a background
 * thread as search results are received.
 */
class GroupedSearchResultImpl implements GroupedSearchResult {

    private static final Comparator<Friend> FRIEND_COMPARATOR = new FriendComparator();
    
    private final Set<RemoteHost> remoteHosts;
    
    private volatile List<SearchResult> coreResults;
    private volatile Set<Friend> friends;
    private volatile boolean anonymous;
    private volatile float relevance = 0;
    
    /**
     * Constructs a GroupedSearchResult containing the specified search result,
     * with the specified initial relevance.
     */
    public GroupedSearchResultImpl(SearchResult searchResult, String query, float relevance) {
        this.remoteHosts = new CopyOnWriteArraySet<RemoteHost>();
        this.relevance = relevance;
        addNewSource(searchResult, query);
    }

    /**
     * Adds the specified search result to the grouping.  The specified query
     * text is used to adjust the relevance score.  This method is only called 
     * by CoreSearchResultList after a write lock is obtained on the parent 
     * list.
     */
    void addNewSource(SearchResult result, String query) {
        // Optimize for only having a single result.
        if (coreResults == null) {
            coreResults = Collections.singletonList(result);
        } else {
            if (coreResults.size() == 1) {
                coreResults = new CopyOnWriteArrayList<SearchResult>(coreResults);
            }
            coreResults.add(result);
        }
        
        // Accumulate relevance score.
        relevance += result.getRelevance(query);
        
        // Build collection of non-anonymous friends for filtering.
        RemoteHost host = result.getSource();
        remoteHosts.add(host);

        Friend friend = host.getFriendPresence().getFriend();
        if (friend.isAnonymous()) {
            anonymous = true;
        } else {
            if (friends == null) {
                // optimize for a single friend having it
                friends = Collections.singleton(friend);
            } else {
                // convert to CopyOnWriteArraySet if we need to.
                if (!(friends instanceof CopyOnWriteArraySet)) {
                    Set<Friend> newFriends = new CopyOnWriteArraySet<Friend>();
                    newFriends.addAll(friends);
                    friends = newFriends;
                }
                friends.add(friend);
            }

        }
    }
    
    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public String getFileName() {
        return coreResults.get(0).getFileName();
    }

    @Override
    public Collection<Friend> getFriends() {
        if (friends == null) {
            return Collections.<Friend>emptySet();
        } else {
            // Create sorted set of friends.
            Set<Friend> newFriends = new TreeSet<Friend>(FRIEND_COMPARATOR);
            newFriends.addAll(friends);
            return newFriends;
        }
    }

    @Override
    public float getRelevance() {
        return relevance;
    }
    
    @Override
    public List<SearchResult> getSearchResults() {
        return coreResults;
    }

    @Override
    public Collection<RemoteHost> getSources() {
        return remoteHosts;
    }

    @Override
    public URN getUrn() {
        return coreResults.get(0).getUrn();
    }

    /**
     * Comparator to order Friend objects.
     */
    private static class FriendComparator implements Comparator<Friend> {
        @Override
        public int compare(Friend o1, Friend o2) {
            String id1 = o1.getId();
            String id2 = o2.getId();
            return Objects.compareToNullIgnoreCase(id1, id2, false);
        }
    }    
}
