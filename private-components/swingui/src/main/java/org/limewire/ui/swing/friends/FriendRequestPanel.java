package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.friend.api.FriendRequest;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class FriendRequestPanel extends JXPanel {
    
    private final List<FriendRequest> pendingRequests;
     private final JLabel requestLabel;
    
    @Inject 
    public FriendRequestPanel(BarPainterFactory barPainterFactory) {
        setLayout(new MigLayout("gap 0, insets 0, fill", "10[]2", "2[]2[]2[]2"));
        setBackgroundPainter(barPainterFactory.createFriendsBarPainter());
        setOpaque(false);

        pendingRequests = new ArrayList<FriendRequest>();
        requestLabel = new JLabel();
        
        // workaround for LWC-2465 -- MultiLineLabel seems to require it.
        addComponentListener(new ComponentAdapter() {
           @Override
            public void componentShown(ComponentEvent e) {
               invalidate();
               validate();
            } 
        });
        
        JComponent yes = new HyperlinkButton(new AbstractAction(I18n.tr("Yes")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                completeRequest(true);
            }
        });
        JComponent no = new HyperlinkButton(new AbstractAction(I18n.tr("No")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                completeRequest(false);
            }
        });

        add(requestLabel, "growx, wrap");
        add(yes, "gapbefore push, split, alignx right");
        add(no, "alignx right");
        
        ensureRequestVisible();
    }

    public void addRequest(FriendRequest request) {
        pendingRequests.add(request);
        ensureRequestVisible();
    }
    
    private void ensureRequestVisible() {
        if(pendingRequests.size() > 0) {
            requestLabel.setText(I18n.tr("<html><b>{0}</b> wants to be your friend.  Do you accept?</html>", pendingRequests.get(0).getFriendUsername()));
            setVisible(true);
            
        } else {
            setVisible(false);
        }
    }
    
    private void completeRequest(final boolean accept) {
        final FriendRequest request = pendingRequests.remove(0);
        BackgroundExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                request.getDecisionHandler().handleDecision(request.getFriendUsername(), accept);
            }
        });
        ensureRequestVisible();
    }

}
