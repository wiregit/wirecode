package org.limewire.core.impl.search;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIUtils;
import org.limewire.collection.Tuple;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class TorrentUriPrioritizerImpl implements TorrentUriPrioritizer {
    
    private static final Log LOG = LogFactory.getLog(TorrentUriPrioritizerImpl.class);
    
    private final static Pattern numbers = Pattern.compile("[0-9]+");
    
    private final URI referrer;

    private final String query;
    
    private final String referrerHost;
    
    @SuppressWarnings("unchecked")
    private final List<Predicate<URI>> predicates = ImmutableList.of(
            new IsTorrentUriPredicate(),
            new IsMagnetUriPredicate(),
            new UriSimilarToOtherTorrentUriPredicate(), 
            new UriEndsWithTorrentPredicate(), 
            new UriContainsQueryPredicate(),
            new UriOnSameHostAsReferrerPredicate());
            

    private final String[] queryTokens;

    private final TorrentUriStore torrentUriStore;
    
    @Inject
    public TorrentUriPrioritizerImpl(@Assisted URI referrer, @Assisted String query,
            TorrentUriStore torrentUriStore) {
        this.referrer = referrer;
        this.query = query;
        this.torrentUriStore = torrentUriStore;
        this.queryTokens = toLowerCase(query.split("\\s"));
        String host = getCanonicalHost(referrer);
        this.referrerHost = host != null ? host : "";
    }
    
    private static String getCanonicalHost(URI uri) {
        String host = uri.getHost();
        return host != null ? host.toLowerCase(Locale.US) : null;
    }

    @Override
    public List<URI> prioritize(List<URI> candidates) {
        // remove duplicates
        candidates = uniquify(candidates);
        // remove known non torrent uris
        int size = candidates.size();
        candidates = filter(candidates, new NotTorrenUriPredicate());
        LOG.debugf("removed non torrents: {0}", size - candidates.size());
        // compute scores
        List<Tuple<URI, Integer>> scoredUris = transform(candidates, new TorrentUriLikelihoodFunction());
        // sort by how likely a candidate
        Collections.sort(scoredUris, new ScoreComparator());
        // transform back
        return transform(scoredUris, new UriExtractor());
    }
    
    static <S, T> List<T> transform(List<S> list, Function<S, T> function) {
        List<T> transformed = new ArrayList<T>(list.size());
        for (S element : list) {
            transformed.add(function.apply(element));
        }
        return transformed;
    }

    <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        List<T> filtered = new ArrayList<T>(list.size());
        for (T element : list) {
            if (predicate.apply(element)) {
                filtered.add(element);
            }
        }
        return filtered;
    }

    private List<URI> uniquify(List<URI> candidates) {
        return new ArrayList<URI>(new HashSet<URI>(candidates));
    }

    @Override
    public void setIsTorrent(URI uri, boolean isTorrent) {
        torrentUriStore.setIsTorrentUri(uri, isTorrent);
        if (isTorrent) {
            String host = getCanonicalHost(uri);
            String path = uri.getPath();
            if (host == null || path == null) {
                LOG.debugf("host or path null {0}, {1}", host, path);
                return;
            }
            List<String> tokens = tokenize(path);
            String canonicalPath = "/" + StringUtils.explode(tokens, "/");
            uri = URIUtils.resolve(uri, canonicalPath);
            LOG.debugf("canonicalized uri: {0}", uri);
            torrentUriStore.addCanonicalTorrentUris(host, uri);
        }
    }
    
    public static void main(String[] args) throws URISyntaxException {
        URI uri = org.limewire.util.URIUtils.toURI("http://torrent/download/134545/query.torrent");
        System.out.println(uri.getPath());
        System.out.println(Arrays.asList(uri.getPath().split("[/?#]")));
    }
    
    Set<URI> getTorrentUrisForDomain(URI uri) {
        String host = getCanonicalHost(uri);
        if (host != null) {
           return torrentUriStore.getTorrentUrisForHost(host);
        }
        return Collections.emptySet();
    }
    
    boolean isStructurallySimilar(URI uri, Iterable<URI> torrentUris) {
        for (URI torrentUri : torrentUris) {
            if (isStructurallySimilar(uri, torrentUri)) {
                return true;
            }
        }
        return false;
    }
    
    List<String> tokenize(String path) {
        String[] tokens = path.split("[/?#]");
        List<String> canonicalized = new ArrayList<String>(tokens.length);
        for (String token : tokens) {
            if (containsQuery(token)) {
                canonicalized.add("*query*");
            } else if (numbers.matcher(token).matches()) {
                canonicalized.add("*numbers*");
            } else if (!token.isEmpty()) {
                canonicalized.add(token);
            }
        }
        return canonicalized;
    }
    
    boolean isStructurallySimilar(URI uri, URI torrentUri) {
        String path = uri.getPath();
        String torrentPath = torrentUri.getPath();
        if (path == null || torrentPath == null) {
            return false;
        }
        int score = 0;
        List<String> pathTokens = tokenize(path);
        List<String> torrentPathTokens = tokenize(torrentPath);
        if (pathTokens.size() == torrentPathTokens.size()) {
            score += 1;
        }
        for (Tuple<String, String> tuple : zip(pathTokens, torrentPathTokens)) {
            if (tuple.getFirst().equals(tuple.getSecond())) {
                score += 1;
            } else {
                score -= 1;
            }
        }
        return score > 3;
    }
    
    boolean containsQuery(String value) {
        value = value.toLowerCase();
        for (String token : queryTokens) {
            if (!value.contains(token)) {
                return false;
            }
        }
        return true;
    }
    
    private int computeScore(URI uri) {
        int score = 0;
        for (Predicate<URI> predicate : predicates) {
            if (predicate.apply(uri)) {
                score += 1;
            }
            score <<= 1;
        }
        return score;
    }
    
    private static String[] toLowerCase(String...tokens) {
        List<String> results = new ArrayList<String>(tokens.length);
        for (String token : tokens) {
            results.add(token.toLowerCase());
        }
        return results.toArray(new String[results.size()]);
    }
    
    private class IsTorrentUriPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return torrentUriStore.isTorrentUri(uri);
        }
    }
    
    private class IsMagnetUriPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return "magnet".equalsIgnoreCase(uri.getScheme());
        }
    }
    
    private class UriSimilarToOtherTorrentUriPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            Set<URI> torrentUris = getTorrentUrisForDomain(uri);
            if (torrentUris.isEmpty()) {
                return false;
            }
            return isStructurallySimilar(uri, torrentUris);
        }
    }
    
    private class UriContainsQueryPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return containsQuery(uri.toString());
        }
    }
    
    private class UriEndsWithTorrentPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            String path = uri.getPath();
            if (path != null) {
                return FileUtils.getFileExtension(uri.getPath()).equalsIgnoreCase("torrent");
            }
            return false;
        }
    }
    
    private class UriOnSameHostAsReferrerPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return referrerHost.equals(getCanonicalHost(uri));
        }
    }
    
    private class NotTorrenUriPredicate implements Predicate<URI> {
        
        @Override
        public boolean apply(URI uri) {
            return !torrentUriStore.isNotTorrentUri(uri);
        }
        
    }
    
    private class TorrentUriLikelihoodFunction implements Function<URI, Tuple<URI, Integer>> {
        @Override
        public Tuple<URI, Integer> apply(URI uri) {
            return new Tuple<URI, Integer>(uri, computeScore(uri));
        }
    }
    
    private class ScoreComparator implements Comparator<Tuple<URI, Integer>> {
        @Override
        public int compare(Tuple<URI, Integer> o1, Tuple<URI, Integer> o2) {
            return o2.getSecond().compareTo(o1.getSecond());
        }
    }
    
    private class UriExtractor implements Function<Tuple<URI, Integer>, URI> {
        @Override
        public URI apply(Tuple<URI, Integer> tuple) {
            return tuple.getFirst();
        }
    }

    static <S, T> Iterable<Tuple<S, T>> zip(final Iterable<S> iterableS, final Iterable<T> iterableT) {
        return new Iterable<Tuple<S,T>>() {
            @Override
            public Iterator<Tuple<S, T>> iterator() {
                return new ZipIterator<S, T>(iterableS.iterator(), iterableT.iterator());
            }
        };
    }
    
    private static class ZipIterator<S, T> implements Iterator<Tuple<S, T>> {
        
        private final Iterator<S> iteratorS;
        private final Iterator<T> iteratorT;

        public ZipIterator(Iterator<S> iteratorS, Iterator<T> iteratorT) {
            this.iteratorS = iteratorS;
            this.iteratorT = iteratorT;
        }

        @Override
        public boolean hasNext() {
            return iteratorS.hasNext() && iteratorT.hasNext();
        }
        
        @Override
        public Tuple<S, T> next() {
            return new Tuple<S, T>(iteratorS.next(), iteratorT.next());
        }
        
        @Override
        public void remove() {
            iteratorS.remove();
            iteratorT.remove();
        }
        
    }
}
