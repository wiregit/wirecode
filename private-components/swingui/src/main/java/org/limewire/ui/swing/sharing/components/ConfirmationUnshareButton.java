package org.limewire.ui.swing.sharing.components;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.util.GuiUtils;

public class ConfirmationUnshareButton extends JPanel {

    @Resource
    Icon removeIcon;
    
    private JButton xButton;
    private JLabel divider;
    private HyperLinkButton yesButton;
    private HyperLinkButton noButton;
    
    
    public ConfirmationUnshareButton(Action confirmAction) {
        
        GuiUtils.assignResources(this); 
        
        yesButton = new HyperLinkButton("Yes");
        yesButton.setAction(confirmAction);
        yesButton.setForegroundColor(Color.BLUE);
        yesButton.setMouseOverColor(Color.BLUE);
        divider = new JLabel("/");
        noButton = new HyperLinkButton("No");
        noButton.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                setConfirmation(false);
                xButton.setEnabled(true);
            }           
        });
        noButton.setForegroundColor(Color.BLUE);
        noButton.setMouseOverColor(Color.BLUE);
        xButton = new JButton(removeIcon);
        xButton.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                setConfirmation(true);
                xButton.setEnabled(false);
            }
        });
        
        setConfirmation(false);
        setBorder(BorderFactory.createEmptyBorder());
        
        setLayout(new MigLayout("insets 0 5 0 0, hidemode 3", "", ""));
        
        add(yesButton, "gapbefore 15");
        add(divider);
        add(noButton);
        add(xButton);
    }
    
    private void setConfirmation(boolean value) {
        yesButton.setVisible(value);
        divider.setVisible(value);
        noButton.setVisible(value);
    }
    
    @Override
    public void setEnabled(boolean value) {
        if(value == false) 
            setConfirmation(value);
        xButton.setEnabled(value);
    }
}
