package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.HyperlinkTextUtil.hyperlinkText;
import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.ui.swing.util.I18n.trn;

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
import java.util.Collections;
import java.util.Comparator;

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
import org.limewire.core.api.search.actions.FromActions;
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

    private static final String HTML = "<html>";
    private static final String GRAY_TEXT_COLOR_DIV = "<div color=\"rgb(166,166,166)\">";
    private static final String CLOSING_HTML_TAG = "</html>";
    private static final String CLOSING_DIV_HTML = "</div></html>";
    private static final int SIMILARITY_INDENTATION = 50;
    private final Log LOG = LogFactory.getLog(getClass());
    private final Color SELECTED_COLOR = Color.GREEN;
    private final PropertyKeyComparator AUDIO_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.GENRE, PropertyKey.BITRATE, PropertyKey.TRACK_NUMBER, PropertyKey.SAMPLE_RATE);
    private final PropertyKeyComparator VIDEO_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.YEAR, PropertyKey.RATING, PropertyKey.COMMENTS, PropertyKey.HEIGHT, 
                                  PropertyKey.WIDTH, PropertyKey.BITRATE);
    private final PropertyKeyComparator DOCUMENTS_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.DATE_CREATED, PropertyKey.AUTHOR);
    private final PropertyKeyComparator PROGRAMS_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.PLATFORM, PropertyKey.COMPANY);

    public static final int HEIGHT = 60;
    public static final int LEFT_WIDTH = 440;
    public static final int WIDTH = 740;

    private String searchText;

    @Resource private Icon itemIcon;
    
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");

    private ActionColumnTableCellEditor actionEditor;
    private ActionButtonPanel actionButtonPanel;
    private FromWidget fromWidget;
    private JLabel itemIconLabel;
    private JXHyperlink similarButton = new JXHyperlink();
    private JLabel headingLabel = new JLabel();
    private JLabel subheadingLabel = new JLabel();
    private JLabel otherLabel = new JLabel();
    private JXPanel actionPanel = new JXPanel();
    private JXPanel thePanel;

    private VisualSearchResult vsr;
    private JComponent similarResultIndentation;
    private JPanel indentablePanel;

    @Inject
    public ListViewTableCellEditor(
        ActionColumnTableCellEditor actionEditor, String searchText, FromActions fromActions) {

        this.actionEditor = actionEditor;
        this.searchText = searchText;
        FontUtils.changeSize(similarButton, -2.0F);

        GuiUtils.assignResources(this);
        fromWidget = new FromWidget(fromActions);
    }

    /**
     * Adds an HTML bold tag around every occurrence of highlightText.
     * Note that comparisons are case insensitive.
     * @param text the text to be modified
     * @return the text containing bold tags
     */
    private String highlightMatches(String sourceText) {
        boolean haveSearchText = searchText != null && searchText.length() > 0;

        // If there is no search or filter text then return sourceText as is.
        if (!haveSearchText)
            return sourceText;
        
        return SearchHighlightUtil.highlight(searchText, sourceText);
    }

    private PropertyMatch getPropertyMatch(VisualSearchResult vsr) {
        if(searchText == null)
            return null;
        
        ArrayList<PropertyKey> properties = new ArrayList<PropertyKey>(vsr.getProperties().keySet());
        Collections.sort(properties, getComparator());
        for (PropertyKey key : properties) {
            String value = vsr.getPropertyString(key);

            String propertyMatch = highlightMatches(value);
            if (value != null && isDifferentLength(value, propertyMatch)) {
                String betterKey = key.toString().toLowerCase();
                betterKey = betterKey.replace('_', ' ');
                return new PropertyMatch(betterKey, propertyMatch);
            }
        }

        // No match found.
        return null;
    }

    private Comparator<PropertyKey> getComparator() {
        switch (getCategory()) {
        case AUDIO:
            return AUDIO_COMPARATOR;
        case VIDEO:
            return VIDEO_COMPARATOR;
        case DOCUMENT:
            return DOCUMENTS_COMPARATOR;
        default:
            return PROGRAMS_COMPARATOR;
        }
    }
    
    private static class PropertyKeyComparator implements Comparator<PropertyKey> {
        private final PropertyKey[] keyOrder;

        public PropertyKeyComparator(PropertyKey... keys) {
            this.keyOrder = keys;
        }

        @Override
        public int compare(PropertyKey o1, PropertyKey o2) {
            if (o1 == o2) {
                return 0;
            }
            
            for(PropertyKey key : keyOrder) {
                if (o1 == key) {
                    return -1;
                } else if (o2 == key) {
                    return 1;
                }
            }
            return 0;
        }
    }

    public Object getCellEditorValue() {
        return vsr;
    }

    public Component getTableCellEditorComponent(
        final JTable table, Object value, boolean isSelected, int row, int column) {

        vsr = (VisualSearchResult) value;
        LOG.debugf("ListViewTableCellEditor.getTableCellEditorComponent: row = {0} urn: {1}", row, vsr.getCoreSearchResults().get(0).getUrn());

        similarButton.setVisible(getSimilarResultsCount() > 0);

        actionButtonPanel =
            (ActionButtonPanel) actionEditor.getTableCellEditorComponent(
                    table, value, isSelected, row, column);
        
        final JToggleButton junkButton = actionButtonPanel.getSpamButton();
        
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

        LOG.debugf("row: {0} shouldIndent: {1}", row, vsr.getSimilarityParent() != null);
        
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
        return vsr != null && getSimilarResultsCount() > 0 && vsr.isChildrenVisible();
    }

    private Component makeCenterPanel() {
        similarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean toggleVisibility = !isShowingSimilarResults();
                vsr.setChildrenVisible(toggleVisibility);
                
                similarButton.setText(getHideShowSimilarFilesButtonText());
            }
        });

        JXPanel panel = new JXPanel(new MigLayout("insets 0 0 0 0", "0[]0", "0[]0[]0")) {
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
        indentablePanel = new JPanel(new BorderLayout()) {
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
        indentablePanel.setOpaque(false);
        similarResultIndentation = new JComponent(){};
        similarResultIndentation.setPreferredSize(new Dimension(SIMILARITY_INDENTATION, 0));
        indentablePanel.add(component, BorderLayout.CENTER);
        return indentablePanel;
    }

    private void markAsJunk(boolean junk) {
        float opacity = junk ? 0.2f : 1.0f;
        thePanel.setAlpha(opacity);
    }

    private void populateFrom(VisualSearchResult vsr) {
        Collection<RemoteHost> sources = vsr.getSources();
        fromWidget.setPeople(new ArrayList<RemoteHost>(sources));
    }

    /**
     * Returns a value to indicate whether the heading was decorated to highlight
     * parts of the content that match search terms
     */
    private boolean populateHeading(VisualSearchResult vsr) {
        String name = getProperty(vsr, PropertyKey.NAME);
        String heading;
        
        switch(getCategory()) {
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

        String highlightMatches = highlightMatches(heading);
        LOG.debugf("Heading: {0} highlightedMatches: {1}", heading, highlightMatches);
        headingLabel.setText(highlightMatches);
        return isDifferentLength(heading, highlightMatches);
    }

    private String getProperty(VisualSearchResult vsr, PropertyKey key) {
        Object property = vsr.getProperty(key);
        return property == null ? "?" : property.toString();
    }

    private void populateOther(VisualSearchResult vsr, boolean shouldHidePropertyMatches) {
        otherLabel.setText("");
        if (shouldHidePropertyMatches) { 
            return;
        }
        
        PropertyMatch pm = getPropertyMatch(vsr);
        
        if (pm != null) {
            String html = pm.highlightedValue;
            String tag = HTML;
            // Insert the following: a div to color the text grey, the key, a colon and a space after the html start tag, then the closing tags.
            html = tag + GRAY_TEXT_COLOR_DIV + pm.key + ": " + 
                    html.substring(tag.length(), html.length() - CLOSING_HTML_TAG.length()) + CLOSING_DIV_HTML;
            otherLabel.setText(html);
        }
    }

    private void populatePanel(VisualSearchResult vsr) {
        if (vsr == null) return;
        
        if (vsr.getSimilarityParent() != null) {
            indentablePanel.add(similarResultIndentation, BorderLayout.WEST);
        } else {
            indentablePanel.remove(similarResultIndentation);
        }
        
        actionButtonPanel.setDownloadingDisplay(vsr);

        boolean headingDecorated = populateHeading(vsr);
        boolean subheadingDecorated = populateSubheading(vsr);
        populateOther(vsr, headingDecorated || subheadingDecorated);
        populateFrom(vsr);

        if (getSimilarResultsCount() > 0) {
            similarButton.setText(getHideShowSimilarFilesButtonText());
        }
    }

    /**
     * Returns a value to indicate whether the subheading was decorated to highlight
     * parts of the content that match search terms
     */
    private boolean populateSubheading(VisualSearchResult vsr) {
        String subheading;

        switch(getCategory()) {
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
                + " - " + vsr.getProperty(PropertyKey.FILE_SIZE) + tr("MB");
        }

        String highlightMatches = highlightMatches(subheading);
        LOG.debugf("Subheading: {0} highlightedMatches: {1}", subheading, highlightMatches);
        subheadingLabel.setText(highlightMatches);
        return isDifferentLength(subheading, highlightMatches);
    }

    private boolean isDifferentLength(String str1, String str2) {
        return str1 != null && str2 != null && str1.length() != str2.length();
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
        if (isShowingSimilarResults()) {
            return hyperlinkText(trn("Hide 1 similar file", "Hide {0} similar files", similarResultsCount));
        }
        return hyperlinkText(trn("Show 1 similar file", "Show {0} similar files", similarResultsCount));
    }

    private Category getCategory() {
        return vsr.getCategory();
    }

    private static class PropertyMatch {
        private final String key;
        private final String highlightedValue;
        PropertyMatch(String key, String highlightedProperty) {
            this.key = key;
            this.highlightedValue = highlightedProperty;
        }
    }
}