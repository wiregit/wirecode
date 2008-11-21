package org.limewire.ui.swing.friends.login;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;


class LoggedInPanel extends JXPanel {
    
    private final LimeComboBox optionsBox;
    private final LimeComboBox signoutBox;
    
    @Inject
    LoggedInPanel(LimeComboBoxFactory comboFactory) {
        setLayout(new MigLayout("insets 0, gap 0"));
        
        optionsBox = comboFactory.createMiniComboBox();
        signoutBox = comboFactory.createMiniComboBox();

        JPopupMenu optionsMenu = new JPopupMenu(); 
        optionsMenu.add(new AbstractAction(I18n.tr("Add Friend")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
        optionsMenu.addSeparator();
        optionsMenu.add(new JLabel(I18n.tr("Show:")));
        optionsMenu.add(new JCheckBoxMenuItem(new AbstractAction(I18n.tr("Offline friends")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                            
            }
        }));
        optionsMenu.addSeparator();
        optionsMenu.add(new JLabel(I18n.tr("Set {0} status", "<service>")));
        JCheckBoxMenuItem available = new JCheckBoxMenuItem(new AbstractAction(I18n.tr("Available")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        JCheckBoxMenuItem dnd = new JCheckBoxMenuItem(new AbstractAction(I18n.tr("Do Not Disturb")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        ButtonGroup group = new ButtonGroup();
        group.add(available);
        group.add(dnd);
        optionsMenu.add(available);
        optionsMenu.add(dnd);
        optionsBox.overrideMenu(optionsMenu);
        optionsBox.setText(I18n.tr("Options"));
        
        signoutBox.setText(I18n.tr("Sign out"));
        signoutBox.addAction(new AbstractAction(I18n.tr("Switch user")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
        signoutBox.addAction(new AbstractAction(I18n.tr("Sign out")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
        
        add(new JLabel("O currentuser@host.com"), "gapleft 9, gaptop 2, wmin 0, wrap");
        add(optionsBox, "gapleft 2, alignx left, gapbottom 2, split");
        add(signoutBox, "alignx right, gapbottom 2");
        
        setBackgroundPainter(new RectanglePainter<JXPanel>(2, 2, 2, 2, 5, 5, true, Color.LIGHT_GRAY, 0f, Color.LIGHT_GRAY));
    }

}
