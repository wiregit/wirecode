package org.limewire.ui.swing.activation;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.limewire.ui.swing.util.NativeLaunchUtils;

public class LabelWithLinkSupport extends JEditorPane {
    
    public LabelWithLinkSupport() {
        setContentType("text/html");
        setEditable(false);
        setOpaque(false);
        addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    NativeLaunchUtils.openURL(e.getURL().toExternalForm());
                }
            }
        });
    }
    
}
