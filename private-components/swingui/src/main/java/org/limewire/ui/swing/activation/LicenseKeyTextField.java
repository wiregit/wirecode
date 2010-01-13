package org.limewire.ui.swing.activation;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.components.Paintable;
import org.limewire.ui.swing.components.TextFieldClipboardControl;

import com.google.inject.Inject;

public class LicenseKeyTextField extends JTextField implements Paintable {

    private Painter<JTextField> backgroundPainter;
    private Insets paintedInsets;

    @Inject
    public LicenseKeyTextField() {
        super(15);
        
        TextFieldClipboardControl.install(this);
    }
    
    @Override
    protected Document createDefaultModel() {
        return new LicenseKeyDocument();
    }
    
    class LicenseKeyDocument extends PlainDocument {
        
        @Override
        public void insertString(int offs, String stringToInsert, AttributeSet a) 
            throws BadLocationException {

            if (stringToInsert == null) {
                return;
            }

            int stringLength = stringToInsert.length();
            // license keys are always upper case. if the user entered it as lower case, convert it to upper case.
            stringToInsert = stringToInsert.toUpperCase();
            StringBuffer modifiedStringToInsert = new StringBuffer();
            for (int counter = offs; counter < offs+stringLength; counter++)
            {
                // disallow more than 14 characters. that is the length of the key with the hyphens
                if ((offs+modifiedStringToInsert.length()) < 14) {
                    modifiedStringToInsert.append(stringToInsert.charAt(counter-offs));
                }

                // here we insert a hyphen into the license key if the user is at the 3rd or the 8th position
                if ((offs+modifiedStringToInsert.length()) == 4 || (offs+modifiedStringToInsert.length()) == 9) {
                    // if the user is copying and pasting the key, it might have hyphens already. check for that
                    // and only add the hyphens if they don't exist already
                    if (counter + 1 >= (offs+stringLength) || stringToInsert.charAt(counter-offs+1) != '-')
                        modifiedStringToInsert.append("-");
                }
            }
            
            super.insertString(offs, modifiedStringToInsert.toString(), a);
        }
    }

    
    /**
     * Returns the effective insets rendered by a custom painter.  This can be
     * used to determine the actual dimensions as drawn on the screen.
     */
    @Override
    public Insets getPaintedInsets() {
        return (backgroundPainter == null) ? new Insets(0, 0, 0, 0) : paintedInsets;
    }

    /**
     * Sets the background painter for this component.
     */
    public void setBackgroundPainter(Painter<JTextField> painter) {
        this.backgroundPainter = painter;
        
        // Set painted insets.  For now, we assume that a non-null painter 
        // visualizes the component with 1-pixel insets around the edges.
        if (painter != null) {
            paintedInsets = new Insets(1, 1, 1, 1);
        }
    }

    /**
     * Paints this component, including an icon and
     * the default text when this component has focus and has no text value.
     */
    @Override
    protected void paintComponent(Graphics g) {

        if (backgroundPainter != null) {
            this.backgroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
        }
            
        super.paintComponent(g);
    }

}
