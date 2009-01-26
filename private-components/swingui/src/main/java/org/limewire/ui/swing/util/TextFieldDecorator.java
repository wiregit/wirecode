package org.limewire.ui.swing.util;

import java.awt.Paint;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.limewire.ui.swing.components.LimePromptPasswordField;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.painter.TextFieldPainterFactory;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TextFieldDecorator {
    
    private final TextFieldPainterFactory painterFactory;
    
    @Inject
    TextFieldDecorator(TextFieldPainterFactory painterFactory) {
        this.painterFactory = painterFactory;
    }
    
    /**
     * Decorates the specified text field using a background painter that 
     * includes an X icon to clear the field.
     */
    public void decorateClearablePromptField(LimePromptTextField field, AccentType accent) {
        field.setBackgroundPainter(painterFactory.createClearableBackgroundPainter(field, accent));
        // Get installed border, and restore it at the end.  This has a larger
        // right margin to prevent text from running into the reset icon.
        Border border = field.getBorder();
        decorateGeneralText(field);
        field.setBorder(border);
    }
    
    public void decoratePromptField(LimePromptTextField field, AccentType accent, Paint border) {
        field.setBackgroundPainter(painterFactory.createBasicBackgroundPainter(accent, border));
        decorateGeneralText(field);
    }
    
    public void decoratePromptField(LimePromptTextField field, AccentType accent) {
        field.setBackgroundPainter(painterFactory.createBasicBackgroundPainter(accent));
        decorateGeneralText(field);
    }
   
    public void decoratePromptField(LimePromptPasswordField field, AccentType accent, Paint border) {
        field.setBackgroundPainter(painterFactory.createBasicBackgroundPainter(accent, border));
        decorateGeneralPassword(field);
    }
    
    public void decoratePromptField(LimePromptPasswordField field, AccentType accent) {
        field.setBackgroundPainter(painterFactory.createBasicBackgroundPainter(accent));
        decorateGeneralPassword(field);
    }
    
    private void decorateGeneralText(LimePromptTextField field) {
        field.setPromptPainter(painterFactory.createBasicPromptPainter());
        decorateGeneral(field);
    }
    
    private void decorateGeneralPassword(LimePromptPasswordField field) {
        field.setPromptPainter(painterFactory.createBasicPromptPainter());
        decorateGeneral(field);
    }
    
    private void decorateGeneral(JTextField field) {
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(2,10,2,12));
    }

}
