package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DropTargetDragEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLEditorKit;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.CopyAction;
import org.limewire.ui.swing.action.CopyAllAction;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicEventSubscriber;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.FileUtils;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPException;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * 
 */
public class ConversationPane extends JPanel implements Displayable {
    private static final String DISABLED_LIBRARY_TOOLTIP = tr(" isn't using LimeWire. Tell them about it to see their Library");
    private static final Log LOG = LogFactory.getLog(ConversationPane.class);
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private final ArrayList<Message> messages = new ArrayList<Message>();
    private final Map<String, FileMetaData> idToFileMetaDataMap = new HashMap<String, FileMetaData>();
    private final JEditorPane editor;
    private final String conversationName;
    private final String friendId;
    private final MessageWriter writer;
    private final Friend friend;
    private final LibraryManager libraryManager;
    private final IconManager iconManager;
    private ResizingInputPanel inputPanel;
    private ChatState currentChatState;

    @AssistedInject
    public ConversationPane(@Assisted MessageWriter writer, @Assisted Friend friend, 
            LibraryManager libraryManager, IconManager iconManager) {
        this.writer = writer;
        this.friend = friend;
        this.conversationName = friend.getName();
        this.friendId = friend.getID();
        this.libraryManager = libraryManager;
        this.iconManager = iconManager;
        
        setLayout(new BorderLayout());
        
        JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        
        //Needed to add margin to the left side of the scrolling chat pane
        JPanel chatWrapper = new JPanel(new GridBagLayout());
        chatWrapper.setBackground(BACKGROUND_COLOR);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 8, 0, 0);
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        chatWrapper.add(scroll,constraints);
        
        add(chatWrapper, BorderLayout.CENTER);

        editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.addHyperlinkListener(new HyperlinkListener());
        HTMLEditorKit editorKit = (HTMLEditorKit) editor.getEditorKit();
        editorKit.setAutoFormSubmission(false);
        
        PopupUtil.addPopupMenus(editor, new CopyAction(editor), new CopyAllAction());
        
        scroll.getViewport().add(editor);
        
        add(footerPanel(writer, friend), BorderLayout.SOUTH);

        setBackground(DEFAULT_BACKGROUND);
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @RuntimeTopicEventSubscriber(methodName="getMessageReceivedTopicName")
    public void handleConversationMessage(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("Message: from {0} text: {1} topic: {2}", message.getSenderName(), message.getMessageText(), topic);
        messages.add(message);
        Type type = message.getType();
        
        if (type != Type.Sent) {
            currentChatState = ChatState.active;
        }
        
        if (type == Type.FileOffer) {
            FileMetaData fileOffer = message.getFileOffer();
            idToFileMetaDataMap.put(fileOffer.getId(), fileOffer);
        }
        
        displayMessages();
    }
    
    @RuntimeTopicEventSubscriber(methodName="getChatStateTopicName")
    public void handleChatStateUpdate(String topic, ChatStateEvent event) {
        LOG.debugf("Chat state update for {0} to {1}", event.getFriend().getName(), event.getState());
        if (currentChatState != event.getState()) {
            currentChatState = event.getState();
            displayMessages();
        }
    }
    
    @RuntimeTopicEventSubscriber(methodName="getPresenceUpdateTopicName")
    public void handlePresenceUpdate(String topic, PresenceUpdateEvent event) {
        org.limewire.xmpp.api.client.Presence.Type type = event.getPresence().getType();
        if (type == Presence.Type.unavailable) {
            displayMessages(true);
            inputPanel.getInputComponent().setEnabled(false);
        } else if (type == Presence.Type.available) {
            displayMessages(false);
            inputPanel.getInputComponent().setEnabled(true);
        }
    }
    
    public void closeChat() {
        try {
            writer.setChatState(ChatState.gone);
        } catch (XMPPException e) {
            LOG.error("Could not set chat state while closing the conversation", e);
        }
    }
    
    public void destroy() {
        EventAnnotationProcessor.unsubscribe(this);
    }
    
    public String getMessageReceivedTopicName() {
        return MessageReceivedEvent.buildTopic(friendId);
    }

