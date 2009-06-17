package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

@LazySingleton
class LibrarySharingFriendListPanel {

    @Resource private Font labelFont;
    @Resource private Color labelColor;
    @Resource private Font linkFont;
    @Resource private Color backgroundColor;
    
    private final JPanel component;
    private final JLabel headerLabel;
    private final HyperlinkButton editButton;
    
    private final LibrarySharingTable<String> table;
    private final LibrarySharingFriendListRenderer renderer;
    private final JScrollPane scrollPane;

    @Inject
    public LibrarySharingFriendListPanel(LibrarySharingTable<String> table,
            LibrarySharingFriendListRenderer renderer, EditSharingAction sharingAction) {
        this.table = table;
        this.renderer = renderer;

        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx", "134!", ""));
        
        component.setOpaque(false);
        
        headerLabel = new JLabel();
        headerLabel.setFont(labelFont);
        headerLabel.setForeground(labelColor);
        component.add(headerLabel, "aligny top, gaptop 8, gapleft 6, gapbottom 6, wrap");
        
        scrollPane = new JScrollPane(table);
        scrollPane.setMinimumSize(new Dimension(0,0));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(1,0,1,0));
        scrollPane.setBackground(backgroundColor);
       
        component.add(scrollPane, "grow, wrap");
        
        editButton = new HyperlinkButton(I18n.tr("Edit Sharing"), sharingAction);
        editButton.setFont(linkFont);
        component.add(editButton, "aligny top, gaptop 5, gapleft 6, gapbottom 5, wrap");
    }
    
    private void setHeaderLabelText() {
        if(table.getRowCount() > 0)
            headerLabel.setText(I18n.tr("Sharing list with..."));
        else
            headerLabel.setText(I18n.tr("Not Shared"));
    }
    
    @Inject
    void register() {
        scrollPane.getVerticalScrollBar().addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {
                scrollPane.setBorder(BorderFactory.createEmptyBorder(1,0,1,0));
            }

            @Override
            public void componentShown(ComponentEvent e) {
                scrollPane.setBorder(BorderFactory.createMatteBorder(1,0,1,0, Color.BLACK));
            }
            
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {}
        });
    }
    
    public void setSharedFileList(SharedFileList fileList) {
        table.setEventList(fileList.getFriendIds());
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        setHeaderLabelText();
        component.revalidate();
    }
    
    public JComponent getComponent() {
        return component;
    }
}
