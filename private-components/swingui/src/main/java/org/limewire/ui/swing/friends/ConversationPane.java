package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CapsulePainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.jdesktop.swingx.painter.CapsulePainter.Portion;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicEventSubscriber;
import org.limewire.xmpp.api.client.MessageWriter;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * 
 */
public class ConversationPane extends JPanel implements Displayable {
    private static final Log LOG = LogFactory.getLog(ConversationPane.class);
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private ArrayList<Message> messages = new ArrayList<Message>();
    private JEditorPane editor;
    private IconLibrary icons;
    private final String conversationName;
    private final Color BACKGROUND_COLOR = Color.WHITE;
    private ResizingInputPanel inputPanel;

    @AssistedInject
    public ConversationPane(@Assisted MessageWriter writer, @Assisted String conversationName, IconLibrary icons) {
        this.icons = icons;
        this.conversationName = conversationName;
        
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
        
        scroll.getViewport().add(editor);
        
        add(footerPanel(writer), BorderLayout.SOUTH);

        setBackground(DEFAULT_BACKGROUND);
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @RuntimeTopicEventSubscriber
    public void handleConversationMessage(String topic, MessageReceivedEvent event) {
        LOG.debugf("Message: from {0} text: {1}", event.getMessage().getSenderName(), event.getMessage().getMessageText());
        handleMessage(event.getMessage());
    }
    
    public String getTopicName() {
        return MessageReceivedEvent.buildTopic(conversationName);
    }
    
    private void handleMessage(Message message) {
        messages.add(message);
        String chatDoc = ChatDocumentBuilder.buildChatText(messages);
        LOG.debugf("Chat doc: {0}", chatDoc);
        editor.setText(chatDoc);
    }
    
    private JPanel footerPanel(MessageWriter writer) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.add(new JXButton(tr("Library")), BorderLayout.NORTH);
        inputPanel = new ResizingInputPanel(writer);
        panel.add(inputPanel, BorderLayout.CENTER);
        return panel;
    }
    
    @Override
    public void handleDisplay() {
        editor.repaint();
        inputPanel.handleDisplay();
    }

    @SuppressWarnings("unused")
    private JPanel wrap(JPanel panel, boolean isHeader) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        
        GridBagConstraints con = new GridBagConstraints();
        con.insets = new Insets(isHeader ? 0 : 6, 6, 6, 6);
        con.fill = GridBagConstraints.HORIZONTAL;
        con.weightx = 1.0;
        wrapper.add(panel, con);
        return wrapper;
    }

    @SuppressWarnings("unused")
    private JPanel headerPanel(Friend friend) {
        JXPanel headerPanel = roundPanel(new Color(144, 144, 144), true);
        
        headerPanel.setLayout(new BorderLayout());

        JPanel statusPanel = new JPanel(new GridBagLayout());
        statusPanel.setOpaque(false);
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridheight = 2;
        constraints.insets = new Insets(8, 8, 8, 0);
        statusPanel.add(new JLabel(icons.getChatStatusStub()), constraints);

        JLabel name = new JLabel(friend.getName(), icons.getAvailable(), SwingConstants.LEFT);
        name.setForeground(Color.WHITE);
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.insets = new Insets(8, 4, 0, 8);
        statusPanel.add(name, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 4, 8, 8);
        statusPanel.add(new JLabel(friend.getStatus()), constraints);
        
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new GridBagLayout());

        constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 0, 0, 0);
        JXButton libraryButton = new JXButton(icons.getLibrary());
        libraryButton.setBorderPainted(false);
        buttonsPanel.add(libraryButton, constraints);
        JXButton sharingButton = new JXButton(icons.getSharing());
        sharingButton.setBorderPainted(false);
        buttonsPanel.add(sharingButton, constraints);

        JLabel libraryLabel = new JLabel(tr("Library"));
        libraryLabel.setForeground(Color.WHITE);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 8, 0);
        buttonsPanel.add(libraryLabel, constraints);

        JLabel sharingLabel = new JLabel(tr("Sharing"));
        sharingLabel.setForeground(Color.WHITE);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 8, 8, 8);
        buttonsPanel.add(sharingLabel, constraints);
        
        headerPanel.add(statusPanel, BorderLayout.WEST);
        headerPanel.add(buttonsPanel, BorderLayout.EAST);
        
        return headerPanel;
    }

    private JXPanel roundPanel(Color backgroundColor, boolean flatTop) {
        JXPanel panel = new JXPanel();
        RectanglePainter painter = new RectanglePainter(backgroundColor, backgroundColor);
        painter.setRounded(true);
        painter.setRoundHeight(20);
        painter.setRoundWidth(20);

        Painter panelPainter = painter;
        if (flatTop) {
            CapsulePainter capsulePainter = new CapsulePainter(Portion.Bottom);
            capsulePainter.setFillPaint(backgroundColor);
            panelPainter = new CompoundPainter(painter, capsulePainter);
        }
        
        panel.setBackgroundPainter(panelPainter);
        return panel;
    }
    
    private class HyperlinkListener implements javax.swing.event.HyperlinkListener {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            // TODO Auto-generated method stub
        }
    }
}
