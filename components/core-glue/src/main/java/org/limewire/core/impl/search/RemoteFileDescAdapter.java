package org.limewire.core.impl.search;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.endpoint.RemoteHostAction;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.SearchResult;
import org.limewire.io.IpPort;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryReply;

public class RemoteFileDescAdapter implements SearchResult {

    private final RemoteFileDesc rfd;
    private final List<IpPort> locs;

    public RemoteFileDescAdapter(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        this.rfd = rfd;
        this.locs = new ArrayList<IpPort>(locs);
    }
    
    public RemoteFileDesc getRfd() {
        return rfd;
    }
    
    public List<IpPort> getAlts() {
        return locs;
    }

    @Override
    public String getDescription() {
        return rfd.toString();
    }

    @Override
    public String getFileExtension() {
        return FileUtils.getFileExtension(rfd.getFileName());
    }

    @Override
    public Map<Object, Object> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public ResultType getResultType() {
        MediaType type = MediaType.getMediaTypeForExtension(getFileExtension());
        if (type == MediaType.getAudioMediaType()) {
            return ResultType.AUDIO;
        } else if (type == MediaType.getVideoMediaType()) {
            return ResultType.VIDEO;
        } else if (type == MediaType.getImageMediaType()) {
            return ResultType.IMAGE;
        } else if (type == MediaType.getDocumentMediaType()) {
            return ResultType.DOCUMENT;
        } else {
            return ResultType.UNKNOWN;
        }
    }

    @Override
    public long getSize() {
        return rfd.getFileSize();
    }

    @Override
    public String getUrn() {
        return rfd.getSHA1Urn().toString();
    }

    @Override
    public List<RemoteHost> getSources() {
        return new AbstractList<RemoteHost>() {
            @Override
            public RemoteHost get(final int index) {
                if (index == 0) {
                    return new RemoteHost() {
                        @Override
                        public List<RemoteHostAction> getHostActions() {
                            return Collections.emptyList();
                        }

                        @Override
                        public String getHostDescription() {
                            return rfd.getInetSocketAddress().toString();
                        }
                    };
                } else {
                    return new RemoteHost() {
                        @Override
                        public List<RemoteHostAction> getHostActions() {
                            return Collections.emptyList();
                        }

                        @Override
                        public String getHostDescription() {
                            return locs.get(index - 1).getInetSocketAddress().toString();
                        }
                    };
                }
            }

            @Override
            public int size() {
                return 1 + locs.size();
            }

        };
    }
}
