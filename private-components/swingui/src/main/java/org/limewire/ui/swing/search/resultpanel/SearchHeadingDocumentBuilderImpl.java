package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import org.limewire.ui.swing.search.model.BasicDownloadState;

public class SearchHeadingDocumentBuilderImpl implements SearchHeadingDocumentBuilder {
    static String DOCUMENT_START = 
        "<html>" +
            "<head>" +
                "<style>" +
                    ".title { " +
                        "font-family: Arial;" +
                        "color: #2152a6;" +
                        "font-size: large;}" +
                "</style>" +
            "</head>" +
            "<body>";
    static String DOCUMENT_END = "</body></html>";

    public String getHeadingDocument(String heading, BasicDownloadState downloadState, boolean mouseOver, boolean isSpam) {
        heading = heading.replace("<html>", "").replace("</html>", "");
        StringBuilder bldr = new StringBuilder();
        bldr.append(DOCUMENT_START);
        String headingText = wrapHeading(heading, false);
        if (isSpam) {
            bldr.append(tr("{0} is Spam", headingText));
        } else {
            switch(downloadState) {
            case PRE_EXISTING_DOWNLOADING:
            case DOWNLOADING:
                bldr.append(tr("You are <a href=\"#downloading\">downloading</a> {0}", headingText));
                break;
            case NOT_STARTED:
                bldr.append(wrapHeading(heading, mouseOver));
                break;
            case DOWNLOADED:
            case LIBRARY:
                String content = tr("{0} is in <a href=\"#library\">Your Library</a>", headingText);
                bldr.append(content);
                break;
            }
        }
        bldr.append(DOCUMENT_END);
        return bldr.toString();
    }

    private String wrapHeading(String heading, boolean mouseOver) {
        return "<span class=\"title\">" + downloadLink(heading, mouseOver) + "</span>";
    }

    private String downloadLink(String heading, boolean mouseOver) {
        if (mouseOver) {
            return "<a href=\"#download\">" + heading + "</a>";
        }
        return heading;
    }
}
