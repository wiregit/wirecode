package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

/**
 * Spam Option View
 */
public class SpamOptionPanel extends OptionPanel {
    
    private JButton clearSpamButton;
    
    public SpamOptionPanel() {       
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getClearPanel(), "pushx, growx");
    }
    
    private JPanel getClearPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        
        clearSpamButton = new JButton(I18n.tr("Clear Spam"));
        clearSpamButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
//                spamManager.clearFilterData();
            }
        });
        
        p.add(new JLabel(I18n.tr("Reset the Spam filter by clearing all files markes as spam")), "push");
        p.add(clearSpamButton);
        
        return p;
    }
    
    @Override
    void applyOptions() {
        // TODO Auto-generated method stub
        
    }

    @Override
    boolean hasChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    void initOptions() {
        
    }
}
