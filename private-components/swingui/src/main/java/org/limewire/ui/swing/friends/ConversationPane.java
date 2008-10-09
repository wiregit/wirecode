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
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URLDecoder;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLEditorKit;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.i18n.I18nMarker;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.CopyAction;
import org.limewire.ui.swing.action.CopyAllAction;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicEventSubscriber;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.ui.swing.sharing.FriendSharingDisplay;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.FileUtils;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPException;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 *
 */
public class ConversationPane extends JPanel implements Displayable {
    private static final String DISABLED_LIBRARY_TOOLTIP = I18nMarker.marktr("{0} isn't using LimeWire. Tell them about it to see their Library");
    private static final Log LOG = LogFactory.getLog(ConversationPane.class);
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private final ArrayList<Message> messages = new ArrayList<Message>();
    private final Map<String, FileMetaData> idToFileMetaDataMap = new ConcurrentHashMap<String, FileMetaData>();
    private final JEditorPane editor;
    private final String conversationName;
    private final String friendId;
    private final MessageWriter writer;
    private final ChatFriend chatFriend;
    private final ShareListManager libraryManager;
    private final IconManager iconManager;
    private final FriendSharingDisplay friendSharingDisplay;
    private ResizingInputPanel inputPanel;
    private ChatState currentChatState;
    private final ResultDownloader downloader;

    @AssistedInject
    public ConversationPane(@Assisted MessageWriter writer, @Assisted ChatFriend chatFriend,
            ShareListManager libraryManager, IconManager iconManager, FriendSharingDisplay friendSharingDisplay,
            ResultDownloader downloader) {
        this.writer = writer;
        this.chatFriend = chatFriend;
        this.conversationName = chatFriend.getName();
        this.friendId = chatFriend.getID();
        this.libraryManager = libraryManager;
        this.iconManager = iconManager;
        this.friendSharingDisplay = friendSharingDisplay;
        this.downloader = downloader;

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

        FriendShareDropTarget friendShare = new FriendShareDropTarget(editor, new ShareLocalFileList());
        editor.setDropTarget(friendShare.getDropTarget());
        scroll.getViewport().add(editor);
        
        add(footerPanel(writer, chatFriend), BorderLayout.SOUTH);

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

        if (message.hasFileOffer()) {
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
                if (!extension.isEmpty()) {
                    Icon icon = iconManager.getIconForExtension(extension);
                    button.setIcon(icon);

                    // Using end of button text to determine whether button shouild be disabled
                    // then disable it.  This is because JEditorPane does not disable buttons
                    // disabled in the form html
                    if (buttonText.endsWith(":disabled")) {
                        button.setText(buttonText.substring(0, buttonText.lastIndexOf(":disabled")));
                        button.setEnabled(false);
                    }
                }
            }
        }
    }

    private JPanel footerPanel(MessageWriter writer, ChatFriend chatFriend) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        JXButton libraryButton = new JXButton(new LibraryAction());
        if (!chatFriend.isSignedInToLimewire()) {
            libraryButton.setEnabled(false);
            libraryButton.setToolTipText(I18n.tr(DISABLED_LIBRARY_TOOLTIP, chatFriend.getName()));
        }
        panel.add(libraryButton, BorderLayout.NORTH);
        inputPanel = new ResizingInputPanel(writer);
        panel.add(inputPanel, BorderLayout.CENTER);

        JTextComponent inputComponent = inputPanel.getInputComponent();
        FriendShareDropTarget friendShare = new FriendShareDropTarget(inputComponent, new ShareLocalFileList());
        inputComponent.setDropTarget(friendShare.getDropTarget());

        return panel;
    }

    @Override
    public void handleDisplay() {
        editor.repaint();
        inputPanel.handleDisplay();
    }

    private class LibraryAction extends AbstractAction {
        public LibraryAction() {
            super(tr("Library"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            friendSharingDisplay.selectFriendLibrary(chatFriend.getFriend());
        }
    }

    private class HyperlinkListener implements javax.swing.event.HyperlinkListener {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e instanceof FormSubmitEvent) {
                FormSubmitEvent event = (FormSubmitEvent) e;
                //Just pushed the download the file button...
                LOG.debugf("File offer download requested. FileId: {0}", event.getData());

                // Initiate download of shared file from friend
                if (!(chatFriend.getPresence() instanceof LimePresence)) {
                    LOG.error("Can only download from other LimeWire clients");
                    return;
                }

                try {
                    String dataStr = event.getData();
                    String fileIdEncoded = dataStr.substring(dataStr.indexOf("=")+1).trim();
                    String fileId = URLDecoder.decode(fileIdEncoded, "UTF-8");

                    FileMetaData offeredFile = idToFileMetaDataMap.get(fileId);

                    // TODO: what if offered file not in map for any reason?
                    //       Also, when would we remove items from the map?
                   downloader.addDownload((LimePresence)chatFriend.getPresence(), offeredFile);
                } catch (IOException e1) {
                    throw new RuntimeException("FIX ME", e1);
                }

                // TODO: Track download states by adding listeners to dl item

            } else if (EventType.ACTIVATED == e.getEventType()) {
                if (ChatDocumentBuilder.LIBRARY_LINK.equals(e.getDescription())) {
                    LOG.debugf("Opening a view to {0}'s library", chatFriend.getName());
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
        if(chatFriend.getPresence() instanceof LimePresence) {
            FileMetaData metadata = file.offer((LimePresence)chatFriend.getPresence());
            new MessageReceivedEvent(new MessageImpl(null, null, friendId,
                        null, Message.Type.Sent, metadata)).publish();
        }
    }
    
    private class FriendShareDropTarget extends ShareDropTarget {
        private final ShareLocalFileList fileList;

        public FriendShareDropTarget(JComponent component, ShareLocalFileList fileList) {
            super(component, fileList);
            this.fileList = fileList;
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
            if (!chatFriend.isSignedInToLimewire()) {
                dtde.rejectDrag();
            }
        }

        @Override
        protected void dropCompleted() {
            if (fileList.size() > 0) {
                for(LocalFileItem item : fileList.getSwingModel()) {
                    offerFile(item);
                }
                fileList.clear();
            }
        }
    }
    
    private class ShareLocalFileList implements LocalFileList {
        private final EventList<LocalFileItem> eventList = GlazedLists.threadSafeList(new BasicEventList<LocalFileItem>());
        
        @Override
        public EventList<LocalFileItem> getSwingModel() {
            return eventList;
        }
        
        @Override
        public EventList<LocalFileItem> getModel() {
            return eventList;
        }
        
        @Override
        public void addFile(File file) {
            LocalFileList friendList = libraryManager.getOrCreateFriendShareList(chatFriend.getFriend());
            friendList.addFile(file);
            for (LocalFileItem item : friendList.getModel()) {
                if (file.getPath().equals(item.getFile().getPath())) {
                    eventList.add(item);
                }
            }
        }

        @Override
        public void removeFile(File file) {
            //TODO: how do you convert a File into a LocalFileItem?
        }

        @Override
        public int size() {
            return eventList.size();
        }

        @Override
        public void clear() {
            eventList.clear();
        }    
    }
}
