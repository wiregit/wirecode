package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Font;

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
import org.limewire.ui.swing.library.sharing.actions.EditSharingAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;

import com.google.inject.Inject;

@LazySingleton
public class LibrarySharingNonEditablePanel {

    @Resource Font labelFont;
    @Resource Color labelColor;
    @Resource Font linkFont;
    
    private final JPanel component;
    private final HyperlinkButton editButton;
    
    private LibrarySharingTable<String> table;
    private final LibrarySharingNonEditableRenderer renderer;
    
    @Inject
    public LibrarySharingNonEditablePanel(LibrarySharingTable<String> table, LibrarySharingNonEditableRenderer renderer,
            EditSharingAction sharingAction) {
        this.table = table;
        this.renderer = renderer;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx", "125!", ""));
        
        component.setOpaque(false);
        
        JLabel label = new JLabel(I18n.tr("Sharing list with..."));
        label.setFont(labelFont);
        label.setForeground(labelColor);
        component.add(label, "aligny top, gaptop 5, gapleft 5, wrap");
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
       
        component.add(scrollPane, "grow, wrap");
        
        editButton = new HyperlinkButton(I18n.tr("Edit Sharing"), sharingAction);
        editButton.setFont(linkFont);
        component.add(editButton, "aligny top, gaptop 5, alignx center, wrap");
    }
    
    public void setSharedFileList(SharedFileList fileList) {
        if(fileList == null)
            table.setEventList(new BasicEventList<String>());
        else
            table.setEventList(fileList.getFriendIds());
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        component.revalidate();
    }
    
    public JComponent getComponent() {
        return component;
    }
}
