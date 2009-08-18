package org.limewire.core.impl.search.store;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.core.impl.MockURN;
import org.limewire.core.impl.search.store.MockStoreResult.MockAlbumIcon;

import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the mock core.
 */
@Singleton
public class MockStoreManager implements StoreManager {

    private final CopyOnWriteArrayList<StoreListener> listeners = 
        new CopyOnWriteArrayList<StoreListener>();
    
    private volatile StoreStyle storeStyle;

    @Override
    public void addStoreListener(StoreListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeStoreListener(StoreListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }
    
    @Override
    public StoreStyle getStoreStyle() {
        return storeStyle;
    }

    @Override
    public void startSearch(final SearchDetails searchDetails) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                
                // Get query text.
                String query = searchDetails.getSearchQuery();
                
                // Create mock store style.
                if (query.indexOf("monkey") > -1) {
                    storeStyle = new MockStoreStyle(Type.STYLE_A);
                } else if (query.indexOf("bear") > -1) {
                    storeStyle = new MockStoreStyle(Type.STYLE_B);
                } else if (query.indexOf("cat") > -1) {
                    storeStyle = new MockStoreStyle(Type.STYLE_C);
                } else if (query.indexOf("dog") > -1) {
                    storeStyle = new MockStoreStyle(Type.STYLE_D);
                }
                
                // Create mock store results.
                StoreResult[] storeResults = createStoreResults(0);
                
                // Fire event to update style.
                fireStyleUpdated(storeStyle);
                
                // Fire event to handle results.
                fireResultsFound(storeResults);
            }
        }).start();
    }

    /**
     * Notifies registered listeners to handle the specified store results.
     */
    private void fireResultsFound(StoreResult[] storeResults) {
        for (StoreListener listener : listeners) {
            listener.resultsFound(storeResults);
        }
    }
    
    /**
     * Notifies registered listeners to update the specified store style.
     */
    private void fireStyleUpdated(StoreStyle storeStyle) {
        for (StoreListener listener : listeners) {
            listener.styleUpdated(storeStyle);
        }
    }
    
    /**
     * Creates an array of store results based on the specified index.
     */
    private StoreResult[] createStoreResults(int i) {
        List<StoreResult> resultList = new ArrayList<StoreResult>();

        // Create album with multiple tracks.
        URN urn = new MockURN("www.store.limewire.com" + i);
        MockStoreResult msr = new MockStoreResult(urn, Category.AUDIO);
        msr.setFileExtension("mp3");
        msr.setAlbumIcon(new MockAlbumIcon(Color.RED, 50));
        msr.setPrice("4 Credits");
        msr.setProperty(FilePropertyKey.AUTHOR, "Green Monster");
        //msr.setProperty(FilePropertyKey.NAME, "Premonitions, Monkeys & Science");
        msr.setProperty(FilePropertyKey.NAME, "When Everyone has a Sweet Party and you're invited! I was at this totally swinging hepcat party last weekend. Oh man, that joint was jumpin!");
        msr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        msr.setSize(9 * 1024 * 1024);

        MockStoreTrackResult mstr = new MockStoreTrackResult();
        mstr.setExtension("mp3");
        mstr.setUrn("www.store.limewire.com" + (i + 1));
        mstr.setPrice("1 Credit");
        mstr.setProperty(FilePropertyKey.AUTHOR, "Green Monster");
        mstr.setProperty(FilePropertyKey.NAME, "Heh?");
        mstr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        mstr.setSize(3 * 1024 * 1024);
        msr.addAlbumResult(mstr);

        mstr = new MockStoreTrackResult();
        mstr.setExtension("mp3");
        mstr.setUrn("www.store.limewire.com" + (i + 2));
        mstr.setPrice("1 Credit");
        mstr.setProperty(FilePropertyKey.AUTHOR, "Green Monster");
        mstr.setProperty(FilePropertyKey.NAME, "Take Me To Space (Man)");
        mstr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        mstr.setSize(3 * 1024 * 1024);
        msr.addAlbumResult(mstr);

        mstr = new MockStoreTrackResult();
        mstr.setExtension("mp3");
        mstr.setUrn("www.store.limewire.com" + (i + 3));
        mstr.setPrice("1 Credit");
        mstr.setProperty(FilePropertyKey.AUTHOR, "Green Monster");
        mstr.setProperty(FilePropertyKey.NAME, "Crush");
        mstr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        mstr.setSize(3 * 1024 * 1024);
        msr.addAlbumResult(mstr);

        resultList.add(msr);

        // Create single file result.
        urn = new MockURN("www.store.limewire.com" + (i + 10));
        msr = new MockStoreResult(urn, Category.AUDIO);
        msr.setFileExtension("mp3");
        msr.setPrice("1 Credit");
        msr.setProperty(FilePropertyKey.AUTHOR, "Green Monster");
        msr.setProperty(FilePropertyKey.ALBUM, "Premonitions, Echoes & Science");
        msr.setProperty(FilePropertyKey.NAME, "Chomp");
        msr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        msr.setSize(6 * 1024 * 1024);

        mstr = new MockStoreTrackResult();
        mstr.setExtension("mp3");
        mstr.setUrn("www.store.limewire.com" + (i + 11));
        mstr.setPrice("1 Credit");
        mstr.setProperty(FilePropertyKey.AUTHOR, "Green Monster");
        mstr.setProperty(FilePropertyKey.ALBUM, "Premonitions, Echoes & Science");
        mstr.setProperty(FilePropertyKey.NAME, "Chomp");
        mstr.setProperty(FilePropertyKey.QUALITY, Long.valueOf(3));
        mstr.setSize(3 * 1024 * 1024);
        msr.addAlbumResult(mstr);

        resultList.add(msr);

        return resultList.toArray(new StoreResult[resultList.size()]);
    }
}
