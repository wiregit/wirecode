package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.HyperlinkTextUtil.hyperlinkText;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is a panel containing three "action" buttons
 * for the actions "download", "more info" and "mark as junk".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ActionButtonPanel extends JXPanel {

    private final String[] TOOLTIPS =
        { tr("Download"), tr("More Info"), tr("Mark as Junk") };
    
    private static final int DOWNLOAD = 0;    
    private static final int MORE_INFO = 1;    
    private static final int MARK_AS_JUNK = 2;    

    @Resource private Icon downloadDownIcon;
    @Resource private Icon downloadOverIcon;
    @Resource private Icon downloadUpIcon;
    @Resource private Icon infoDownIcon;
    @Resource private Icon infoOverIcon;
    @Resource private Icon infoUpIcon;
    @Resource private Icon junkDownIcon;
    @Resource private Icon junkOverIcon;
    @Resource private Icon junkUpIcon;

    private Icon[][] icons;
    private JButton downloadButton;
    private JXHyperlink downloadingLink = new JXHyperlink();
    private JButton infoButton;
    private JToggleButton junkButton;
    private int height;
    private VisualSearchResult vsr;
    private int currentRow;

    public ActionButtonPanel(final Navigator navigator) {
        GuiUtils.assignResources(this);

        downloadButton = new JButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        downloadingLink.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (vsr.getDownloadState() == BasicDownloadState.DOWNLOADING) {
                    navigator.getNavItem(
                        NavCategory.DOWNLOAD,
                        MainDownloadPanel.NAME).select();
                } else if (vsr.getDownloadState() == BasicDownloadState.DOWNLOADED) {
                    throw new UnsupportedOperationException("Implement Me Properly!");                    
//                    navigator.getNavItem(
//                        NavCategory.LIBRARY,
//                        MyLibraryPanel.NAME).select();
                    
                }
            }
        });
        
        infoButton = new JButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        junkButton = new JToggleButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        icons = new Icon[][] {
            { downloadUpIcon, downloadOverIcon, downloadDownIcon },
            { infoUpIcon, infoOverIcon, infoDownIcon },
            { junkUpIcon, junkOverIcon, junkDownIcon }
        };
        calculateHeight();

        setLayout(new GridLayout(1, 3));
        addButtonsToPanel();

        // Set the tooltip delay to zero only when the mouse is over this panel.
        addMouseListener(new MouseAdapter() {
            private int delay;

            @Override
            public void mouseEntered(MouseEvent e) {
                // Save the previous delay.
                delay = ToolTipManager.sharedInstance().getInitialDelay();
                ToolTipManager.sharedInstance().setInitialDelay(0);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Restore the previous delay.
                ToolTipManager.sharedInstance().setInitialDelay(delay);
            }
        });
    }
    
    public void startDownload() {
        // Find the BaseResultPanel this is inside.
        Container parent = getParent();
        while (!(parent instanceof BaseResultPanel)) {
            parent = parent.getParent();
        }
        BaseResultPanel brp = (BaseResultPanel) parent;

        brp.download(vsr, currentRow);
        setDownloadingDisplay(vsr);
    }
    
    public void setVisualSearchResult(VisualSearchResult vsr) {
        this.vsr = vsr;
    }

    private void calculateHeight() {
        for (Icon[] iconSet : icons) {
            Icon upIcon = iconSet[0];
            height = Math.max(height, upIcon.getIconHeight());
        }
    }

    private void addButtonsToPanel() {
        int buttonIndex = 0;

        for (Icon[] iconSet : icons) {
            Icon upIcon = iconSet[0];
            Icon overIcon = iconSet[1];
            Icon downIcon = iconSet[2];

            AbstractButton button = getButton(buttonIndex);
            if (button == null) continue; // should never happen

            button.setIcon(upIcon);
            button.setRolloverIcon(overIcon);
            button.setPressedIcon(downIcon);
            button.setSelectedIcon(downIcon);

            button.setToolTipText(TOOLTIPS[buttonIndex++]);

            button.setBorderPainted(false);
            button.setContentAreaFilled(false);

            Dimension size =
                new Dimension(upIcon.getIconWidth(), upIcon.getIconHeight());
            button.setPreferredSize(size);

            add(button, buttonIndex == TOOLTIPS.length ? "wrap" : "");
        }
    }

    private AbstractButton getButton(int buttonIndex) {
        return buttonIndex == DOWNLOAD ? downloadButton :
            buttonIndex == MORE_INFO ? infoButton :
            buttonIndex == MARK_AS_JUNK ? junkButton : null;
    }
    
    /**
     * Gets the "Mark as Junk" button.
     * @return the button
     */
    public JToggleButton getSpamButton() {
        return junkButton;
    }

    /**
     * Gets the rollover icon of the button at a given index.
     * @param buttonIndex the button index
     * @return the rollover icon
     */
    private Icon getRolloverIcon(int buttonIndex) {
        return buttonIndex == DOWNLOAD ? downloadOverIcon :
            buttonIndex == MORE_INFO ? infoOverIcon :
            buttonIndex == MARK_AS_JUNK ? junkOverIcon : null;
    }

    private Point getToolTipOffset(JComponent component) {
        // Determine the size of the tooltip.
        Font font = component.getFont();
        FontMetrics fm = component.getFontMetrics(font);
        String text = component.getToolTipText();
        int toolTipWidth = fm.stringWidth(text);
        int toolTipHeight = fm.getHeight();

        Dimension componentSize = component.getSize();

        int x = componentSize.width/2 - toolTipWidth/2 + 1;
        int y = -toolTipHeight - 3;

        return new Point(x, y);
    }

    /**
     * Gets the up icon of the button at a given index.
     * @param buttonIndex the button index
     * @return the up icon
     */
    private Icon getUpIcon(int buttonIndex) {
        return buttonIndex == DOWNLOAD ? downloadUpIcon :
            buttonIndex == MORE_INFO ? infoUpIcon :
            buttonIndex == MARK_AS_JUNK ? junkUpIcon : null;
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        
        Component[] components = getComponents();
        for (Component component : components) {
            component.setBackground(bg);
        }
    }

    /**
     * Changes the "normal" icon of the specified button
     * based on whether it should simulate a mouse rollover.
     * @param buttonIndex the button index
     * @param rollover true to simulate a rollover; false otherwise
     */
    public void setRollover(int buttonIndex, boolean rollover) {
        AbstractButton button = getButton(buttonIndex);
        Icon icon = rollover ?
            getRolloverIcon(buttonIndex) : getUpIcon(buttonIndex);
        button.setIcon(icon);
    }
    
    public void setDownloadingDisplay(VisualSearchResult vsr) {
        switch (vsr.getDownloadState()) {
            case NOT_STARTED:
                downloadButton.setEnabled(true);
                downloadingLink.setText("");
                downloadingLink.setVisible(false);
                break;
            case DOWNLOADING:
                downloadButton.setEnabled(false);
                downloadingLink.setText(hyperlinkText(tr("Downloading...")));
                downloadingLink.setVisible(true);
                break;
            case DOWNLOADED:
                downloadButton.setEnabled(false);
                downloadingLink.setText(hyperlinkText(tr("Download Complete")));
                downloadingLink.setVisible(true);
                break;
        }
    }

    public void setRow(int row) {
        this.currentRow = row;        
    }
    
    public void configureForListView(final JTable table) {
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startDownload();
                table.editingStopped(new ChangeEvent(table));
            }
        });
        
        removeAll();
        setLayout(new MigLayout("insets 0 0 0 0", "0[]0[]0[]0", "0[]0[]0"));
        addButtonsToPanel();
        FontUtils.changeSize(downloadingLink, -2.0f);
        add(downloadingLink, "span");
    }
}