package org.limewire.ui.swing.pro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Rectangle;

import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

/** A nag to go to LimeWire PRO. */ 
public class ProNag extends JXPanel implements Resizable {
    
    private final Application application;
    private final java.awt.Panel parent;
    private final HTMLPane editorPane;
    
    @Inject public ProNag(Application application) {
        super(new BorderLayout());
        this.application = application;
        this.parent = new Panel(new BorderLayout()); // heavyweight, to show over other things.
        this.editorPane = new HTMLPane();

        setOpaque(false);
        parent.setMinimumSize(new Dimension(350, 200));
        parent.setMaximumSize(new Dimension(350, 200));
        parent.setPreferredSize(new Dimension(350, 200));

        editorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // Look for the HREF attribute of the A tag...
                    Object a = e.getSourceElement().getAttributes().getAttribute(HTML.Tag.A);
                    Object href = "";
                    if(a instanceof AttributeSet) {
                        href = ((AttributeSet)a).getAttribute(HTML.Attribute.HREF);
                    }
                    if(href != null && href.equals("_hide_nag_")) {
                        ProNag.this.setVisible(false);
                    } else if(e.getURL() != null) {
                        NativeLaunchUtils.openURL(e.getURL().toExternalForm());
                    }
                }
            }            
        });   
        
        add(parent, BorderLayout.CENTER);
        
        JScrollPane scroller = new JScrollPane(editorPane, 
                                               JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                               JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        parent.add(scroller, BorderLayout.CENTER);
    }

    @Override
    public void resize() {
        if (isVisible()) {
            Rectangle parentBounds = getParent().getBounds();
            Dimension childPreferredSize = parent.getPreferredSize();
            int w = childPreferredSize.width;
            int h = childPreferredSize.height; 
            setBounds(parentBounds.width / 2 - w / 2, parentBounds.height - h, w, h);
        }
    }
    
    
    public void loadContents() {
        editorPane.setPageAsynchronous(application.getUniqueUrl("http://client-data.limewire.com/pronag?html32=true"), createDefaultPage());
    }
    
    private String createDefaultPage() {
        String outgoing = application.getUniqueUrl("http://www.limewire.com/clientpro?offline=true");
        return
         "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">"
         + "<html>" 
         + "<body>"
         + "<table width=100% border=0><tr><td>"
         + "<table width=100% border=0><tr><td align='left'><h1>Upgrade to PRO</h1></td><td align='right'><a href=\"_hide_nag_\">hide</a></td></tr></table>"
         + "&nbsp;<b>Do you want:</b>"
         + "<ul>"
         + "<li>Turbo-charged downloads?</li>"
         + "<li>More search results?</li>"
         + "<li>Free tech support and upgrades?</li>"
         + "</ul>"
         + "<center><a href=\"" + outgoing + "\">Get LimeWire PRO now</a></center>"
         + "</td></tr></table>"
         + "</body>"
         + "</html>"
         ;
    }

}
