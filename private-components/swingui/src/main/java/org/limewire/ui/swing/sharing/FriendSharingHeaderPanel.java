package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.painter.DarkButtonPainter;
import org.limewire.ui.swing.sharing.menu.FriendSharingPopupHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Header for the Friend Sharing Panel. Displays the friend's name, 
 * a filter box, a library button and a sharing button.
 * 
 * The library button is enabled and directs you to their library
 * if they are signed on through LW. 
 * 
 * The Sharing button displays a list of sharing actions to share
 * file types with them.
 */
@Singleton
public class FriendSharingHeaderPanel extends LimeHeaderBar {
    
    @Resource
    private Icon downIcon;
    
    private JTextField filterBox;
    
    private JXButton shareButton;
    
    private FriendSharingPopupHandler popupHandler;
    
    @Inject
    public FriendSharingHeaderPanel(FriendSharingPopupHandler popupHandler, 
            LimeHeaderBarFactory headerBarFactory) {
        
        super(I18n.tr("Sharing with"));
        
        GuiUtils.assignResources(this);
        
        headerBarFactory.decorateBasic(this);
        
        this.popupHandler = popupHandler;
        
        createComponents();
        layoutComponents();  
    }
    
    private void createComponents() {     
        filterBox = new PromptTextField();
        
        shareButton = new JXButton(" ", downIcon);       
        shareButton.setForeground(Color.WHITE);
        shareButton.setHorizontalTextPosition(SwingConstants.LEFT);
        shareButton.setBackgroundPainter(new DarkButtonPainter());
        shareButton.addActionListener(new PopupActionListener());
    }
    
    public JTextField getFilterBox() {
        return filterBox;
    }
    
    protected void layoutComponents() {
        setLayout(new MigLayout("insets 0 0 0 0", "", "align 50%"));

        add(shareButton,"push");
        
        add(filterBox, "gapafter 10");
    }

    public void setFriendName(Friend friend) {
        shareButton.setText(friend.getRenderName());
    }
    
    private class PopupActionListener implements ActionListener {        
        @Override
        public void actionPerformed(ActionEvent e) {
            popupHandler.showPopup(shareButton, 0, shareButton.getHeight());
        }
    }
}
