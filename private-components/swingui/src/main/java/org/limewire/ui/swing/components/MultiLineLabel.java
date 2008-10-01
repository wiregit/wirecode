package org.limewire.ui.swing.components;

import org.jdesktop.swingx.JXLabel;

public class MultiLineLabel extends JXLabel {

    public MultiLineLabel(String text, int lineWidth) {
        super(text);
        setMaxLineSpan(lineWidth);
        setLineWrap(true);
    }
    
    

}
