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
 * Implementation of GroupedSearchResult for the mock core.
 */
class MockGroupedSearchResult implements GroupedSearchResult {

    private static final Comparator<Friend> FRIEND_COMPARATOR = new FriendComparator();
    private static final Comparator<RemoteHost> REMOTE_HOST_COMPARATOR = new RemoteHostComparator();
    
    private final Set<RemoteHost> remoteHosts;
    
    private volatile List<SearchResult> coreResults;
    private volatile Set<Friend> friends;
    private volatile boolean anonymous;
    private volatile float relevance = 0;    
    
    /**
     * Constructs a GroupedSearchResult containing the specified search result.
     */
    public MockGroupedSearchResult(SearchResult searchResult, String query) {
        this.remoteHosts = new CopyOnWriteArraySet<RemoteHost>();
        
        addNewSource(searchResult, query);
    }
    
    /**
     * Adds the specified search result to the grouping.  The specified query
     * text is used to adjust the relevance score.  This method is only called 
     * by SearchResultList after a write lock is obtained on the parent list.
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
        for (RemoteHost host : result.getSources()) {
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
        // Create sorted set of sources.
        Set<RemoteHost> sources = new TreeSet<RemoteHost>(REMOTE_HOST_COMPARATOR);
        sources.addAll(remoteHosts);
        return sources;
    }

    @Override
    public URN getUrn() {
        return coreResults.get(0).getUrn();
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
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
    
    /**
     * Comparator to order RemoteHost objects.
     */
    private static class RemoteHostComparator implements Comparator<RemoteHost> {
        @Override
        public int compare(RemoteHost o1, RemoteHost o2) {
            int compare = 0;
            boolean anonymous1 = o1.getFriendPresence().getFriend().isAnonymous();
            boolean anonymous2 = o2.getFriendPresence().getFriend().isAnonymous();

            if (anonymous1 == anonymous2) {
                String name1 = o1.getFriendPresence().getFriend().getRenderName();
                String name2 = o2.getFriendPresence().getFriend().getRenderName();
                compare = name1.compareToIgnoreCase(name2);
            } else if (anonymous1) {
                compare = 1;
            } else if (anonymous2) {
                compare = -1;
            }
            return compare;
        }
    }
}
