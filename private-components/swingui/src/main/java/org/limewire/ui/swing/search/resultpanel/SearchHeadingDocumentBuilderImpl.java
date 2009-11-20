package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.text.MessageFormat;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.search.model.BasicDownloadState;

@LazySingleton
public class SearchHeadingDocumentBuilderImpl implements SearchHeadingDocumentBuilder {

    public String getHeadingDocument(SearchHeading heading, BasicDownloadState downloadState, boolean isSpam) {
        if (isSpam) {
            return tr("{0} is marked as spam.", wrapHeading(heading.getText(), false));
        } else {
            switch(downloadState) {
            case DOWNLOADING:
                String downloadMessage = tr("<a href=\"#downloading\">Downloading</a> {0}...");
                return MessageFormat.format(downloadMessage, wrapHeading(heading.getText(downloadMessage), false));
            case NOT_STARTED:
                return wrapHeading(heading.getText(), true);
            case DOWNLOADED:
            case LIBRARY:
                String message = tr("{0} is in your <a href=\"#library\">Library</a>.");
                return MessageFormat.format(message, wrapHeading(heading.getText(message), false));
            }
        }
        return "";
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
