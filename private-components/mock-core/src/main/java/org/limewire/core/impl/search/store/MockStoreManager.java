package org.limewire.core.impl.search.store;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.core.impl.MockURN;
import org.limewire.core.impl.search.store.MockStoreResult.MockAlbumIcon;

import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the mock core.
 */
@Singleton
public class MockStoreManager implements StoreManager {

    @Override
    public boolean isDownloadApproved(StoreResult storeResult) {
        return false;
    }

    @Override
    public boolean isDownloadApproved(StoreTrackResult trackResult) {
        return false;
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public void startSearch(final SearchDetails searchDetails, final StoreListener storeListener) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                
                // Get query text.
                String query = searchDetails.getSearchQuery();
                
                // Create mock store style.
                StoreStyle storeStyle;
                if (query.indexOf("monkey") > -1) {
                    storeStyle = new MockStoreStyle(Type.STYLE_A);
                } else if (query.indexOf("bear") > -1) {
                    storeStyle = new MockStoreStyle(Type.STYLE_B);
                } else if (query.indexOf("cat") > -1) {
                    storeStyle = new MockStoreStyle(Type.STYLE_C);
                } else if (query.indexOf("dog") > -1) {
                    storeStyle = new MockStoreStyle(Type.STYLE_D);
                } else {
                    storeStyle = new MockStoreStyle(Type.STYLE_A);
                }
                
                // Create mock store results.
                StoreResult[] storeResults = createStoreResults(0);
                
                // Fire event to update style.
                storeListener.styleUpdated(storeStyle);
                
                // Fire event to handle results.
                storeListener.resultsFound(storeResults);
            }
        }).start();
    }
    
    /**
     * Creates an array of store results based on the specified index.
     */
    private StoreResult[] createStoreResults(int i) {
        List<StoreResult> resultList = new ArrayList<StoreResult>();

        // Create album with multiple tracks.
        URN urn = new MockURN("www.store.limewire.com" + i);
        MockStoreResult msr = new MockStoreResult(urn, Category.AUDIO);
        msr.setAlbumIcon(new MockAlbumIcon(Color.RED, 50));
        msr.setFileExtension("mp3");
        msr.setFileName("Green Monkeys The Collection.mp3");
        msr.setInfoURI("http://www.store.limewire.com/store/app/pages/album/Album/productId/210687/");
        msr.setPrice("4 Credits");
        msr.setProperty(FilePropertyKey.AUTHOR, "Green Monkeys");
        msr.setProperty(FilePropertyKey.ALBUM, "The Collection That Keeps on Playing and Playing and Playing and Playing and Playing");
        msr.setProperty(FilePropertyKey.TITLE, "The Collection That Keeps on Playing and Playing and Playing and Playing and Playing");
        msr.setProperty(FilePropertyKey.LENGTH, Long.valueOf(568));
        msr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        msr.setSize(9 * 1024 * 1024);

        MockStoreTrackResult mstr = new MockStoreTrackResult();
        mstr.setExtension("mp3");
        mstr.setUrn("www.store.limewire.com" + (i + 1));
        mstr.setPrice("1 Credit");
        mstr.setProperty(FilePropertyKey.AUTHOR, "Green Monkeys");
        mstr.setProperty(FilePropertyKey.TITLE, "Heh?");
        mstr.setProperty(FilePropertyKey.LENGTH, Long.valueOf(129));
        mstr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        mstr.setSize(3 * 1024 * 1024);
        msr.addAlbumResult(mstr);

        mstr = new MockStoreTrackResult();
        mstr.setExtension("mp3");
        mstr.setUrn("www.store.limewire.com" + (i + 2));
        mstr.setPrice("1 Credit");
        mstr.setProperty(FilePropertyKey.AUTHOR, "Green Monkeys");
        mstr.setProperty(FilePropertyKey.TITLE, "Take Me To Space (Man)");
        mstr.setProperty(FilePropertyKey.LENGTH, Long.valueOf(251));
        mstr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        mstr.setSize(3 * 1024 * 1024);
        msr.addAlbumResult(mstr);

        mstr = new MockStoreTrackResult();
        mstr.setExtension("mp3");
        mstr.setUrn("www.store.limewire.com" + (i + 3));
        mstr.setPrice("1 Credit");
        mstr.setProperty(FilePropertyKey.AUTHOR, "Green Monkeys");
        mstr.setProperty(FilePropertyKey.TITLE, "Crush");
        mstr.setProperty(FilePropertyKey.LENGTH, Long.valueOf(188));
        mstr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        mstr.setSize(3 * 1024 * 1024);
        msr.addAlbumResult(mstr);

        resultList.add(msr);

        // Create single file result.
        urn = new MockURN("www.store.limewire.com" + (i + 10));
        msr = new MockStoreResult(urn, Category.AUDIO);
        msr.setFileExtension("mp3");
        msr.setFileName("Green Monkeys Chomp.mp3");
        msr.setInfoURI("http://www.store.limewire.com/store/app/pages/album/Album/productId/210687/");
        msr.setPrice("1 Credit");
        msr.setProperty(FilePropertyKey.AUTHOR, "Green Monkeys");
        msr.setProperty(FilePropertyKey.ALBUM, "Premonitions, Echoes & Science");
        msr.setProperty(FilePropertyKey.TITLE, "Chomp");
        msr.setProperty(FilePropertyKey.LENGTH, Long.valueOf(208));
        msr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        msr.setSize(6 * 1024 * 1024);

        mstr = new MockStoreTrackResult();
        mstr.setExtension("mp3");
        mstr.setUrn("www.store.limewire.com" + (i + 11));
        mstr.setPrice("1 Credit");
        mstr.setProperty(FilePropertyKey.AUTHOR, "Green Monkeys");
        mstr.setProperty(FilePropertyKey.ALBUM, "Premonitions, Echoes & Science");
        mstr.setProperty(FilePropertyKey.TITLE, "Chomp");
        mstr.setProperty(FilePropertyKey.LENGTH, Long.valueOf(208));
        mstr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        mstr.setSize(3 * 1024 * 1024);
        msr.addAlbumResult(mstr);

        resultList.add(msr);

        return resultList.toArray(new StoreResult[resultList.size()]);
    }
}
