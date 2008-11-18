package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.features.FileOfferFeature;
import org.limewire.core.api.friend.feature.features.FileOfferer;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.xmpp.RemoteFileItemFactory;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
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
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.FileUtils;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPException;

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

    private final List<Message> messages = new ArrayList<Message>();
    private final Map<String, MessageFileOffer> idToMessageWithFileOffer =
            new ConcurrentHashMap<String, MessageFileOffer>();

    private final JEditorPane editor;
    private final String conversationName;
    private final String friendId;
    private final String loggedInID;
    private final MessageWriter writer;
    private final ChatFriend chatFriend;
    private final ShareListManager shareListManager;
    private final IconManager iconManager;
    private final FriendSharingDisplay friendSharingDisplay;
    private ResizingInputPanel inputPanel;
    private ChatState currentChatState;
    private final ResultDownloader downloader;
    private final RemoteFileItemFactory remoteFileItemFactory;

    @AssistedInject
    public ConversationPane(@Assisted MessageWriter writer, @Assisted ChatFriend chatFriend, @Assisted String loggedInID,
                            ShareListManager libraryManager, IconManager iconManager, FriendSharingDisplay friendSharingDisplay,
                            ResultDownloader downloader, RemoteFileItemFactory remoteFileItemFactory) {
        this.writer = writer;
        this.chatFriend = chatFriend;
        this.remoteFileItemFactory = remoteFileItemFactory;
        this.conversationName = chatFriend.getName();
        this.friendId = chatFriend.getID();
        this.loggedInID = loggedInID;
        this.shareListManager = libraryManager;
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

        FriendShareDropTarget friendShare = new FriendShareDropTarget(editor, libraryManager.getOrCreateFriendShareList(chatFriend.getFriend()));
        editor.setDropTarget(friendShare.getDropTarget());
        scroll.getViewport().add(editor);

        add(footerPanel(writer, chatFriend), BorderLayout.SOUTH);

        setBackground(DEFAULT_BACKGROUND);

        EventAnnotationProcessor.subscribe(this);
    }

    @RuntimeTopicEventSubscriber(methodName="getMessageReceivedTopicName")
    public void handleConversationMessage(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("Message: from {0} text: {1} topic: {2}", message.getSenderName(), message.toString(), topic);
        messages.add(message);
        Type type = message.getType();

        if (type != Type.Sent) {
            currentChatState = ChatState.active;
        }

        if (message instanceof MessageFileOffer) {
            MessageFileOffer msgWithFileOffer = (MessageFileOffer)message;
            String fileOfferID = msgWithFileOffer.getFileOffer().getId();
            idToMessageWithFileOffer.put(fileOfferID, msgWithFileOffer);
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
        Presence updatedPresence = event.getPresence();
        org.limewire.xmpp.api.client.Presence.Type type = updatedPresence.getType();
        if ((type == Presence.Type.unavailable) && (!updatedPresence.getUser().isSignedIn())) {
            displayMessages(true);
            inputPanel.getInputComponent().setEnabled(false);
        } else if (event.isNewPresence() &&
                type == Presence.Type.available) {
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
                }

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
        FriendShareDropTarget friendShare = new FriendShareDropTarget(inputComponent, shareListManager.getOrCreateFriendShareList(chatFriend.getFriend()));
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

                DownloadItem dl;
                final MessageFileOffer msgWithfileOffer;
                try {
                    String dataStr = event.getData();
                    String fileIdEncoded = dataStr.substring(dataStr.indexOf("=")+1).trim();
                    String fileId = URLDecoder.decode(fileIdEncoded, "UTF-8");
                    msgWithfileOffer = idToMessageWithFileOffer.get(fileId);

                    RemoteFileItem file = remoteFileItemFactory.create(chatFriend.getBestPresence(),
                           msgWithfileOffer.getFileOffer());
                    // TODO: what if offered file not in map for any reason?
                    //       Also, when would we remove items from the map?
                   dl = downloader.addFriendDownload(file);
                } catch(SaveLocationException sle) {
                    throw new RuntimeException("FIX ME", sle); // BROKEN
                } catch (InvalidDataException ide) {
                    // not exactly broken, but need better behavior --
                    // this means the FileMetaData we received isn't well-formed.
                    throw new RuntimeException("FIX ME", ide);
                } catch(UnsupportedEncodingException uee) {
                    throw new RuntimeException(uee); // impossible
                }

                // Track download states by adding listeners to dl item
                dl.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("state".equals(evt.getPropertyName())) {
                            DownloadState state = (DownloadState) evt.getNewValue();
                            msgWithfileOffer.setDownloadState(state);
                            displayMessages();
                        }
                    }
                });

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
    
    public void offerFolder(File folder, ListeningFuture<List<ListeningFuture<LocalFileItem>>> future) {
        // TODO: Change this to show event immediately & update as status changes.
        future.addFutureListener(new EventListener<FutureEvent<List<ListeningFuture<LocalFileItem>>>>() {
            @Override
            public void handleEvent(FutureEvent<List<ListeningFuture<LocalFileItem>>> event) {
                if(event.getResult() != null) {
                    for(ListeningFuture<LocalFileItem> future : event.getResult()) {
                        offerFile(null, future);
                    }
                }
            }
        });
    }

    public void offerFile(File file, ListeningFuture<LocalFileItem> future) {
        // TODO: Change this to show event immediately & update as status changes.
        future.addFutureListener(new EventListener<FutureEvent<LocalFileItem>>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(FutureEvent<LocalFileItem> event) {
               if(event.getResult() != null) {
                   FriendPresence presence = chatFriend.getBestPresence();
                   if(presence.getFeature(FileOfferFeature.ID) != null) {
                       // update the chat state in case the remote user has not started conversation with us
                       try {
                           writer.setChatState(ChatState.active);
                       } catch (XMPPException e) {
                           LOG.error("Unable to set chat state prior to file offer", e);
                       }

                       // TODO: Listen for the file being added to the library &
                       //       update the MFOI based on status.
                       //       Send to offerer when File becomes FileItem.
                       FileOfferer fileOfferer = ((FileOfferFeature)presence.getFeature(FileOfferFeature.ID)).getFeature();
                       FileMetaData metadata = event.getResult().toMetadata();
                       fileOfferer.offerFile(metadata);
                       new MessageReceivedEvent(new MessageFileOfferImpl(loggedInID, null, friendId,
                                   Message.Type.Sent, metadata)).publish();
                   } else {
                       // TODO
                   }
               }
            } 
        });
        
    }
    
    private class FriendShareDropTarget extends ShareDropTarget {
        public FriendShareDropTarget(JComponent component, LocalFileList fileList) {
            super(component, fileList);
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
        protected void acceptedFile(File file, ListeningFuture<LocalFileItem> future) {
            offerFile(file, future);
        }
        
        @Override
        protected void acceptedFolder(File folder, ListeningFuture<List<ListeningFuture<LocalFileItem>>> future) {
            offerFolder(folder, future);
        }
    }
}
