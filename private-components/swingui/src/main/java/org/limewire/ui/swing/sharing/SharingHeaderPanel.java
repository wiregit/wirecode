package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.text.MessageFormat;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.painter.SubpanelPainter;
import org.limewire.ui.swing.search.FilteredTextField;
import org.limewire.ui.swing.sharing.friends.FriendUpdate;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

/**
 * This displays the header on all the sharing panels. It includes the filter
 * box and name and icon describing what this panel is for.
 * 
 * TODO: This is very similar to SortAndFilterPanel. Once final designs are
 * ironed out these two classes should be merged and subclassed.
 */
public class SharingHeaderPanel extends JXPanel implements FriendUpdate {

    private static final int FILTER_WIDTH = 10;
    
    private final String staticText;
    
    protected JLabel descriptionLabel;
    protected JTextField filterBox;
    protected ViewSelectionPanel viewSelectionPanel;
    
    @Resource
    private int height;
    @Resource
    private Color fontColor;
    @Resource 
    private float fontSize;
    
    /**
     * Constructs sharing header panel.
     * 
     * @param icon
     * @param staticText is expected to contain the place holder '{0}' which
     * is replaced by the argument <code>name</code>, or through 
     * {@link #setFriendName(String)}
     * @param name
     * @param viewPanel
     */
    public SharingHeaderPanel(Icon icon, String staticText, String name, ViewSelectionPanel viewPanel) {
        GuiUtils.assignResources(this);
        
        setBackgroundPainter(new SubpanelPainter());
    
        this.staticText = staticText;
        this.viewSelectionPanel = viewPanel;
        createComponents();
        createComponents(icon, MessageFormat.format(staticText, name));
        layoutComponents();
        
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
    }
    
    public JTextField getFilterBox() {
        return filterBox;
    }
    
    protected void createComponents() {
    }
    
    private void createComponents(Icon icon, String text) {
        descriptionLabel = new JLabel(text, icon, JLabel.LEFT);
        descriptionLabel.setForeground(fontColor);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(fontSize));
        filterBox = new FilteredTextField(FILTER_WIDTH);
    }
    
    protected void layoutComponents() {
        setLayout(new MigLayout("insets 0 0 0 0", "", ""));

        add(descriptionLabel, "gapx 5, push");
        add(filterBox);
        add(viewSelectionPanel, "gapafter 5");
    }
    
    @Override
    public void setFriendName(String name) {
        descriptionLabel.setText(MessageFormat.format(staticText, name));
    }

    @Override
    public void setEventList(EventList<LocalFileItem> model) {

    }
}
