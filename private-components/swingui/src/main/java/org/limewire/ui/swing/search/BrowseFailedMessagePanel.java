package org.limewire.ui.swing.search;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.MessageDecorator;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Replaces the search results when a browse fails.  Must be disposed.
 */
public class BrowseFailedMessagePanel extends JPanel {

    private final SearchResultsModel searchResultsModel;
    private final Provider<MessageDecorator> messageDecoratorProvider;
    private final HeaderBarDecorator headerBarDecorator;
    
    private BrowseSearch browseSearch;
    
    private boolean isInitialized = false;

    private List<Friend> friends;

    @Inject
    public BrowseFailedMessagePanel(
            Provider<MessageDecorator> messageDecoratorProvider, HeaderBarDecorator headerBarDecorator,
            @Assisted SearchResultsModel searchResultsModel) {
        GuiUtils.assignResources(this);
        this.searchResultsModel = searchResultsModel;
        this.messageDecoratorProvider = messageDecoratorProvider;
        this.headerBarDecorator = headerBarDecorator;
    }
    
    public void update(BrowseState state, BrowseSearch browseSearch, List<Friend> friends){
        this.browseSearch = browseSearch;
        this.friends = friends;
        if(!isInitialized){
            isInitialized = true;
            initialize();
        }
        updateLabel();
    }
    
    private void initialize() {
        setLayout(new MigLayout("insets 0, gap 0, fill", "[]", "[][grow][grow]"));
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
        
        HeaderBar header = new HeaderBar(new JLabel(""));
        header.setLayout(new MigLayout("insets 0, gap 0!, novisualpadding, alignx 100%, aligny 100%"));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerBarDecorator.decorateBasic(header);
        add(header, "growx, growy, wrap");       
        
        MessageComponent messageComponent = new MessageComponent();
        messageDecoratorProvider.get().decorateGrayMessage(messageComponent);

        JLabel message = new JLabel(text);
        messageComponent.decorateHeaderLabel(message);
        messageComponent.addComponent(message, "");
       
        HyperlinkButton refresh = new HyperlinkButton(I18n.tr("Retry"));
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrowseFailedMessagePanel.this.setVisible(false);
                new DefaultSearchRepeater(browseSearch, searchResultsModel).refresh();
            }
        });
        messageComponent.decorateHeaderLink(refresh);
        messageComponent.addComponent(refresh, "gapleft 5, wrap");
        
        return messageComponent;
    }
    
    private JComponent createBottomComponent(){
        return new JLabel();
    }

    private String getLabelText() {
        if (isSingleBrowse()) {
            return I18n.tr("There was a problem browsing {0}.", getSingleFriendName());
        } else {
            return I18n.tr("There was a problem viewing these people.");
        }
    }

    private boolean isSingleBrowse(){
        return friends.size() == 1;
    }   
    
    private String getSingleFriendName(){
        return friends.get(0).getRenderName();
    }
}
