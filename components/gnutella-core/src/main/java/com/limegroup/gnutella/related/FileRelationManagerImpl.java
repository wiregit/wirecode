package com.limegroup.gnutella.related;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.friend.GnutellaPresence;
import org.limewire.inject.LazySingleton;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.related.annotations.BadFileCache;
import com.limegroup.gnutella.related.annotations.ExtensionCountCache;
import com.limegroup.gnutella.related.annotations.GoodFileCache;
import com.limegroup.gnutella.related.annotations.PlayCountCache;

@LazySingleton
class FileRelationManagerImpl implements FileRelationManager {

    private static final int MAX_BROWSES_IN_PROGRESS = 3;
    private static final byte[] NULL_GUID = new byte[16];
    private static final int GOOD_FILE_PLAY_COUNT = 3;

    private static final Log LOG =
        LogFactory.getLog(FileRelationManagerImpl.class);

    private final BrowseFactory browseFactory;
    private final URNFilter urnFilter;
    private final FileRelationCache fileRelationCache;
    private final Cache<URN> goodFileCache, badFileCache;
    private final CountCache<URN> playCountCache;
    private final CountCache<String> extensionCountCache; // Lock before accessing

    // LOCKING: this for browsesInProgress, browsedOrQueued and queue.
    private int browsesInProgress = 0;
    private final Set<BrowseRecord> browsedOrQueued = new HashSet<BrowseRecord>();
    private final LinkedList<BrowseRecord> queue = new LinkedList<BrowseRecord>();

    @Inject
    FileRelationManagerImpl(BrowseFactory browseFactory, URNFilter urnFilter,
            FileRelationCache fileRelationCache,
            @GoodFileCache Cache<URN> goodFileCache,
            @BadFileCache Cache<URN> badFileCache,
            @PlayCountCache CountCache<URN> playCountCache,
            @ExtensionCountCache CountCache<String> extensionCountCache) {
        this.browseFactory = browseFactory;
        this.urnFilter = urnFilter;
        this.fileRelationCache = fileRelationCache;
        this.goodFileCache = goodFileCache;
        this.badFileCache = badFileCache;
        this.playCountCache = playCountCache;
        this.extensionCountCache = extensionCountCache;
    }

    @Override
    public void markFileAsBad(URN sha1) {
        fileRelationCache.removeAllBrowses(sha1);
        goodFileCache.remove(sha1);
        playCountCache.remove(sha1);
        badFileCache.add(sha1);
    }

    @Override
    public void unmarkFileAsBad(URN sha1) {
        badFileCache.remove(sha1);
    }

    @Override
    public void markFileAsGood(URN sha1) {
        badFileCache.remove(sha1);
        goodFileCache.add(sha1);
    }

    @Override
    public synchronized void chunkDownloaded(RemoteFileDesc rfd) {
        byte[] guid = rfd.getClientGUID();
        if(Arrays.equals(guid, NULL_GUID)) {
            LOG.debug("No GUID");
            return;
        }
        Address address = rfd.getAddress();
        if(address == null) {
            LOG.debug("No address");
            return;
        }
        URN sha1 = rfd.getSHA1Urn();
        if(sha1 == null) {
            LOG.debug("No SHA1");
            return;
        }
        if(!rfd.isBrowseHostEnabled()) {
            LOG.debug("Not browseable");
            return;
        }
        BrowseRecord record = new BrowseRecord(sha1, address, guid);
        if(!browsedOrQueued.add(record)) {
            if(LOG.isDebugEnabled())
                LOG.debug("Already browsed or queued " + record);
            return;
        }
        if(browsesInProgress == MAX_BROWSES_IN_PROGRESS) {
            if(LOG.isDebugEnabled())
                LOG.debug("Queueing " + record);
            queue.add(record);
            return;
        }
        browse(record);
    }

    // LOCKING: this
    private void browse(BrowseRecord record) {
        browsesInProgress++;
        GnutellaPresence presence;
        Address address = record.address;
        if(address instanceof Connectable) {
            if(LOG.isDebugEnabled())
                LOG.debug("Browsing " + record + " with Connectable");
            presence = new GnutellaPresence.GnutellaPresenceWithConnectable(
                    (Connectable)address);
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug("Browsing " + record + " with GUID");
            presence = new GnutellaPresence.GnutellaPresenceWithGuid(address,
                    record.guid);
        }
        browseFactory.createBrowse(presence).start(new Browser());
    }

    @Override
    public int getNumberOfRelatedGoodFiles(URN sha1) {
        int good = 0, unknown = 0;
        for(URN related : fileRelationCache.getRelated(sha1)) {
            if(goodFileCache.contains(related))
                good++;
            else
                unknown++;
        }
        if(LOG.isDebugEnabled() && good + unknown > 0)
            LOG.debug(good + " good and " + unknown + " unknown related files");
        return good;
    }

    @Override
    public void increasePlayCount(URN sha1) {
        if(playCountCache.increment(sha1) == GOOD_FILE_PLAY_COUNT)
            markFileAsGood(sha1);
    }

    @Override
    public float guessDownloadProbability(String filename) {
        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        if(extension.isEmpty())
            return 0;
        synchronized(extensionCountCache) {
            int count = extensionCountCache.get(extension);
            if(count == 0)
                return 0;
            int total = extensionCountCache.total();
            assert count <= total;
            return (float)count / total;
        }
    }

    @Override
    public void downloadStarted(String filename) {
        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        if(extension.isEmpty())
            return;
        synchronized(extensionCountCache) {
            extensionCountCache.increment(extension);
        }
    }

    private synchronized void browseFinished() {
        browsesInProgress--;
        BrowseRecord record = queue.poll();
        if(record == null)
            LOG.debug("Queue is empty");
        else
            browse(record);
    }

    private static class BrowseRecord {

        private final URN sha1;
        private final Address address;
        private final byte[] guid;

        BrowseRecord(URN sha1, Address connectable, byte[] guid) {
            this.sha1 = sha1;
            this.address = connectable;
            this.guid = guid;
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof BrowseRecord) {
                BrowseRecord br = (BrowseRecord)o;
                return sha1.equals(br.sha1) &&
                address.equals(br.address) &&
                Arrays.equals(guid, br.guid);
            }
            return false;
        }

        @Override
        public String toString() {
            return address.getAddressDescription();
        }
    }

    private class Browser implements BrowseListener {

        private final SortedSet<URN> browseUrns;

        Browser() {
            browseUrns = new TreeSet<URN>();
        }

        @Override
        public void handleBrowseResult(SearchResult result) {
            org.limewire.core.api.URN urn = result.getUrn();
            if(!(urn instanceof URN)) {
                LOG.debug("Browse returned non-SHA1 URN");
                return;
            }
            URN coreUrn = (URN)urn;
            if(!coreUrn.isSHA1()) {
                LOG.debug("Browse returned non-core URN");
                return;
            }
            browseUrns.add(coreUrn);
        }

        @Override
        public void browseFinished(boolean success) {
            if(success) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Browsed " + browseUrns.size() + " URNs");
                for(URN urn : browseUrns) {
                    if(badFileCache.contains(urn) ||
                            urnFilter.isBlacklisted(urn)) {
                        LOG.debug("Discarding browse with bad URNs");
                        return;
                    }
                }
                fileRelationCache.addRelations(browseUrns);
            } else {
                LOG.debug("Browse failed");
            }
            FileRelationManagerImpl.this.browseFinished();
        }
    }
}
