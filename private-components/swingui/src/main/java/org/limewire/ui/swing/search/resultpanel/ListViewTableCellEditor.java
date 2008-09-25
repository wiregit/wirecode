package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.HyperlinkTextUtil.hyperlinkText;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ListViewTableCellEditor
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {

    private static final int SIMILARITY_INDENTATION = 20;
    private final Log LOG = LogFactory.getLog(getClass());
    private final Color SELECTED_COLOR = Color.GREEN;

    private final String SEARCH_TEXT_COLOR = "red";

    public static final int HEIGHT = 60;
    public static final int LEFT_WIDTH = 440;
    public static final int WIDTH = 740;

    private String searchText;

    @Resource private Icon itemIcon;
    
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");

    private ActionColumnTableCellEditor actionEditor;
    private ActionButtonPanel actionButtonPanel;
    private FromWidget fromWidget = new FromWidget();
    private JLabel itemIconLabel;
    private JXHyperlink similarButton = new JXHyperlink();
    private JLabel headingLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JLabel otherLabel = new JLabel();
    private JXPanel actionPanel = new JXPanel();
    private JXPanel thePanel;
    private Category category;

    private VisualSearchResult vsr;
    private JComponent similarResultIndentation;

    @Inject
    public ListViewTableCellEditor(
        ActionColumnTableCellEditor actionEditor) {

        this.actionEditor = actionEditor;
        FontUtils.changeSize(similarButton, -2.0F);

        GuiUtils.assignResources(this);
    }

    /**
     * Adds an HTML bold tag around every occurrence of highlightText.
     * Note that comparisons are case insensitive.
     * @param text the text to be modified
     * @return the text containing bold tags
     */
    private String highlightMatches(String sourceText) {
        boolean haveSearchText = searchText != null && searchText.length() > 0;
        LOG.debugf("haveSearchText = {0}", haveSearchText);

        // If there is no search or filter text then return sourceText as is.
        if (!haveSearchText)
            return sourceText;

        String lowerText = sourceText.toLowerCase();
        if (haveSearchText)
            searchText = searchText.toLowerCase();

        int searchIndex = haveSearchText ? lowerText.indexOf(searchText) : -1;

        boolean foundSearchText = searchIndex != -1;

        // If sourceText doesn't contains searchText or filterText
        // then return sourceText as is.
        if (!foundSearchText)
            return sourceText;

        // We know that either the search text or the filter text was found
        // at this point.
        // Get the index of the first one found.
        int index = searchIndex;

        String result = "<html>";

        while (index != -1) {
            int matchLength = searchText.length();
            String match = sourceText.substring(index, index + matchLength);
            String color = SEARCH_TEXT_COLOR;

            result += sourceText.substring(0, index);
            result += "<span style='color:" + color + "; font-weight:bold'>";
            result += match;
            result += "</span>";

            // Find the next occurrences.

            sourceText = sourceText.substring(index + matchLength);
            lowerText = lowerText.substring(index + matchLength);

            searchIndex = haveSearchText ? lowerText.indexOf(searchText) : -1;

            foundSearchText = searchIndex != -1;

            index = searchIndex;
        }

        result += sourceText; // tack on the remaining sourceText
        LOG.debugf("highlighted text is {0}", result);
        return result;
    }

    private PropertyMatch getPropertyMatch(VisualSearchResult vsr) {
        if(searchText == null)
            return null;
        
        // If any data displayed in the headingLabel or subheadingLabel
        // matches the search or filter criteria,
        // then the otherLabel isn't needed.
        // In this case, return an empty string.
        if (headingLabel.getText().contains(searchText))
            return null;
        if (subheadingLabel.getText().contains(searchText))
            return null;

        // Look for metadata that matches the search criteria.
        Map<PropertyKey, Object> props = vsr.getProperties();
        for (PropertyKey key : props.keySet()) {
            String value = vsr.getPropertyString(key);

            if (value.toLowerCase().contains(searchText)) {
                String betterKey = key.toString().toLowerCase();
                betterKey = betterKey.replace('_', ' ');
                return new PropertyMatch(betterKey, value);
            }
        }

        // No match found.
        return null;
    }

    public Object getCellEditorValue() {
        return vsr;
    }

    public Component getTableCellEditorComponent(
        final JTable table, Object value, boolean isSelected, int row, int column) {
        LOG.debugf("ListViewTableCellEditor.getTableCellEditorComponent: row = {0}", row);

        vsr = (VisualSearchResult) value;
        category = vsr.getCategory();

        similarButton.setVisible(getSimilarResultsCount() > 0);

        actionButtonPanel =
            (ActionButtonPanel) actionEditor.getTableCellEditorComponent(
                    table, value, isSelected, row, column);
        
        final JToggleButton junkButton = actionButtonPanel.getJunkButton();
        
        if (thePanel == null) {
            thePanel = makePanel();
            actionButtonPanel.configureForListView(table);
            itemIconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    actionButtonPanel.startDownload();
                    table.editingStopped(new ChangeEvent(table));
                }
            });
            
            junkButton.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    table.editingStopped(new ChangeEvent(table));
                }
            });
        }

        actionPanel.removeAll();
        actionPanel.add(actionButtonPanel);

        markAsJunk(junkButton.isSelected());

        populatePanel((VisualSearchResult) value);
        setBackground(isSelected);

        return thePanel;
    }

    private int getSimilarResultsCount() {
        return vsr == null ? 0 : vsr.getSimilarResults().size();
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        return getTableCellEditorComponent(
            table, value, isSelected, row, column);
    }
    
    private boolean isShowingSimilarResults() {
        return vsr != null && getSimilarResultsCount() > 0 && vsr.getSimilarResults().get(0).isVisible();
    }

    private Component makeCenterPanel() {
        similarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean toggleVisibility = !isShowingSimilarResults();
                vsr.setChildrenVisible(toggleVisibility);
                
                similarButton.setText(getHideShowSimilarFilesButtonText());
            }
        });

        JXPanel panel = new JXPanel(new MigLayout("insets 0 0 0 0", "0[left]0", "0[]0[]0")) {
            @Override
            public void setBackground(Color color) {
                super.setBackground(color);
                for (Component component : getComponents()) {
                    component.setBackground(color);
                }
            }
        };

        panel.setOpaque(false);
        panel.add(fromWidget, "wrap");
        panel.add(similarButton);

        return panel;
    }

    private Component makeLeftPanel() {
        itemIconLabel = new JLabel(itemIcon);
        itemIconLabel.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        itemIconLabel.setOpaque(false);


        JXPanel downloadPanel = new JXPanel(new MigLayout());
        downloadPanel.setOpaque(false);
        downloadPanel.add(itemIconLabel);

        JXPanel headingPanel = new JXPanel(new MigLayout("insets 0 0 0 0", "0[]0", "0[]0[]0[]0"));
        headingPanel.setOpaque(false);
        headingPanel.add(headingLabel, "wrap");
        headingPanel.add(subheadingLabel, "wrap");
        headingPanel.add(otherLabel);

        JXPanel panel = new JXPanel(new MigLayout("insets 0 0 0 0", "0[][]0", "0[]0"));

        panel.setOpaque(false);

        panel.add(downloadPanel);
        panel.add(headingPanel);

        return panel;
    }

    private JXPanel makePanel() {
        final JXPanel panel = new JXPanel(new MigLayout("insets 0 0 0 0", "0[]push[shrinkprio 50]0", "0[]0")) {

            @Override
            public void setBackground(Color bg) {
                super.setBackground(bg);
                if (actionButtonPanel != null) {
                    actionButtonPanel.setBackground(bg);
                }
            }
        };

        panel.add(makeIndentablePanel(makeLeftPanel()));

        JPanel rightColumn = new JPanel(new MigLayout("insets 0 0 0 0", "0[][]0", "0[]0"));
        rightColumn.setOpaque(false);
        rightColumn.add(makeCenterPanel(), "wmin 250, grow");

        actionPanel.setOpaque(false);
        rightColumn.add(actionPanel);
        panel.add(rightColumn);

        return panel;
    }

    private Component makeIndentablePanel(Component component) {
        JPanel wrapperPanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = LEFT_WIDTH;
                return size;
            }
        };
        wrapperPanel.setOpaque(false);
        similarResultIndentation = new JComponent(){};
        wrapperPanel.add(similarResultIndentation, BorderLayout.WEST);
        wrapperPanel.add(component, BorderLayout.CENTER);
        return wrapperPanel;
    }

    private void markAsJunk(boolean junk) {
        float opacity = junk ? 0.2f : 1.0f;
        thePanel.setAlpha(opacity);
    }

    private void populateFrom(VisualSearchResult vsr) {
        List<String> people = new ArrayList<String>();
        Collection<RemoteHost> sources = vsr.getSources();
        for (RemoteHost source : sources) {
            people.add(source.getHostDescription());
        }
        fromWidget.setPeople(people);
    }

    private void populateHeading(VisualSearchResult vsr) {
        String name = getProperty(vsr, PropertyKey.NAME);
        String heading;
        
        switch(category) {
        case AUDIO:
            heading = getProperty(vsr, PropertyKey.ARTIST_NAME) + " - " + name;
            break;
        case VIDEO:
        case IMAGE:
            heading = name;
            break;
        default:
            heading = name + "." + vsr.getFileExtension();
        }

        headingLabel.setText(highlightMatches(heading));
    }

    private String getProperty(VisualSearchResult vsr, PropertyKey key) {
        Object property = vsr.getProperty(key);
        return property == null ? "?" : property.toString();
    }

    private void populateOther(VisualSearchResult vsr) {
        PropertyMatch pm = getPropertyMatch(vsr);
        if (pm == null) {
            otherLabel.setText("");
        } else {
            String html = highlightMatches(pm.value);
            String tag = "<html>";
            // Insert the key, a colon and a space after the html start tag.
            html = tag + pm.key + ": " + html.substring(tag.length());
            otherLabel.setText(html);
        }
    }

    private void populatePanel(VisualSearchResult vsr) {
        if (vsr == null) return;
        
        Dimension indentSize = new Dimension(
                vsr.getSimilarityParent() != null ? SIMILARITY_INDENTATION : 0, 0);
        similarResultIndentation.setPreferredSize(indentSize);
        
        actionButtonPanel.setDownloadingDisplay(vsr);

        populateHeading(vsr);
        populateSubheading(vsr);
        populateOther(vsr);
        populateFrom(vsr);

        if (getSimilarResultsCount() > 0) {
            similarButton.setText(getHideShowSimilarFilesButtonText());
        }
    }

    private void populateSubheading(VisualSearchResult vsr) {
        String subheading;

        switch(category) {
        case AUDIO:
            Object albumTitle = vsr.getProperty(PropertyKey.ALBUM_TITLE);
            String prefix = albumTitle == null ? "" : albumTitle + " - ";
            subheading = prefix + vsr.getProperty(PropertyKey.QUALITY)
                + " - " + vsr.getProperty(PropertyKey.LENGTH);
            break;
        case VIDEO:
            subheading = vsr.getProperty(PropertyKey.QUALITY)
                + " - " + vsr.getProperty(PropertyKey.LENGTH);
            break;
        case IMAGE:
            Object time = vsr.getProperty(PropertyKey.DATE_CREATED);
            if(time == null) {
                subheading = "";
            } else {
                subheading = DATE_FORMAT.format(new java.util.Date((Long)time));
            }
            break;
        default:
            subheading = "{application name}"
                + " - " + vsr.getProperty(PropertyKey.FILE_SIZE) + "MB";
        }

        subheadingLabel.setText(highlightMatches(subheading));
    }

    private void setBackground(boolean isSelected) {
        if (thePanel == null) return;

        Color color = isSelected ? SELECTED_COLOR : Color.WHITE;
        thePanel.setBackground(color);

        int childCount = thePanel.getComponentCount();
        for (int i = 0; i < childCount; i++) {
            Component child = thePanel.getComponent(i);
            child.setBackground(color);
        }
    }

    private String getHideShowSimilarFilesButtonText() {
        int similarResultsCount = getSimilarResultsCount();
        return hyperlinkText(tr(isShowingSimilarResults() ? "Hide" : "Show"), 
                " ", similarResultsCount, " ", tr(similarResultsCount > 1 ? "similar files" : "similar file"));
    }

    static class PropertyMatch {
        String key;
        String value;
        PropertyMatch(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}