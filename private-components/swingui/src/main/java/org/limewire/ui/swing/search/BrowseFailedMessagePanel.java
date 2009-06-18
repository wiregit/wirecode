package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.core.settings.FriendBrowseSettings;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.components.MessageComponent.MessageBackground;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Replaces the search results when a browse fails.
 */
public class BrowseFailedMessagePanel extends JPanel {

    @Resource private Color backgroundColor;

    private final SearchResultsModel searchResultsModel;

    private BrowseSearch browseSearch;
    
    private boolean isInitialized = false;

    private BrowseState state;

    private List<Friend> friends;

    public BrowseFailedMessagePanel(SearchResultsModel searchResultsModel) {
        GuiUtils.assignResources(this);
        this.searchResultsModel = searchResultsModel;
    }
    
    public void update(BrowseState state, BrowseSearch browseSearch, List<Friend> friends){
        this.state = state;
        this.browseSearch = browseSearch;
        this.friends = friends;
        if(!isInitialized){
            isInitialized = true;
            initialize();
        }
        updateLabel();
    }

    private void initialize() {
        setBackground(backgroundColor);
        setLayout(new MigLayout("insets 0, gap 0, fill"));
    }
    
    private void updateLabel(){
        removeAll();
        add(createMessageComponent(getLabelText()), "pos 0.50al 0.4al");
        add(createBottomComponent(), "pos 1al 1al");
    }

    /**
     * Floating message in the FriendLibrary. This displays feedback to the user
     * as to what state their friend is in when no files are displayed.
     */
    private JComponent createMessageComponent(String text) {
        MessageComponent messageComponent = new MessageComponent(MessageBackground.GRAY);

        JLabel message = new JLabel(text);
        messageComponent.decorateHeaderLabel(message);
        messageComponent.addComponent(message, "wrap");
        
        if (state == BrowseState.NO_FRIENDS_SHARING){
            JLabel subMessage = new JLabel(I18n.tr("When they sign on LimeWire and share with you, their files will appear here."));
            messageComponent.decorateSubLabel(subMessage);            
            messageComponent.addComponent(subMessage, "");
        }
        

        if (state != BrowseState.OFFLINE && state != BrowseState.NO_FRIENDS_SHARING) {
            HyperlinkButton refresh = new HyperlinkButton(I18n.tr("Retry"));
            refresh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BrowseFailedMessagePanel.this.setVisible(false);
                    new SearchRepeater(browseSearch, searchResultsModel).refresh();
                }
            });
            messageComponent.decorateHeaderLink(refresh);
            messageComponent.addComponent(refresh, "gapleft 5");
        }
        
        return messageComponent;
    }
    private JComponent createBottomComponent(){
        if(state == BrowseState.NO_FRIENDS_SHARING){
            return new JLabel("Chat and tell them to sign on");
        }
        return new JLabel();
    }


    private String getLabelText() {
        if (state == BrowseState.NO_FRIENDS_SHARING) {
            if (FriendBrowseSettings.HAS_BROWSED_ALL_FRIENDS.get()) {
                return I18n.tr("No friends are on LimeWire");
            } else {
                return I18n.tr("No friends are sharing with you");
            }
        } else if (state == BrowseState.OFFLINE){
            if (friends.size() == 1) {
                return I18n.tr("{0} signed off LimeWire.", friends.get(0).getRenderName());
            } else {
                return I18n.tr("These people signed off LimeWire.");
            }
            
        } else {
        if (friends.size() == 1) {
            return I18n.tr("There was a problem browsing {0}.", friends.get(0).getRenderName());
        } else {
            return I18n.tr("There was a problem viewing these people.");
        }
        }
    }

}
