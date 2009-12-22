package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.model.BasicDownloadState;

/**
 * Defines a factory to build the heading text for search results.
 */
public interface SearchHeadingDocumentBuilder {

    /**
     * Returns the heading text for the specified heading object, download 
     * state, and spam indicator.
     */
    String getHeadingDocument(SearchHeading heading, BasicDownloadState downloadState, 
            boolean isSpam);

    /**
     * Returns the heading text for the specified heading object, download 
     * state, spam indicator, and underline indicator.
     */
    String getHeadingDocument(SearchHeading heading, BasicDownloadState downloadState, 
            boolean isSpam, boolean underline);
}
