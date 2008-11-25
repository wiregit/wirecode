package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Creates as little tables as possible. Expected to be used with a 
 * JXLayer disabling the main component
 */
public class EmptyLibraryPanel extends LibraryPanel {

    private FriendLibraryMediator mediator;
    
    @AssistedInject
    public EmptyLibraryPanel(@Assisted Friend friend,  
            @Assisted FriendLibraryMediator mediator, 
            @Assisted JComponent messageComponent,
            LimeHeaderBarFactory headerBarFactory) {
        super(friend, true, headerBarFactory);

        this.mediator = mediator;

        loadHeader();
        loadSelectionPanel();

        createEmptyPanel(messageComponent);
    }

    @Override
    public void loadHeader() {
        headerPanel.enableButton(new ViewSharedLibraryAction());
    }
    
    private void createEmptyPanel(JComponent component) {
        JPanel p = new JPanel(new MigLayout("insets 0, gap 0, fill"));
        p.setBackground(new Color(100,100,100,160));
        p.add(component, "alignx 50%, aligny 50%");
       
        add(p, "grow, span");
    }
    
    @Override
    protected void addMainPanels() {
    }

    @Override
    public void loadSelectionPanel() {
    }

    private class ViewSharedLibraryAction extends AbstractAction {

        public ViewSharedLibraryAction() {
            putValue(Action.NAME, I18n.tr("Share with {0}", friend.getRenderName()));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Show files you're sharing with {0}", friend.getRenderName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            mediator.showSharingCard();
        }
    }
}
