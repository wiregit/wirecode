package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.endpoint.RemoteHostAction;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;

public class MockSearch implements Search {
    
    private SearchListener listener;

    public MockSearch(SearchDetails searchDetails) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
    }
    
    @Override
    public SearchCategory getCategory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void start(final SearchListener searchListener) {
        listener = searchListener;
        addResults("");
    }
    
    private void addResults(final String suffix) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                listener.handleSearchResult(
                        new MockSearchResult("5.0. Blues" + suffix, "ogg", ResultType.AUDIO, 123456789L, Arrays.asList(new MockRemoteHost("bob")), "test" + suffix));
                listener.handleSearchResult(
                        new MockSearchResult("5.0 Blues Source 2" + suffix, "ogg", ResultType.AUDIO, 1234789L, Arrays.asList(new MockRemoteHost("tom"), new MockRemoteHost("bob")), "test" + suffix));
                listener.handleSearchResult(
                        new MockSearchResult("Standdown Situps" + suffix, "ogv", ResultType.VIDEO, 123456L, Arrays.asList(new MockRemoteHost("dick")), "test1" + suffix));
                listener.handleSearchResult(
                        new MockSearchResult("Craziness" + suffix, "tmp", ResultType.UNKNOWN, 1L, Arrays.asList(new MockRemoteHost("harry"), new MockRemoteHost("larry")), "test2" + suffix));
            }
        }).start();
    }
    
    @Override
    public void repeat() {
        addResults(" (from repeat)");
    }

    private static class MockSearchResult implements SearchResult {
        private final String description;
        private final String extension;
        private final ResultType resultType;
        private final long size;
        private final List<RemoteHost> sources;
        private final String urn;

        public MockSearchResult(String description, String extension,
                ResultType resultType, long size, List<? extends RemoteHost> sources,
                String urn) {
            this.description = description;
            this.extension = extension;
            this.resultType = resultType;
            this.size = size;
            this.sources = new ArrayList<RemoteHost>(sources);
            this.urn = urn;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getFileExtension() {
            return extension;
        }

        @Override
        public Map<Object, Object> getProperties() {
            return Collections.emptyMap();
        }

        @Override
        public ResultType getResultType() {
            return resultType;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public List<RemoteHost> getSources() {
            return sources;
        }

        @Override
        public String getUrn() {
            return urn;
        }
    }

    private static class MockRemoteHost implements RemoteHost {
        private final String description;

        public MockRemoteHost(String description) {
            this.description = description;
        }

        @Override
        public List<RemoteHostAction> getHostActions() {
            return Collections.emptyList();
        }

        @Override
        public String getHostDescription() {
            return description;
        }
    }

}
