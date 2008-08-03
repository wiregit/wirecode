package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.FontUtils.bold;
import static org.limewire.ui.swing.util.FontUtils.changeSize;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CapsulePainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.jdesktop.swingx.painter.CapsulePainter.Portion;
import org.limewire.ui.swing.friends.Message.Type;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * 
 */
public class ConversationPane extends JPanel {
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private static final Color SELF_CHATTER_NAME = new Color(13, 48, 101);
    private static final Color OTHER_CHATTER_NAME = new Color(119, 19, 36);
    private DefaultListModel model;

    public ConversationPane(String friendName) {
        JPanel header = headerPanel();
        JLabel otherChatter = new JLabel(friendName);
        otherChatter.setForeground(Color.WHITE);
        header.add(otherChatter);
        JPanel wrapper = wrap(header, true);

        setLayout(new BorderLayout());
        add(wrapper, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane();
        scroll.setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        model = new DefaultListModel();
        JList list = new JList(model);
        list.setCellRenderer(new PassThroughCellRenderer());
        scroll.getViewport().add(list);

        setBackground(DEFAULT_BACKGROUND);
    }
    
    public void handleMessage(String topic, Message message) {
        JPanel exch = exchangePanel(message);
        model.addElement(wrap(exch, false));
    }
    
    private static class PassThroughCellRenderer implements ListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            return (Component)value;
        }
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

    private JPanel headerPanel() {
        return roundPanel(new Color(144, 144, 144), true);
    }

    private JPanel exchangePanel(Message message) {
        JXPanel exch = roundPanel(Color.WHITE, false);
        exch.setLayout(new GridBagLayout());
        GridBagConstraints cons = new GridBagConstraints();
        cons.anchor = GridBagConstraints.WEST;
        cons.insets = new Insets(8, 8, 2, 8);
        JLabel name = new JLabel(message.getSenderName());
        bold(name);
        changeSize(name, 1.4f);
        name.setForeground(message.getType() == Type.Received ? OTHER_CHATTER_NAME : SELF_CHATTER_NAME);
        exch.add(name, cons);
        JXLabel text = new JXLabel(message.getMessageText());
        text.setLineWrap(true);
        cons.gridy = 1;
        cons.insets = new Insets(2, 8, 8, 8);
        exch.add(text, cons);
        return exch;
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
