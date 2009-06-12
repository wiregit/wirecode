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
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.components.MessageComponent.MessageBackground;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.browse.BrowseSearch;
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

    public BrowseFailedMessagePanel(SearchResultsModel searchResultsModel) {
        GuiUtils.assignResources(this);
        this.searchResultsModel = searchResultsModel;
    }
    
    public void update(BrowseSearch browseSearch, List<Friend> friends){
        if(!isInitialized){
            isInitialized = true;
            initialize(friends);
            this.browseSearch = browseSearch;
        }
    }

    private void initialize(List<Friend> friends) {
        setBackground(backgroundColor);
        setLayout(new MigLayout("insets 0, gap 0, fill"));
        add(createMessageComponent(getLabelText(friends)), "pos 0.50al 0.4al");
    }

    /**
     * Floating message in the FriendLibrary. This displays feedback to the user
     * as to what state their friend is in when no files are displayed.
     */
    private JComponent createMessageComponent(String text) {
        MessageComponent messageComponent = new MessageComponent(MessageBackground.GRAY);

        JLabel message = new JLabel(text);
        messageComponent.decorateHeaderLabel(message);

        HyperlinkButton refresh = new HyperlinkButton(I18n.tr("Retry"));
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrowseFailedMessagePanel.this.setVisible(false);
                new BrowseSearchRefresher(browseSearch, searchResultsModel).refresh();
            }
        });
        messageComponent.decorateHeaderLink(refresh);

        messageComponent.addComponent(message, "");
        messageComponent.addComponent(refresh, "gapleft 5");
        
        return messageComponent;
    }


    /**
     * Friend signed onto LimeWire, browse failed.
     */
    private String getLabelText(List<Friend> friends) {
        if (friends.size() == 1) {
            return I18n.tr("There was a problem browsing {0}.", friends.get(0).getRenderName());
        } else {
            return I18n.tr("There was a problem viewing these people.");
        }
    }

}
