package org.limewire.ui.swing.activation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.limewire.ui.swing.util.NativeLaunchUtils;

public class LabelWithLinkSupport extends JEditorPane {
    // the &lang part of the url is being mangled by java thereby breaking the link.
    // so we encode those before the text is given over to the editor pane.
    private static final Pattern ampPattern = Pattern.compile("&lang");

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
    
    @Override
    public void setText(String text) {
        Matcher matcher = ampPattern.matcher(text);
        text = matcher.replaceAll("&amp;lang");
        super.setText(text);
    }
    
}
