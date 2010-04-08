package org.limewire.core.impl.related;

import com.limegroup.gnutella.URN;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.related.RelatedFiles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.related.FileRelationManager;

@Singleton
class RelatedFilesImpl implements RelatedFiles {

    private static final Log LOG = LogFactory.getLog(RelatedFilesImpl.class);

    private final FileRelationManager manager;

    @Inject
    RelatedFilesImpl(FileRelationManager manager) {
        this.manager = manager;
    }

    @Override
    public void markFileAsBad(org.limewire.core.api.URN sha1) {
        URN coreSha1 = convertToCoreSha1(sha1);
        if(coreSha1 != null)
            manager.markFileAsBad(coreSha1);
    }

    @Override
    public void unmarkFileAsBad(org.limewire.core.api.URN sha1) {
        URN coreSha1 = convertToCoreSha1(sha1);
        if(coreSha1 != null)
            manager.unmarkFileAsBad(coreSha1);
    }

    @Override
    public void markFileAsGood(org.limewire.core.api.URN sha1) {
        URN coreSha1 = convertToCoreSha1(sha1);
        if(coreSha1 != null)
            manager.markFileAsGood(coreSha1);
    }

    @Override
    public int getNumberOfRelatedGoodFiles(org.limewire.core.api.URN sha1) {
        URN coreSha1 = convertToCoreSha1(sha1);
        if(coreSha1 == null)
            return 0;
        else
            return manager.getNumberOfRelatedGoodFiles(coreSha1);
    }

    @Override
    public void increasePlayCount(org.limewire.core.api.URN sha1) {
        URN coreSha1 = convertToCoreSha1(sha1);
        if(coreSha1 != null)
            manager.increasePlayCount(coreSha1);
    }

    @Override
    public float guessDownloadProbability(String filename) {
        return manager.guessDownloadProbability(filename);
    }

    @Override
    public void downloadStarted(String filename) {
        manager.downloadStarted(filename);
    }

    private URN convertToCoreSha1(org.limewire.core.api.URN urn) {
        if(!(urn instanceof URN)) {
            LOG.debug("Not a core URN");
            return null;
        }
        URN coreUrn = (URN)urn;
        if(!coreUrn.isSHA1()) {
            LOG.debug("Not a SHA1 URN");
            return null;
        }
        return coreUrn;
    }
}
