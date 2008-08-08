package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CapsulePainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.jdesktop.swingx.painter.CapsulePainter.Portion;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * 
 */
public class ConversationPane extends JPanel {
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    
//    @Resource private Icon available;
//    @Resource private Icon chatStatusStub;
//    @Resource private Icon library;
//    @Resource private Icon sharing;
    
    private List<Message> messages = new ArrayList<Message>();
    private JEditorPane editor;
    private IconLibrary icons;

    @AssistedInject
    public ConversationPane(@Assisted Friend friend, IconLibrary icons) {
//        GuiUtils.assignResources(this);
        this.icons = icons;
        
        JPanel header = headerPanel(friend);
        
        setLayout(new BorderLayout());
        add(wrap(header, true), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        
        //Needed to add margin to the left side of the scrolling chat pane
        JPanel chatWrapper = new JPanel(new GridBagLayout());
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
        scroll.getViewport().add(editor);

        setBackground(DEFAULT_BACKGROUND);
    }
    
    public void handleMessage(String topic, Message message) {
        messages.add(message);
        editor.setText(ChatDocumentBuilder.buildChatText(messages));
    }
    
    private JPanel wrap(JPanel panel, boolean isHeader) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        
        GridBagConstraints con = new GridBagConstraints();
        con.insets = new Insets(isHeader ? 0 : 6, 6, 6, 6);
        con.fill = GridBagConstraints.HORIZONTAL;
        con.weightx = 1.0;
        wrapper.add(panel, con);
        return wrapper;
    }

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
}
