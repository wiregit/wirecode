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
import org.jdesktop.swingx.JXLabel;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.friends.FriendsCountUpdater;
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
    private ShareListManager shareManager;
    
    public ShareTableRendererEditor(Action shareAction, ShareListManager shareManager){
        GuiUtils.assignResources(this);
        
        this.shareManager = shareManager;
        
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
        configure(table, value, isSelected, hasFocus, row, column);
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        configure(table, value, isSelected, false, row, column);
        return this;
    }
    
    private void configure(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        LocalFileItem item = (LocalFileItem)value;
        if(item != null) {
            gnutellaLabel.setVisible(item.isSharedWithGnutella());
            int friendCount = item.getFriendShareCount();
            friendsLabel.setVisible(friendCount > 0);
            friendsLabel.setText(GuiUtils.toLocalizedInteger(item.getFriendShareCount()));
            shareButton.setEnabled(item.isShareable());
        }
    }

}
