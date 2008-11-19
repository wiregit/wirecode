package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.LimeHeadingLabel;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.sharing.menu.FriendSharingPopupHandler;
import org.limewire.ui.swing.util.FontUtils;
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
public class FriendSharingHeaderPanel extends JXPanel {
    
    @Resource
    private Icon downIcon;
    @Resource
    protected int height;
    @Resource
    private Color fontColor;
    @Resource 
    private int fontSize;
    
    private LimeHeadingLabel titleLabel;
    private JTextField filterBox;
    
    private JXButton shareButton;
    
    private FriendSharingPopupHandler popupHandler;
    
    @Inject
    public FriendSharingHeaderPanel(FriendSharingPopupHandler popupHandler) {               
        GuiUtils.assignResources(this);
        
        this.popupHandler = popupHandler;
        
        setBackgroundPainter(new SubpanelPainter());

        createComponents();
        layoutComponents();       
        
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
    }
    
    private void createComponents() {     
        titleLabel = new LimeHeadingLabel(I18n.tr("Sharing with"));
        titleLabel.setForeground(fontColor);
        FontUtils.setSize(titleLabel, fontSize);
        FontUtils.changeStyle(titleLabel, Font.PLAIN);
        filterBox = new PromptTextField();
        
        shareButton = new JXButton(" ", downIcon);       
        shareButton.setForeground(fontColor);
        shareButton.setHorizontalTextPosition(SwingConstants.LEFT);
        shareButton.setBackgroundPainter(new ButtonPainter());
        shareButton.addActionListener(new PopupActionListener());
    }
    
    public JTextField getFilterBox() {
        return filterBox;
    }
    
    protected void layoutComponents() {
        setLayout(new MigLayout("insets 0 0 0 0", "", "align 50%"));

        add(titleLabel, "gapx 10");
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
