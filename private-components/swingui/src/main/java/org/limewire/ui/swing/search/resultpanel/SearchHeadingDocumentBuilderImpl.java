package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Font;
import java.text.MessageFormat;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

@Singleton
public class SearchHeadingDocumentBuilderImpl implements SearchHeadingDocumentBuilder {
    private final static String DOCUMENT_START = 
        "<html>" +
            "<head>" +
                "<style>" +
                    ".title '{' " +
                        "font-family: {0};" +
                        "color: {1};" +
                        "font-size: {2};'}'" +
                "</style>" +
            "</head>" +
            "<body>";
    static final String DOCUMENT_END = "</body></html>";
    
    private @Resource Font headingFont;
    private @Resource String headingColor;
    final String documentStartHTML;
    
    public SearchHeadingDocumentBuilderImpl() {
        GuiUtils.assignResources(this);
        
        documentStartHTML = MessageFormat.format(DOCUMENT_START, headingFont.getFamily(), 
                headingColor, headingFont.getSize());
    }

    public String getHeadingDocument(String heading, BasicDownloadState downloadState, boolean mouseOver, boolean isSpam) {
        heading = heading.replace("<html>", "").replace("</html>", "");
        StringBuilder bldr = new StringBuilder();
        bldr.append(documentStartHTML);
        String headingText = wrapHeading(heading, false);
        if (isSpam) {
            bldr.append(tr("{0} is Spam", headingText));
        } else {
            switch(downloadState) {
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
