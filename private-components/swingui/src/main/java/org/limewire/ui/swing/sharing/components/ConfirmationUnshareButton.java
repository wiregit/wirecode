package org.limewire.ui.swing.sharing.components;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Panel that displays a remove button. When the remove button is pressed, a confirmation
 * yes/no hyperlink/button is displayed to confirm the remove action.
 */
public class ConfirmationUnshareButton extends JPanel {

    @Resource
    Color foregroundColor;
    @Resource
    Color mouseOverColor;
    
    private UnshareButton xButton;
    private JLabel divider;
    private HyperLinkButton yesButton;
    private HyperLinkButton noButton;
    
    private AWTEventListener eventListener;
    
    public ConfirmationUnshareButton(Action confirmAction) {
        
        GuiUtils.assignResources(this); 
        
        //listens for clicks anywhere in the application. If clicked outside of the 
        //component assumes its a "No" operation
        eventListener = new AWTEventListener(){
            @Override
            public void eventDispatched(AWTEvent event) {
                if ( event.getID() == MouseEvent.MOUSE_PRESSED){
                    MouseEvent e = (MouseEvent)event;
                    if(!ConfirmationUnshareButton.this.contains(e.getPoint())) {
                        setConfirmationVisible(false);
                        xButton.setEnabled(true);
                    }
                    Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
                }
            }
        }; 
        
        yesButton = new HyperLinkButton(I18n.tr("Yes"));
        yesButton.setAction(confirmAction);
        yesButton.setForegroundColor(foregroundColor);
        yesButton.setMouseOverColor(mouseOverColor);
        divider = new JLabel("/");
        noButton = new HyperLinkButton(I18n.tr("No"));
        noButton.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                setConfirmationVisible(false);
                xButton.setEnabled(true);
            }           
        });
        noButton.setForegroundColor(foregroundColor);
        noButton.setMouseOverColor(mouseOverColor);
        xButton = new UnshareButton();
        xButton.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK); 
                setConfirmationVisible(true);
                xButton.setEnabled(false);
            }
        });
        
        setConfirmationVisible(false);
        setBorder(BorderFactory.createEmptyBorder());
        
        setLayout(new MigLayout("insets 0 5 0 0, hidemode 3", "", ""));
        
        add(yesButton, "gapbefore 15");
        add(divider);
        add(noButton);
        add(xButton);
    }
    
    private void setConfirmationVisible(boolean value) {
        yesButton.setVisible(value);
        divider.setVisible(value);
        noButton.setVisible(value);
    }
    
    @Override
    public void setEnabled(boolean value) {
        if(value == false) 
            setConfirmationVisible(value);
        xButton.setEnabled(value);
    }
}
