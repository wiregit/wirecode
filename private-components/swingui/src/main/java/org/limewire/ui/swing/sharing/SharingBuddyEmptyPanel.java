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
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.actions.SharingAddAllAction;
import org.limewire.ui.swing.sharing.friends.BuddyUpdate;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SharingBuddyEmptyPanel extends JPanel implements BuddyUpdate {

    @Resource
    Icon buddyIcon;
    
    private JLabel titleLabel;
    private JLabel text;
    
    private SharingCheckBox audioCheckBox;
    private SharingCheckBox videoCheckBox;
    private SharingCheckBox imageCheckBox;
    
    private JButton shareButton;
    
    private SharingAddAllAction addAllAction;
    
    private final String title = "You are not sharing anything with ";
    private final String buttonText = "Shared with ";
    
    private final String textPart1 = "To share with ";
    private final String textPart2 = ", drag files here, or use the shortcuts below to share files";
    
    @Inject
    public SharingBuddyEmptyPanel(LibraryManager libraryManager) {
        
        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
        
        titleLabel = new JLabel(buddyIcon);
        
        text = new JLabel();

        audioCheckBox = new SharingCheckBox("All my music");
        videoCheckBox = new SharingCheckBox("All my video");
        imageCheckBox = new SharingCheckBox("All my images");
        
        addAllAction = new SharingAddAllAction(audioCheckBox, videoCheckBox, imageCheckBox);
        addAllAction.setLibrary(libraryManager.getLibraryList());
        shareButton = new JButton(addAllAction);
        shareButton.setFocusable(false);
        shareButton.setEnabled(false);
        shareButton.setToolTipText("Use the checkboxes above to choose items to share");
        
        
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
    
    private void setText(String buddyName) {
        text.setText(textPart1 + buddyName + textPart2);
        shareButton.setText(buttonText + buddyName);
        titleLabel.setText( title + buddyName);
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
    public void setBuddyName(String name) {
        setText(name);
        
        // when changing the user name, reset checkboxes to false to 
        //  prevent accidental sharing
        audioCheckBox.setSelected(false);
        videoCheckBox.setSelected(false);
        imageCheckBox.setSelected(false);
    }

    @Override
    public void setEventList(EventList<FileItem> model) {
//        addAllAction.setUserLibrary(model);
    }
    
    public void setUserFileList(FileList fileList) {
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
