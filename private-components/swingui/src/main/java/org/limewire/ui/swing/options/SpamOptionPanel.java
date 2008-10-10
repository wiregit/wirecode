package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Spam Option View
 */
@Singleton
public class SpamOptionPanel extends OptionPanel {
    
    private SpamManager spamManager;
    
    private JButton clearSpamButton;
    
    @Inject
    public SpamOptionPanel(SpamManager spamManager) {   
        this.spamManager = spamManager;
        
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
                //TODO: spam manager needs a isEmpty method added
                spamManager.clearFilterData();
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
