/**
 * 
 */
package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.SearchResult;
import org.limewire.util.FileUtils;

public class TestSearchResult implements SearchResult {

    private String urn;

    private String fileName;

    private Map<PropertyKey, Object> properties;

    public TestSearchResult(String urn, String fileName) {
        this.urn = urn;
        Map<PropertyKey, Object> properties = new HashMap<PropertyKey, Object>();
        properties.put(PropertyKey.NAME, FileUtils.getFilenameNoExtension(fileName));
        this.properties = properties;
        this.fileName = fileName;
    }

    public TestSearchResult(String fileName, Map<PropertyKey, Object> properties) {
        this.urn = UUID.randomUUID().toString();
        this.properties = properties;
        this.fileName = fileName;
    }

    @Override
    public String getFileExtension() {
        return FileUtils.getFileExtension(getFileName());
    }

    @Override
    public Map<PropertyKey, Object> getProperties() {
        return properties;
    }

    @Override
    public Object getProperty(PropertyKey key) {
        return properties.get(key);
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public List<RemoteHost> getSources() {
        List<RemoteHost> sources = new ArrayList<RemoteHost>();
        sources.add(new RemoteHost() {
            UUID randomUUID = UUID.randomUUID();

            @Override
            public String getRenderName() {
                return randomUUID.toString();
            }

            @Override
            public boolean isBrowseHostEnabled() {
                return false;
            }

            @Override
            public boolean isChatEnabled() {
                return false;
            }

            @Override
            public boolean isSharingEnabled() {
                return false;
            }

            @Override
            public FriendPresence getFriendPresence() {
                return null;
            }
        });
        return sources;
    }

    @Override
    public URN getUrn() {
        return new TestURN(urn);
    }

    public String toString() {
        return getUrn() + " - " + getProperty(PropertyKey.NAME);
    }

    @Override
    public Category getCategory() {
        return null;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    private class TestURN implements URN {
        private String urn;

        public TestURN(String urn) {
            this.urn = urn;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TestURN) {
                TestURN oUrn = (TestURN) obj;
                return urn.equals(oUrn);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return urn.hashCode();
        }

        @Override
        public int compareTo(URN o) {
            if (o instanceof TestURN) {
                TestURN testURN = (TestURN) o;
                return urn.compareTo(testURN.urn);
            }
            return -1;
        }

        @Override
        public String toString() {
            return urn;
        }
    }
}