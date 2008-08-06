package org.limewire.ui.swing.components;

import java.awt.Dimension;

import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import org.jdesktop.swingx.JXLabel;

public class MultiLineLabel extends JXLabel {

    public MultiLineLabel(String text, int lineWidth) {
        super(text);
        setMaxLineSpan(lineWidth);
        setLineWrap(true);
        Object viewObj = getClientProperty(BasicHTML.propertyKey);
        // JXLabel, by default, doesn't size the width or height according
        // to the max line span.  The internal View does, though.
        // So we want to take the dimension from the view & set that
        // as our preferred size.
        if(viewObj instanceof View) {
            View view = (View)viewObj;
            int width = (int)view.getPreferredSpan(View.X_AXIS);
            int height = (int)view.getPreferredSpan(View.Y_AXIS);
            setPreferredSize(new Dimension(width, height));
        }
    }
    
    

}
