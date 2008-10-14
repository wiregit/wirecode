package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.sharing.actions.SharingAddAllAction;
import org.limewire.ui.swing.sharing.friends.FriendUpdate;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SharingFriendEmptyPanel extends JPanel implements FriendUpdate {

    @Resource
    Icon friendIcon;
    
    private JLabel titleLabel;
    private JLabel text;
    
    private SharingCheckBox audioCheckBox;
    private SharingCheckBox videoCheckBox;
    private SharingCheckBox imageCheckBox;
    
    private JButton shareButton;
    
    private SharingAddAllAction addAllAction;
    
    private static final String TITLE_TEMPLATE = I18nMarker.marktr("You are not sharing anything with {0}");
    private static final String BUTTON_TEXT_TEMPLATE = I18nMarker.marktr("Shared with {0}");
    private static final String TEXT_TEMPLATE = "To share with {0}, drag files here, or use the shortcuts below to share files.";
    
    @Inject
    public SharingFriendEmptyPanel(LibraryManager libraryManager) {
        
        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
        
        titleLabel = new JLabel(friendIcon);
        
        text = new JLabel();

        audioCheckBox = new SharingCheckBox(I18n.tr("All my music"));
        videoCheckBox = new SharingCheckBox(I18n.tr("All my videos"));
        imageCheckBox = new SharingCheckBox(I18n.tr("All my images"));
        
        addAllAction = new SharingAddAllAction(audioCheckBox, videoCheckBox, imageCheckBox);
        addAllAction.setLibrary(libraryManager.getLibraryManagedList());
        shareButton = new JButton(addAllAction);
        shareButton.setFocusable(false);
        shareButton.setEnabled(false);
        shareButton.setToolTipText(I18n.tr("Use the checkboxes above to choose items to share"));
        
        
        new BoxChangeListener(shareButton, audioCheckBox, videoCheckBox, imageCheckBox);
        
        setText("");
        
        setLayout(new MigLayout("", "[grow]", ""));
        
        add(titleLabel, "center, gaptop 120, wrap 70");
        add(text, "center, top, wrap");
        
        add(audioCheckBox, "sizegroupx1, center, gaptop 30, wrap");
        add(videoCheckBox, "sizegroupx1, center, wrap");
        add(imageCheckBox, "sizegroupx1, center, wrap 30");
        add(shareButton, "center, wrap");

    }
    
    private void setText(String friendName) {
        text.setText(I18n.tr(TEXT_TEMPLATE, friendName));
        shareButton.setText(I18n.tr(BUTTON_TEXT_TEMPLATE, friendName));
        titleLabel.setText(I18n.tr(TITLE_TEMPLATE, friendName));
    }
    
    private class SharingCheckBox extends JCheckBox {
        
        public SharingCheckBox(String text) {
            super(text);
            setBorder(null);
            setFocusable(false);
            setOpaque(false);
        }
    }
    
    @Override
    public void setFriendName(String name) {
        setText(name);
    }

    @Override
    public void setEventList(EventList<LocalFileItem> model) {
    }
    
    public void setUserFileList(FriendFileList fileList) {
        // when changing the user name, reset checkboxes to false to 
        //  prevent accidental sharing
        audioCheckBox.setSelected(fileList.isAddNewAudioAlways());
        videoCheckBox.setSelected(fileList.isAddNewVideoAlways());
        imageCheckBox.setSelected(fileList.isAddNewImageAlways());
        
        addAllAction.setUserLibrary(fileList);
    }
    
    private class BoxChangeListener implements ItemListener {
        
        private JButton button;
        private JCheckBox audioBox;
        private JCheckBox videoBox;
        private JCheckBox imageBox;
        
        public BoxChangeListener(JButton button, JCheckBox audioBox, JCheckBox videoBox, JCheckBox imageBox) {
            this.button = button;
            this.audioBox = audioBox;
            this.videoBox = videoBox;
            this.imageBox = imageBox;
            
            audioBox.addItemListener(this);
            videoBox.addItemListener(this);
            imageBox.addItemListener(this);
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if(audioBox.isSelected() || videoBox.isSelected() || imageBox.isSelected())
                button.setEnabled(true);
            else
                button.setEnabled(false);
        }
    }   
}
