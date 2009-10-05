package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.search.model.BasicDownloadState;

/**
 * Implementation of SearchHeadingDocumentBuilder to create the heading text
 * for search results.
 */
@LazySingleton
public class SearchHeadingDocumentBuilderImpl implements SearchHeadingDocumentBuilder {

    @Override
    public String getHeadingDocument(SearchHeading heading, BasicDownloadState downloadState, boolean isSpam) {
        return getHeadingDocument(heading, downloadState, isSpam, true);
    }

    @Override
    public String getHeadingDocument(SearchHeading heading, BasicDownloadState downloadState,
            boolean isSpam, boolean underline) {
        
        if (isSpam) {
            return tr("{0} is marked as spam.", wrapHeading(heading.getText(), false));
            
        } else {
            switch(downloadState) {
            case DOWNLOADING:
                String downloadMessage = "<a href=\"#downloading\">Downloading</a> {0}...";
                return tr(downloadMessage, wrapHeading(heading.getText(downloadMessage), false));
                
            case NOT_STARTED:
                return wrapHeading(heading.getText(), underline);
                
            case DOWNLOADED:
            case LIBRARY:
                String message = "{0} is in your <a href=\"#library\">Library</a>.";
                return tr(message, wrapHeading(heading.getText(message), false));
                
            default:
                return "";
            }
        }
    }
    
    private String wrapHeading(String heading, boolean underline) {
        return "<span class=\"title\">" + downloadLink(heading, underline) + "</span>";
    }

    private String downloadLink(String heading, boolean underline) {
        if (underline) {
            return "<a href=\"#download\">" + heading + "</a>";
        } else {
            return heading;
        }
    }
}
