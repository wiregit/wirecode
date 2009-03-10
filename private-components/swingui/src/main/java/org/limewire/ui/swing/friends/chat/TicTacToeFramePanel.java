package org.limewire.ui.swing.friends.chat;



import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.PanelDisplayedEvent;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.sound.WavSoundPlayer;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.EnabledListener;
import org.limewire.ui.swing.util.EnabledListenerList;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.ui.swing.util.VisibleComponent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The main frame of the chat panel.
 * 
 * All visible aspects of chat are rendered in this panel.
 */
 @Singleton
 public class TicTacToeFramePanel extends JXPanel implements Resizable, VisibleComponent {
     private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
     private static final Log LOG = LogFactory.getLog(TicTacToeFramePanel.class);
     private static final String MESSAGE_SOUND_PATH = "/org/limewire/ui/swing/mainframe/resources/sounds/friends/message.wav";
     private final TicTacToePanel tictactoePanel;
     private final TicTacToeFriendListPane tictacToeFriendListPane;
     private final TrayNotifier notifier;

     //Heavy-weight component so that it can appear above other heavy-weight components
     private final java.awt.Panel mainPanel;

     private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
     private final EnabledListenerList enabledListenerList = new EnabledListenerList();
     private boolean actionEnabled = false;

     private UnseenMessageListener unseenMessageListener;
     private String lastSelectedConversationFriendId;
     private String mostRecentConversationFriendId;
     private JXPanel borderPanel;

     @Resource private Color border;

     @Inject
     public TicTacToeFramePanel(TicTacToePanel tictactoePanel, TicTacToeFriendListPane  tictactoeFriendListPane, TrayNotifier notifier) {
         super(new BorderLayout());

         GuiUtils.assignResources(this);

         this.tictactoePanel = tictactoePanel;
         this.tictacToeFriendListPane = tictactoeFriendListPane;
         this.notifier = notifier;
         this.mainPanel = new java.awt.Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));

         borderPanel = new JXPanel(new MigLayout("insets 1 1 0 1"));
         borderPanel.setBackgroundPainter(new TicTacToePanelPainter());

         borderPanel.add(tictactoePanel);

         mainPanel.setVisible(false);        
         add(mainPanel);
         setVisible(false);

         tictactoePanel.setMinimizeAction(new AbstractAction() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 setTicTacToePanelVisible(false);
             }
         });

         EventAnnotationProcessor.subscribe(this);
     }

     @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
         connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
             @Override
             @SwingEDTEvent
             public void handleEvent(XMPPConnectionEvent event) {
                 switch(event.getType()) {
                 case CONNECTED:
                     handleConnectionEstablished(event);
                     break;
                 case DISCONNECTED:
                     handleLogoffEvent();
                     break;
                 }
             }
         });
     }


     public void setUnseenMessageListener(UnseenMessageListener unseenMessageListener) {
         this.unseenMessageListener = unseenMessageListener;
     }

     @Override
     public void toggleVisibility() {
         boolean shouldDisplay = !isVisible();
         setVisibility(shouldDisplay);
     }

     public void setTicTacToePanelVisible(boolean shouldDisplay) {
         if(shouldDisplay) {
             resetBounds();
         }

         mainPanel.setVisible(shouldDisplay);
         setVisible(shouldDisplay);
         if (shouldDisplay) {
             getDisplayable().handleDisplay();
             new PanelDisplayedEvent(this).publish();
         }
         visibilityListenerList.visibilityChanged(shouldDisplay);
     }

     private Displayable getDisplayable() {
         return tictactoePanel;
     }

     /**
      * Hides FriendsPanel when another panel is shown in the same layer.
      */
      @EventSubscriber
      public void handleOtherPanelDisplayed(PanelDisplayedEvent event){
          if(event.getDisplayedPanel() != this){
              setVisible(false);
              mainPanel.setVisible(false);
          }
      }

      @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
      public void handleMessageReceived(String topic, MessageReceivedEvent event) {
          if (event.getMessage().getType() != Message.Type.Sent) {
              String messageFriendID = event.getMessage().getFriendID();
              mostRecentConversationFriendId = messageFriendID;
              notifyUnseenMessageListener(event);
              if(isVisible()) {
                  tictacToeFriendListPane.markActiveConversationRead();
              }
          }
          if (event.getMessage().getType() != Message.Type.Sent &&
                  (!GuiUtils.getMainFrame().isActive() || !isVisible())) {
              LOG.debug("Sending a message to the tray notifier");
              notifier.showMessage(getNoticeForMessage(event));

              URL soundURL = ChatFramePanel.class.getResource(MESSAGE_SOUND_PATH);
              if (soundURL != null && SwingUiSettings.PLAY_NOTIFICATION_SOUND.getValue()) {
                  ThreadExecutor.startThread(new WavSoundPlayer(soundURL.getFile()), "newmessage-sound");
              }
          } 
      }

      private void notifyUnseenMessageListener(MessageReceivedEvent event) {
          String messageFriendID = event.getMessage().getFriendID();
          if (!messageFriendID.equals(lastSelectedConversationFriendId) || !isVisible()) {
              unseenMessageListener.messageReceivedFrom(messageFriendID, isVisible());
          }
      }

      @EventSubscriber
      public void handleConversationSelected(ConversationSelectedEvent event) {
          if (event.isLocallyInitiated()) {
              lastSelectedConversationFriendId = event.getFriend().getID();
              unseenMessageListener.conversationSelected(lastSelectedConversationFriendId);
              borderPanel.invalidate();
              borderPanel.repaint();
          }

          visibilityListenerList.visibilityChanged(true);
      }

      @EventSubscriber
      public void handleChatClosed(CloseChatEvent event) {
          lastSelectedConversationFriendId = null;

          if (event.getFriend().getID().equals(mostRecentConversationFriendId)) {
              mostRecentConversationFriendId = null;
          }
          borderPanel.invalidate();
          borderPanel.repaint();

          visibilityListenerList.visibilityChanged(true);
      }

      private Notification getNoticeForMessage(MessageReceivedEvent event) {
          final Message message = event.getMessage();
          String title = tr("Chat from {0}", message.getSenderName());
          String messageString = message.toString();
          Notification notification = new Notification(title, messageString, new AbstractAction(I18n.tr("Reply")) {
              @Override
              public void actionPerformed(ActionEvent e) {
                  ActionMap map = Application.getInstance().getContext().getActionManager()
                  .getActionMap();
                  map.get("restoreView").actionPerformed(e);
                  setTicTacToePanelVisible(true);
                  tictacToeFriendListPane.fireConversationStarted(message.getFriendID());
              }
          });
          return notification;
      }

      private void handleConnectionEstablished(XMPPConnectionEvent event) {
          addChatPanel();
          tictactoePanel.setLoggedInID(formatLoggedInName(event.getSource().getConfiguration().getCanonicalizedLocalID()));
          resetBounds();
          setActionEnabled(true);
      }

      private String formatLoggedInName(String fullLoggedInId) {
          int index = fullLoggedInId.lastIndexOf("@");
          return (index == -1) ? fullLoggedInId : fullLoggedInId.substring(0, index);
      }

      private void addChatPanel() {
          mainPanel.add(borderPanel);
      }

      private void handleLogoffEvent() {
          removeChatPanel();
          resetBounds();
          setTicTacToePanelVisible(false);
          setActionEnabled(false);
          lastSelectedConversationFriendId = null;
          mostRecentConversationFriendId = null;
          unseenMessageListener.clearUnseenMessages();
      }

      private void removeChatPanel() {
          mainPanel.remove(borderPanel);
      }

      public String getMessagingTopicPatternName() {
          return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
      }

      private void resetBounds() {
          Rectangle parentBounds = getParent().getBounds();
          Dimension childPreferredSize = mainPanel.getPreferredSize();
          int w = (int) childPreferredSize.getWidth();
          int h = (int) childPreferredSize.getHeight();
          setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
      }

      @Override
      public void resize() {
          resetBounds();
      }

      @Override
      public void addVisibilityListener(VisibilityListener listener) {
          visibilityListenerList.addVisibilityListener(listener);
      }

      @Override
      public void removeVisibilityListener(VisibilityListener listener) {
          visibilityListenerList.removeVisibilityListener(listener);
      }

      @Override
      public void setVisibility(boolean visible) {
          if(visible) {
              //make the most recent conversation the active one when opening the chat window
              if(mostRecentConversationFriendId != null) {
                  tictacToeFriendListPane.fireConversationStarted(mostRecentConversationFriendId);
              }
              tictacToeFriendListPane.markActiveConversationRead();
          }
          setTicTacToePanelVisible(visible);
          visibilityListenerList.visibilityChanged(visible);
      }

      @Override
      public void addEnabledListener(EnabledListener listener) {
          enabledListenerList.addEnabledListener(listener);
      }

      @Override
      public void removeEnabledListener(EnabledListener listener) {
          enabledListenerList.removeEnabledListener(listener);
      }

      /**
       * Returns true if the component is enabled for use. 
       */
      @Override
      public boolean isActionEnabled() {
          return actionEnabled;
      }

      /**
       * Sets an indicator to determine whether the component is enabled for use,
       * and notifies all registered EnabledListener instances. 
       */
      private void setActionEnabled(boolean enabled) {
          // Get old value, and save new value.
          boolean oldValue = actionEnabled;
          actionEnabled = enabled;

          // Notify listeners if value changed.
          if (enabled != oldValue) {
              enabledListenerList.fireEnabledChanged(enabled);
          }
      }

      /**
       * Returns the last selected friend id. Or null if none is selected. 
       */
      public String getLastSelectedConversationFriendId() {
          return lastSelectedConversationFriendId;
      }

      private class TicTacToePanelPainter extends AbstractPainter {

          public TicTacToePanelPainter() {
              this.setCacheable(true);
              this.setAntialiasing(true);
          }

          @Override
          protected void doPaint(Graphics2D g, Object object, int width, int height) {
              g.setPaint(border);
              g.drawLine(0, 0, 0, height-1);
              g.drawLine(0, 0, width-1, 0);
              g.drawLine(width-1, 0, width-1, height-1);
          }
      }
 }
