package org.limewire.ui.swing.pro;

import java.awt.BorderLayout;
import java.net.URL;
import java.util.Date;

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
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.Application;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.activation.ActivationPanel;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.components.HTMLPane.LoadResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.EncodingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/** A nag to go to LimeWire PRO. */ 
class ProNag extends JXPanel {
    
    private NagContainer container;
    private final Application application;
    private final ActivationManager activationManager;
    private final HTMLPane editorPane;
    
    private long offlineShownAt = -1;
    
    @Inject public ProNag(Application application, ActivationManager activationManager,
                          final GnutellaConnectionManager connectionManager,
                          final Provider<ActivationPanel> activationPanelProvider) {
        super(new BorderLayout());
        
        this.application = application;        
        this.activationManager = activationManager;
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
                    } else if (href != null && href.equals("_edit_license_")) {
                        container.dispose();
                        ActivationPanel activationPanel = activationPanelProvider.get();
                        activationPanel.show();
                    } else if(e.getURL() != null) {
                        String url = e.getURL().toExternalForm();
                        // if the URL has a question mark appended onto the end of it, but it already has another question mark preceding it
                        // in the URL, then chop off the last question mark, b/c it breaks the URL.
                        if (url.endsWith("?") && url.indexOf('?') != (url.length()-1)) {
                            url = url.substring(0, url.length()-1);
                        }
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
    
    /** Returns the delay, in milliseconds, that are required before showing the nag. */
    int getDelay() {
        Document document = editorPane.getDocument();
        if(document instanceof HTMLDocument) {
            HTMLDocument html = (HTMLDocument)document;
            Element element = getElement(html.getDefaultRootElement(), HTML.Tag.BODY);
            if(element != null) {
                Object delay = element.getAttributes().getAttribute("delay");
                if(delay != null) {
                    try {
                        int d = Integer.valueOf(delay.toString());
                        if(d >= 0) {
                            return d;
                        }
                    } catch(NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }
    
    /** Returns the first element in a document that has the given tag. */
    private static Element getElement(Element e, HTML.Tag tag) {
        if(e.getName() != null && e.getName().equals(tag.toString())) {
            return e;
        }
        
        for (int counter = 0, maxCounter = e.getElementCount(); counter < maxCounter; counter++) {
            Element retValue = getElement(e.getElement(counter), tag);
            if (retValue != null) {
                return retValue;
            }
        }
        
        return null;
    }
    
    public ListeningFuture<LoadResult> loadContents(boolean firstLaunch) {
        String bgColor = ColorUtil.toHexString(GuiUtils.getMainFrame().getBackground());
        String url = application.addClientInfoToUrl(
                "http://client-data.limewire.com/client_startup/modal_nag/?html32=true&fromFirstRun=" + firstLaunch 
                + "&bgcolor=" + EncodingUtils.encode(bgColor));
        String backupUrl = createBackupPage(firstLaunch, bgColor);
        
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
    
    /*
     * This returns an empty string if LimeWire is pro (unless your features are expiring soon 
     * in which case it warns you)
     * This returns a blocked message if the users license is blocked.
     * And it returns a pro nag if LimeWire is basic.
     */
    private String createBackupPage(boolean firstLaunch, String bgColor) {
        // Pro has no default page.
        if(activationManager.isProActive()) {
            if (!areModulesExpiringSoon()) {
                return "";
            } else {
                // but we show a features expiring notification if your features are expiring in 3 days
                return createExpiredPage(firstLaunch, bgColor);
            }
        } else if (activationManager.getActivationError() == ActivationError.BLOCKED_KEY) {
            return createBlockedPage(firstLaunch, bgColor);
        } else {
            return createDefaultPage(firstLaunch, bgColor);
        }
    }
    
    /*
     * This checks to see if any features are expiring in three days.
     */
    private boolean areModulesExpiringSoon() {
        Date today = new Date();
        double MILLSECS_PER_DAY = 1000 * 60 * 60 * 24;
        for (ActivationItem item : activationManager.getActivationItems()) {
            Date expirationDate = item.getDateExpired();
            // this comparison isn't 100% correct b/c it neglects to account for daylight savings time
            double deltaDays = ( ((double) expirationDate.getTime()) - today.getTime() ) / MILLSECS_PER_DAY;
            if (deltaDays < 3) {
                return true;
            }
        }
        
        return false;
    }
    
    private String createDefaultPage(boolean firstLaunch, String bgColor) {
        URL bgImage =  ProNag.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/update_background.png");
        
        String outgoing = "http://www.limewire.com/download/pro/?rmnv=z&fromFirstRun=" + firstLaunch;
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
                        + "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
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

    private String createBlockedPage(boolean firstLaunch, String bgColor) {

        String customerSupportURL = application.addClientInfoToUrl(ActivationSettingsController.CUSTOMER_SUPPORT_URL).replace("&", "&amp;");
        
        return 
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">"
            + "<html>"
            + "<head><title>LimeWire " + I18n.tr("License") + "</title></head>"
            + "<body   >"
            + "<center>"
            + "<table width=\"330\" height=\"125\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" bgcolor=\"" + bgColor + "\">"
              + "<tr>"
              + "<td width=\"15\"></td>"
              + "<td colspan=2 >"
                + "<table width=\"300\" height=\"90\" cellspacing=\"5\" cellpadding=\"0\" border=\"0\" bgcolor=\"" + bgColor + "\">"
                  + "<tr><td valign=bottom height=\"25\">"
                    + "<font size=4 color=\"#59762d\"><b>" + I18n.tr("Sorry, your license key has been blocked") + "</b></font>"
                  + "</td></tr>"
                  + "<tr ><td height=\"50\">"
                        + "<font size=4>" + I18n.tr("Your license key has been used on too many installations. ")
                        + I18n.tr("Please {0}contact support{1} to resolve the situation.", "<a href=\"" + customerSupportURL + "\">", "</a>") + "</font>"
                  + "</td></tr>"
                + "</table>"
              + "</td>"
              + "<td width=\"15\"></td>"
              + "</tr>"
              + "<tr valign=top>"
                + "<td width=\"15\"></td>"
                + "<td align=left height=\"40\"><form action=\"_edit_license_\"><input type=\"submit\" name=\"edit_key\" value=\"" +  I18n.tr("Edit License Key") + "\" id=\"\"></form></td>" 
                + "<td align=right><form action=\"_hide_nag_\"><input type=\"submit\" value=\"" + I18n.tr("Later") + "\"/></form></td>"
                + "<td width=\"15\"></td>"
              + "</tr>"
            + "</table>"
            + "</center>"
            + "</body>";
    }

    private String createExpiredPage(boolean firstLaunch, String bgColor) {

        String renewURL = application.addClientInfoToUrl(ActivationSettingsController.RENEW_URL).replace("&", "&amp;");
        
        return 
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">"
            + "<html>"
            + "<head><title>" + I18n.tr("Time to Renew Your LimeWire") + "</title></head>"
            + "<body   >"
            + "<center>"
            + "<table width=\"400\" height=\"115\" cellspacing=\"5\" cellpadding=\"0\" border=\"0\" bgcolor=\"" + bgColor + "\">"
              + "<tr>"
                + "<td width=\"15\"></td>"
                + "<td colspan=2 height=\"30\" valign=bottom><font size=4 color=\"#59762d\"><b>" + I18n.tr("Your LimeWire subscription will be expiring shortly.") + "</b></font></td>"
                + "<td width=\"15\"></td>"
              + "</tr>"
              + "<tr>"
                + "<td width=\"15\"></td>"
                + "<td colspan=2 height=\"30\" valign=top><font size=4 color=\"#59762d\"><b>" + I18n.tr("Don't miss a thing - act now for special renewal pricing!") + "</b></font></td>"
                + "<td width=\"15\"></td>"
            + "</tr>"
              + "<tr valign=top>"
                + "<td width=\"15\"></td>"
                + "<td align=right height=\"40\" width=\"290\"><form action=\"" + renewURL + "\" method=\"GET\" target=\"_blank\">"
                                        + "<input type=\"submit\" value=\"" + I18n.tr("Renew Now") + "\"/>"
                        + "</form></td> "
                + "<td align=right><form action=\"_hide_nag_\"><input type=\"submit\" value=\"" + I18n.tr("Later") + "\"/></form></td>"
                + "<td width=\"15\"></td>"
              + "</tr>"
            + "</table>"
            + "</center>"
            + "</body>";
    }

    static interface NagContainer {
        /** Notifies the nag container that it should be set invisible & disposed. */
        void dispose();
    }

}
