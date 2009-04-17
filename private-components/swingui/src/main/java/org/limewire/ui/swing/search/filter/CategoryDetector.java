package org.limewire.ui.swing.search.filter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.limewire.core.api.Category;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * Support class to detect the category with the most search results.
 */
public class CategoryDetector {
    /** Number of results needed to trigger category detection. */
    private static final int RESULTS_COUNT = 25;
    /** Delay in milliseconds needed to trigger category detection. */
    private static final int TIMER_DELAY = 10000;

    private final SearchResultsModel searchResultsModel;
    private final CategoryFilter categoryFilter;
    
    private CategoryDetectorListener detectorListener;
    private ListEventListener<VisualSearchResult> resultsListener;
    private ActionListener timerListener;
    private Timer timer;
    
    /**
     * Constructs a CategoryDetector using the specified search results model
     * and category filter.
     */
    public CategoryDetector(SearchResultsModel searchResultsModel, CategoryFilter categoryFilter) {
        this.searchResultsModel = searchResultsModel;
        this.categoryFilter = categoryFilter;
    }
    
    /**
     * Starts category detection.  When the default category is found, the 
     * specified listener is notified.
     */
    public void start(CategoryDetectorListener listener) {
        detectorListener = listener;
        
        // Create results listener to detect the category when enough results 
        // are received.
        resultsListener = new ListEventListener<VisualSearchResult>() {
            @Override
            public void listChanged(ListEvent<VisualSearchResult> listChanges) {
                int size = listChanges.getSourceList().size();
                if (size >= RESULTS_COUNT) {
                    fireCategoryFound();
                }
            }
        };
        searchResultsModel.getObservableSearchResults().addListEventListener(resultsListener);
        
        // Create timer listener to detect the category after enough time as 
        // passed.
        timerListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireCategoryFound();
            }
        };
        timer = new Timer(TIMER_DELAY, timerListener);
        timer.start();
    }
    
    /**
     * Stops category detection.  This method removes all listeners.
     */
    public void stop() {
        // Remove listener from list.
        if (resultsListener != null) {
            searchResultsModel.getObservableSearchResults().removeListEventListener(resultsListener);
            resultsListener = null;
        }
        
        // Stop timer and remove listener.
        if (timer != null) {
            timer.stop();
            timer.removeActionListener(timerListener);
            timerListener = null;
            timer = null;
        }
        
        // Remove detector listener.
        detectorListener = null;
    }
    
    /**
     * Notifies the listener with the default category, and stops category
     * detection.
     */
    private void fireCategoryFound() {
        Category category = categoryFilter.getDefaultCategory();
        detectorListener.categoryFound(category);
        stop();
    }

    /**
     * Defines a listener for category found events. 
     */
    public static interface CategoryDetectorListener {
        
        /**
         * Invoked when the category is found.
         */
        void categoryFound(Category category);
        
    }
}