    public String getChatStateTopicName() {
        return ChatStateEvent.buildTopic(friendId);
    }
    
    public String getPresenceUpdateTopicName() {
        return PresenceUpdateEvent.buildTopic(friendId);
    }
    
    private void displayMessages() {
        displayMessages(false);
    }

    private void displayMessages(boolean friendSignedOff) {
        String chatDoc = ChatDocumentBuilder.buildChatText(messages, currentChatState, conversationName, friendSignedOff);
        LOG.debugf("Chat doc: {0}", chatDoc);
        editor.setText(chatDoc);
        decorateFileOfferButtons();
    }
    
    private void decorateFileOfferButtons() {
        //This is a hack to set the file mime-type icon for file offer buttons that may appear in the conversation
        recursiveButtonSearch(editor);
    }

    private void recursiveButtonSearch(Container container) {
        for(Component component : container.getComponents()) {
            if (component instanceof Container) {
                recursiveButtonSearch((Container)component);
            }
            if (component instanceof JButton) {
                JButton button = (JButton)component;
                String buttonText = button.getText();
                String extension = FileUtils.getFileExtension(buttonText);
                if (extension != null) {
                    Icon icon = iconManager.getIconForExtension(extension);
                    button.setIcon(icon);
                }
            }
        }
    }

    private JPanel footerPanel(MessageWriter writer, Friend friend) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        JXButton libraryButton = new JXButton(tr("Library"));
        if (!friend.isSignedInToLimewire()) {
            libraryButton.setEnabled(false);
            libraryButton.setToolTipText(friend.getName() + DISABLED_LIBRARY_TOOLTIP);
        }
        panel.add(libraryButton, BorderLayout.NORTH);
        inputPanel = new ResizingInputPanel(writer);
        panel.add(inputPanel, BorderLayout.CENTER);
        
        JTextComponent inputComponent = inputPanel.getInputComponent();
        BuddyShareDropTarget buddyShare = new BuddyShareDropTarget(inputComponent, libraryManager.getLibraryList(), friend);
        inputComponent.setDropTarget(buddyShare.getDropTarget());
        
        return panel;
    }
    
    @Override
    public void handleDisplay() {
        editor.repaint();
        inputPanel.handleDisplay();
    }

    private class HyperlinkListener implements javax.swing.event.HyperlinkListener {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e instanceof FormSubmitEvent) {
                FormSubmitEvent event = (FormSubmitEvent) e;
                //Just pushed the download the file button...
                LOG.debugf("File offer download requested. FileId: {0}", event.getData());
                //TODO: Initiate download of shared file from friend
                
            } else if (EventType.ACTIVATED == e.getEventType()) {
                if (ChatDocumentBuilder.LIBRARY_LINK.equals(e.getDescription())) {
                    LOG.debugf("Opening a view to {0}'s library", friend.getName());
                    //TODO: Open the view for this friends' library
                    
                } else {
                    String linkDescription = e.getDescription();
                    LOG.debugf("Hyperlink clicked: {0}", linkDescription);
                    if (linkDescription.startsWith("magnet")) {
                        //TODO: Need to do something with magnet links
                        
                    } else {
                        NativeLaunchUtils.openURL(e.getURL().toString());
                    }
                }
            }
        }
    }

    public void offerFile(LocalFileItem file) {
        if(friend.getPresence() instanceof LimePresence) {
            file.offer((LimePresence)friend.getPresence());
        }
    }
    
    private static class BuddyShareDropTarget extends ShareDropTarget {
        private final Friend friend;

        public BuddyShareDropTarget(JTextComponent component, LocalFileList fileList, Friend friend) {
            super(component, fileList);
            this.friend = friend;
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            super.dragEnter(dtde);

            checkLimewireConnected(dtde);
        }
        
        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            super.dragOver(dtde);
            
            checkLimewireConnected(dtde);
        }

        private void checkLimewireConnected(DropTargetDragEvent dtde) {
            if (!friend.isSignedInToLimewire()) {
                dtde.rejectDrag();
            }
        }
    }
}
