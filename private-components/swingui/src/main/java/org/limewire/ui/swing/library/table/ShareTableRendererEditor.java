package org.limewire.ui.swing.library.table;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

public class ShareTableRendererEditor extends TableRendererEditor implements Configurable{
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
    private JButton shareButton;
    
    public ShareTableRendererEditor(Action shareAction){
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        
        gnutellaLabel = new JLabel(shareGnutellaIcon);
        friendsLabel = new JLabel(shareFriendsIcon);
        shareButton = new IconButton(shareButtonIcon, shareButtonIcon, shareButtonPressedIcon);
        shareButton.addActionListener(shareAction);

        friendsLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        friendsLabel.setIconTextGap(4);
        
        setLayout(new MigLayout("hidemode 0"));
        add(gnutellaLabel);
        add(friendsLabel, "pushx");
        add(shareButton);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        configure((LocalFileItem)value);
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        configure((LocalFileItem)value);
        return this;
    }
    
    @Override
    public void configure(LocalFileItem item) {
        gnutellaLabel.setVisible(item.isSharedWithGnutella());
        int friendCount = item.getFriendShareCount();
        friendsLabel.setVisible(friendCount > 0 && !item.isIncomplete());
        friendsLabel.setText(GuiUtils.toLocalizedInteger(item.getFriendShareCount()));
        shareButton.setVisible(item.isShareable() && !item.isIncomplete());
    }

}
