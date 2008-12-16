package org.limewire.ui.swing.components;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Behaves like a TextField. Adds an icon within the textField that
 * is always displayed to the left of the textfield regardless of how much
 * space there is. 
 * 
 * An action can be added to the textfield that will be called on mouse clicks.
 * 
 * This textfield is currently assumed to be non-editable. Border color and background
 * assume this. If this is to be used in a non-editable fashion, more work will need
 * to be put into the L&F of this.
 */
public class LabelTextField extends JPanel {

    @Resource
    private Icon folderIcon;
    
    private JLabel label;
    private JTextField textField;
    
    private MouseListener mouseListener;
    
    public LabelTextField() {
        GuiUtils.assignResources(this);
        
        label = new JLabel(folderIcon);
        textField = new JTextField();
        textField.setEditable(false);
        
        setLayout(new MigLayout("insets 2, gap 3, fillx"));
        add(label);
        add(textField, "growx");
        
        setBackground(textField.getBackground());
        setBorder(textField.getBorder());
        textField.setBorder(BorderFactory.createEmptyBorder());
    }
    
    @Override
    public void setEnabled(boolean value) {
        textField.setEnabled(value);
    }
    
    public void setEditable(boolean value) {
        textField.setEditable(value);
    }
    
    public String getText() {
        return textField.getText();
    }
    
    public void setText(String text) {
        textField.setText(text);
    }
    
    public void addMouseListener(final Action action) {
        if(mouseListener != null) {
            textField.removeMouseListener(mouseListener);
            mouseListener = null;
        }
        mouseListener = new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    System.out.println("action");
                    action.actionPerformed(null);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
        };
        textField.addMouseListener(mouseListener);
    }
    
    public void removeMouseListener() {
        if(mouseListener != null) {
            textField.removeMouseListener(mouseListener);
            mouseListener = null;
        }
    }
}
