package org.limewire.ui.swing.library.table;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

public class ShareTableRendererEditor extends TableRendererEditor{
    @Resource
    private Icon shareGnutellaIcon;
    @Resource
    private Icon shareFriendsIcon;
    @Resource
    private Icon shareButtonIcon;
    @Resource
    private Icon shareButtonPressedIcon;

    private JLabel gnutellaLabel;
    private JLabel friendsLabel;
    private JLabel friendCountLabel;
    private JButton shareButton;
    
    public ShareTableRendererEditor(Action shareAction){
        GuiUtils.assignResources(this);
        
        gnutellaLabel = new JLabel(shareGnutellaIcon);
        friendsLabel = new JLabel(shareFriendsIcon);      
        friendCountLabel = new JLabel("1,234");
        shareButton = new IconButton(shareButtonIcon, shareButtonIcon, shareButtonPressedIcon);
        shareButton.addActionListener(shareAction);

        setLayout(new MigLayout("hidemode 0"));
        add(gnutellaLabel);
        add(friendsLabel);
        add(friendCountLabel, "pushx");
        add(shareButton);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        // TODO Auto-generated method stub
        return this;
    }

}
