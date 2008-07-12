package org.limewire.core.impl.search;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.SearchResult;
import org.limewire.io.IpPort;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryReply;

public class SearchResultAdapter implements SearchResult {

    private final RemoteFileDesc rfd;
    private final QueryReply qr;
    private final Set<? extends IpPort> locs;

    public SearchResultAdapter(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        this.rfd = rfd;
        this.qr = queryReply;
        this.locs = locs;
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
        if(type == MediaType.getAudioMediaType()) {
            return ResultType.AUDIO;
        } else if(type == MediaType.getVideoMediaType()) {
            return ResultType.VIDEO;
        } else if(type == MediaType.getImageMediaType()) {
            return ResultType.IMAGE;
        } else if(type == MediaType.getDocumentMediaType()) {
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

}
