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
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class ShareTableRendererEditor extends TableRendererEditor implements Configurable{
    @Resource
    private Icon shareGnutellaIcon;
    @Resource
    private Icon shareFriendsIcon;

    private JLabel gnutellaLabel;
    private JLabel friendsLabel;
    private JButton shareButton;
    private LocalFileItem fileItem;
    
    public ShareTableRendererEditor(Action shareAction){
        GuiUtils.assignResources(this);
       
        
        gnutellaLabel = new JLabel(shareGnutellaIcon);
        friendsLabel = new JLabel(shareFriendsIcon);
        shareButton = new JButton(I18n.tr("Share"));
        shareButton.setFocusPainted(false);
        shareButton.setVisible(false);
        shareButton.setBorder(null);
        shareButton.setContentAreaFilled(false);
        shareButton.setVisible(false);
        
        FontUtils.underline(shareButton);
        shareButton.addActionListener(shareAction);

        friendsLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        friendsLabel.setIconTextGap(4);
        
        setLayout(new MigLayout("insets 2 5 2 5, hidemode 0, aligny 50%"));
        add(gnutellaLabel);
        add(friendsLabel, "pushx");
        add(shareButton);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        configure((LocalFileItem)value, isSelected);
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        configure((LocalFileItem)value, true);
        return this;
    }
    
    @Override
    public void configure(LocalFileItem item, boolean isRowSelected) {
        fileItem = item;
        gnutellaLabel.setVisible(item.isSharedWithGnutella());
        int friendCount = item.getFriendShareCount();
        friendsLabel.setVisible(friendCount > 0 && !item.isIncomplete());
        friendsLabel.setText(GuiUtils.toLocalizedInteger(item.getFriendShareCount()));
        shareButton.setVisible(item.isShareable() && !item.isIncomplete() && isRowSelected);
    }

    public LocalFileItem getLocalFileItem() {
        return fileItem;
    }
    
    public JButton getShareButton(){
        return shareButton;
    }
}
