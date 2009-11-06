package org.limewire.ui.swing.pro;

import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.color.ColorUtil;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.Application;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.components.HTMLPane.LoadResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.EncodingUtils;

import com.google.inject.Inject;

/** A nag to go to LimeWire PRO. */ 
class ProNag extends JXPanel {
    
    private NagContainer container;
    private final Application application;
    private final HTMLPane editorPane;
    
    private long offlineShownAt = -1;
    
    @Inject public ProNag(Application application, 
                          final GnutellaConnectionManager connectionManager) {
        super(new BorderLayout());
        
        this.application = application;        
        this.editorPane = new HTMLPane();
        editorPane.putClientProperty(BasicHTML.documentBaseKey, "");

        setOpaque(false);

        editorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // Look for the HREF attribute of the A tag,
                    // or the ACTION attribute (which will be there if it was a form)
                    Object a = e.getSourceElement().getAttributes().getAttribute(HTML.Tag.A);
                    Object action = e.getSourceElement().getAttributes().getAttribute(HTML.Attribute.ACTION);
                    Object href = "";
                    if(a instanceof AttributeSet) {
                        href = ((AttributeSet)a).getAttribute(HTML.Attribute.HREF);
                    } else if(action instanceof String) {
                        href = action;
                    }
                    
                    if(href != null && href.equals("_hide_nag_")) {
                        container.dispose();
                    } else if(e.getURL() != null) {
                        String url = e.getURL().toExternalForm();
                        url += "&gs=" + connectionManager.getConnectionStrength().getStrengthId();
                        // If the offline was shown, add the delay between shown & clicked in ms.
                        if(offlineShownAt > 0) {
                            long delay = System.currentTimeMillis() - offlineShownAt;
                            url += "&offlineDelay=" + delay;
                        }
                        NativeLaunchUtils.openURL(url);
                        container.dispose();
                    }
                }
            }            
        });
        
        add(editorPane);
    }

    boolean isModal() {
        Document document = editorPane.getDocument();
        if(document instanceof HTMLDocument) {
            HTMLDocument html = (HTMLDocument)document;
            Element element = html.getElement(html.getDefaultRootElement(), "modal", "false");
            if(element != null && element.getName().equals("body")) {
                return false;
            }
        }
        return true;
    }
    
    /** Returns true if any content was loaded. */
    boolean hasContent() {
        Document document = editorPane.getDocument();
        if (document != null) {
            return document.getLength() > 0;
        } else {
            return false;
        }
    }

    /** Sets the container that should close when something in the nag is clicked. */
    void setContainer(NagContainer dialog) {
        this.container = dialog;
    }
    
    /** Returns the title of the pro nag. */
    String getTitle() {
        Document document = editorPane.getDocument();
        if(document != null) {
            Object title = document.getProperty(Document.TitleProperty);
            if(title instanceof String) {
                return (String)title;
            }
        }
        return null;
    }

    /** Returns true if the dialog wants to be rendered without decoration. */
    boolean isUndecorated() {
        Document document = editorPane.getDocument();
        if(document instanceof HTMLDocument) {
            HTMLDocument html = (HTMLDocument)document;
            Element element = html.getElement(html.getDefaultRootElement(), "decorated", "false");
            if(element != null && element.getName().equals("body")) {
                return true;
            }
        }
        return false;
    }    
    
    public ListeningFuture<LoadResult> loadContents(boolean firstLaunch) {
        String bgColor = ColorUtil.toHexString(GuiUtils.getMainFrame().getBackground());
        String url = application.addClientInfoToUrl(
                "http://client-data.limewire.com/client_startup/modal_nag/?html32=true&fromFirstRun=" + firstLaunch + "&bgcolor=" + EncodingUtils.encode(bgColor));
        String backupUrl = createDefaultPage(firstLaunch, bgColor);
        
        ListeningFuture<LoadResult> future = editorPane.setPageAsynchronous(url, backupUrl);
        
        // add a listener to calculate when the offline was shown (if it was shown)
        future.addFutureListener(new EventListener<FutureEvent<LoadResult>>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FutureEvent<LoadResult> event) {
                // If we used the backup text, set the 'shown at' time.
                if(event.getResult() == LoadResult.OFFLINE_PAGE) {
                    offlineShownAt = System.currentTimeMillis();
                }
            };
        });
        
        return future;        
    }
    
    private String createDefaultPage(boolean firstLaunch, String bgColor) {
        // Pro has no default page.
        if(application.isProVersion()) {
            return "";
        }
        
        URL bgImage =  ProNag.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/update_background.png");
        
        String outgoing = "http://www.limewire.com/download/pro/?rnv=z&fromFirstRun=" + firstLaunch;
        outgoing = application.addClientInfoToUrl(outgoing); // add common LW info
        
        String yes = (outgoing + "&ref=lwn8").replace("&", "&amp;");
        String why = (outgoing + "&ref=lwn9").replace("&", "&amp;");
        
        return 
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">"
            + "<html>"
            + "<head><title>" + I18n.tr("Upgrade to Pro!") + "</title></head>"
            + "<body>"
            + "<center>"
                + "<table cellspacing=\"0\" cellpadding=\"8\" border=\"0\" bgcolor=\"" + bgColor + "\">"
                + "<tr><td align=\"center\">"
                    + "<table width=\"355\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" background=\"" + bgImage.toExternalForm() + "\">"
                    + "<tr><td height=\"126\">"
                        + "<table cellpadding=\"0\" cellspacing\"0\" border=\"0\">"
                        + "<tr>" 
                        + "<td>"
                        + "<table width=\"110\" cellpadding=\"0\" cellspacing\"0\" border=\"0\"></table>"
                        + "</td>"
                        + "<td align=\"left\" valign=\"center\">"
                        + "<b>"   + I18n.tr("Upgrade to PRO today!") + "</b>"
                        + "<br/>" + I18n.tr("Turbo-charged downloads")
                        + "<br/>" + I18n.tr("More search results")
                        + "<br/>" + I18n.tr("Free tech support and upgrades")
                        + "</td>"
                        + "</tr>"
                        + "</table>"
                    + "</td></tr>"
                    + "</table>"
               + "</tr></td>"
                + "<tr><td align=\"center\">"
                    + I18n.tr("Upgrade to LimeWire PRO?") + "<br/>"
                    + "<table cellspacing=\"3\" cellpadding=\"0\" border=\"0\"><tr>"                    
                    + "<td><form action=\"" + yes + "\"><input type=\"submit\" value=\"" + I18n.tr("Yes") + "\"/></form></td>"
                    + "<td><form action=\"" + why + "\"><input type=\"submit\" value=\"" + I18n.tr("Why") + "\"/></form></td>"
                    + "<td><form action=\"_hide_nag_\"><input type=\"submit\" value=\"" + I18n.tr("Later") + "\"/></form></td>"
                    + "</tr></table>"
                + "</td>"
                + "</tr>"
                + "</table>"
            + "</center>"
            + "</body>"
            + "</html>";
    }
    
    static interface NagContainer {
        /** Notifies the nag container that it should be set invisible & disposed. */
        void dispose();
    }

}
