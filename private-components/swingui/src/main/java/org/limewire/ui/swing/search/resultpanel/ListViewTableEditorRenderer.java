package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.HyperlinkTextUtil.hyperlinkText;
import static org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig.HeadingAndMetadata;
import static org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig.HeadingSubHeadingAndMetadata;
import static org.limewire.ui.swing.util.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.PropertyMatch;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.table.RowColorResolver;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ListViewTableEditorRenderer
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {

    private final CategoryIconManager categoryIconManager;
    
    private static final String HTML = "<html>";
    private static final String CLOSING_HTML_TAG = "</html>";
    private final Log LOG = LogFactory.getLog(getClass());

    private final String searchText;
    @Resource private Icon similarResultsIcon;
    @Resource private Color headingLabelColor;
    @Resource private Color subHeadingLabelColor;
    @Resource private Color metadataLabelColor;
    @Resource private Color similarResultsBackgroundColor;
    @Resource private Font headingFont;
    @Resource private Font subHeadingFont;
    @Resource private Font metadataFont;
    @Resource private Font similarResultsButtonFont;
    @Resource private Icon spamIcon;
    @Resource private Icon downloadingIcon;
    
    private final ActionColumnTableCellEditor actionEditor;
    private final SearchHeadingDocumentBuilder headingBuilder;
    private final ListViewRowHeightRule rowHeightRule;
    private ActionButtonPanel actionButtonPanel;
    private SearchResultFromWidget fromWidget;
    private JLabel itemIconLabel;
    private JXHyperlink similarButton = new JXHyperlink();
    private JEditorPane heading = new JEditorPane();
    private JLabel subheadingLabel = new JLabel();
    private JLabel metadataLabel = new JLabel();
    private JXPanel rightPanel = new JXPanel();
    private JXPanel editorComponent;

    private VisualSearchResult vsr;
    private JTable table;
    private JComponent similarResultIndentation;
    private JPanel indentablePanel;
    private JPanel leftPanel;
    private JPanel centerPanel;
    private JXPanel searchResultTextPanel;

    //TODO - Remove RowColorResolver param if uniform background color change is made permanent
    @AssistedInject
    ListViewTableEditorRenderer(
            CategoryIconManager categoryIconManager,
            SearchResultFromWidgetFactory fromWidgetFactory,
        @Assisted ActionColumnTableCellEditor actionEditor, 
        @Assisted String searchText, 
        @Assisted RemoteHostActions remoteHostActions, 
        @Assisted Navigator navigator, 
        @Assisted RowColorResolver<VisualSearchResult> colorResolver,
        SearchHeadingDocumentBuilder headingBuilder,
        ListViewRowHeightRule rowHeightRule) {

        this.categoryIconManager = categoryIconManager;
        
        this.actionEditor = actionEditor;
        this.searchText = searchText;
        this.headingBuilder = headingBuilder;
        this.rowHeightRule = rowHeightRule;
        
        GuiUtils.assignResources(this);

        similarButton.setFont(similarResultsButtonFont);
        
        fromWidget = fromWidgetFactory.create(remoteHostActions);
        
        makePanel(navigator);
    }

    public Object getCellEditorValue() {
        return vsr;
    }

    public Component getTableCellEditorComponent(
        final JTable table, Object value, boolean isSelected, int row, final int col) {

        vsr = (VisualSearchResult) value;
        this.table = table;
        LOG.debugf("getTableCellEditorComponent: row = {0} column = {1}", row, col);

        actionButtonPanel =
            (ActionButtonPanel) actionEditor.getTableCellEditorComponent(
                    table, value, isSelected, row, col);
        
        if (editorComponent == null) {
            editorComponent = new JXPanel(new MigLayout("insets 0 0 0 0", "0[]0", "0[]0"));
            
            itemIconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    actionButtonPanel.startDownload();
                    table.editingStopped(new ChangeEvent(table));
                }
            });
        }

        LOG.debugf("row: {0} shouldIndent: {1}", row, vsr.getSimilarityParent() != null);
        
        populatePanel((VisualSearchResult) value, col);
        
        editorComponent.removeAll();

        JPanel panel = col == 0 ? leftPanel : col == 1 ? centerPanel : rightPanel;
        
        editorComponent.add(panel, "height 100%");

        return editorComponent;
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

    private JPanel makeCenterPanel() {
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

    private Component makeLeftPanel(final Navigator navigator) {
        itemIconLabel = new JLabel();
        itemIconLabel.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        itemIconLabel.setOpaque(false);
        
        heading.setContentType("text/html");
        heading.setEditable(false);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        heading.setOpaque(false);
        
        heading.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                populateHeading(rowHeightRule.getDisplayResult(vsr, searchText), vsr.getDownloadState(), true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                populateHeading(rowHeightRule.getDisplayResult(vsr, searchText), vsr.getDownloadState(), false);
            }
        });

        subheadingLabel.setForeground(subHeadingLabelColor);
        subheadingLabel.setFont(subHeadingFont);
        
        metadataLabel.setForeground(metadataLabelColor);
        metadataLabel.setFont(metadataFont);
        
        JXPanel downloadPanel = new JXPanel(new MigLayout("insets 7 0 0 5", "0[]", "0[top]0"));
        downloadPanel.setOpaque(false);
        downloadPanel.add(itemIconLabel);

        searchResultTextPanel = new JXPanel(new MigLayout("insets 0 0 0 0", "3[]", "5[]0[]0[]0"));
        searchResultTextPanel.setOpaque(false);
        searchResultTextPanel.add(heading, "wrap");
        searchResultTextPanel.add(subheadingLabel, "wrap, wmin 350");
        searchResultTextPanel.add(metadataLabel, "wmin 350");

        JXPanel panel = new JXPanel(new MigLayout("insets 0 0 0 0", "5[][]0", "0[]0"));

        panel.setOpaque(false);

        panel.add(downloadPanel);
        panel.add(searchResultTextPanel);
        
        heading.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (EventType.ACTIVATED == e.getEventType()) {
                    if (e.getDescription().equals("#download")) {
                        actionButtonPanel.startDownload();
                        table.editingStopped(new ChangeEvent(table));
                    } else if (e.getDescription().equals("#downloading")) {
                        navigator.getNavItem(
                            NavCategory.DOWNLOAD,
                            MainDownloadPanel.NAME).select(vsr);
                    } else if (e.getDescription().equals("#library")) {
                        navigator.getNavItem(
                            NavCategory.LIBRARY,
                            LibraryNavigator.NAME_PREFIX + vsr.getCategory()).select(vsr);
                        
                    }
                }
            }
        });

        return panel;
    }

    private void makePanel(Navigator navigator) {
        leftPanel = makeIndentablePanel(makeLeftPanel(navigator));

        centerPanel = makeCenterPanel();

        rightPanel.setOpaque(false);
    }

    private JPanel makeIndentablePanel(Component component) {
        indentablePanel = new JPanel(new MigLayout("insets 0 0 0 5", "[][]", "[]"));
        indentablePanel.setOpaque(false);
        similarResultIndentation = new JPanel(new BorderLayout());
        similarResultIndentation.add(new JLabel(similarResultsIcon), BorderLayout.CENTER);
        indentablePanel.add(component, "cell 1 0, top, left");
        return indentablePanel;
    }

    private void populateFrom(VisualSearchResult vsr) {
        Collection<RemoteHost> sources = vsr.getSources();
        fromWidget.setPeople(new ArrayList<RemoteHost>(sources));
    }

    private void populateHeading(RowDisplayResult result, BasicDownloadState downloadState, boolean isMouseOver) {
        this.heading.setText(headingBuilder.getHeadingDocument(result.getHeading(), downloadState, isMouseOver, result.isSpam()));
    }
    
    private void populateOther(RowDisplayResult result) {
        metadataLabel.setText("");
        RowDisplayConfig config = result.getConfig();
        if (config != HeadingSubHeadingAndMetadata && config != RowDisplayConfig.HeadingAndMetadata) { 
            return;
        }
        
        PropertyMatch pm = result.getMetadata();
        
        if (pm != null) {
            String html = pm.getHighlightedValue();
            // Insert the following: the key, a colon and a space after the html start tag, then the closing tags.
            html = html.replace(HTML, "").replace(CLOSING_HTML_TAG, "");
            html = HTML + pm.getKey() + ":" + html + CLOSING_HTML_TAG;
            metadataLabel.setText(html);
        }
    }

    private void populatePanel(VisualSearchResult vsr, int column) {
        if (vsr == null) return;
        
        if (column == 0) {
            if (vsr.getSimilarityParent() != null) {
                indentablePanel.add(similarResultIndentation, "cell 0 0, height 100%");
                similarResultIndentation.setBackground(similarResultsBackgroundColor);
            } else {
                indentablePanel.remove(similarResultIndentation);
            }
            
            itemIconLabel.setIcon(getIcon(vsr));

            RowDisplayResult result = rowHeightRule.getDisplayResult(vsr, searchText);
            
            populateHeading(result, vsr.getDownloadState(), false);
            
            populateSearchResultTextPanel(result.getConfig());

            populateSubheading(result);
            populateOther(result);
            
            
        } else if (column == 1) {
            similarButton.setVisible(getSimilarResultsCount() > 0);
            populateFrom(vsr);
            
            if (getSimilarResultsCount() > 0) {
                similarButton.setText(getHideShowSimilarFilesButtonText());
            }
            
        } 
    }

    private Icon getIcon(VisualSearchResult vsr) {
        if (vsr.isSpam()) {
            return spamIcon;
        } else if (vsr.getDownloadState() == BasicDownloadState.DOWNLOADING) {
            return downloadingIcon;
        }
        return categoryIconManager.getIcon(vsr.getCategory());
    }

    private void populateSearchResultTextPanel(RowDisplayConfig config) {
        switch(config) {
        case HeadingOnly:
            searchResultTextPanel.remove(subheadingLabel);
            searchResultTextPanel.remove(metadataLabel);
            break;
        case HeadingAndSubheading:
            searchResultTextPanel.add(subheadingLabel, "cell 0 1, wmin 350");
            searchResultTextPanel.remove(metadataLabel);
            break;
        case HeadingAndMetadata:
            searchResultTextPanel.remove(subheadingLabel);
            searchResultTextPanel.add(metadataLabel, "cell 0 1, wmin 350");
            break;
        case HeadingSubHeadingAndMetadata:
            searchResultTextPanel.add(subheadingLabel, "cell 0 1, wmin 350, wrap");
            searchResultTextPanel.add(metadataLabel, "cell 0 2, wmin 350");
        }
    }

    /**
     * Returns a value to indicate whether the subheading was decorated to highlight
     * parts of the content that match search terms
     */
    private void populateSubheading(RowDisplayResult result) {
        subheadingLabel.setText(result.getSubheading());
    }

    private String getHideShowSimilarFilesButtonText() {
        int similarResultsCount = getSimilarResultsCount();
        if (isShowingSimilarResults()) {
            return hyperlinkText(trn("Hide 1 similar file", "Hide {0} similar files", similarResultsCount));
        }
        return hyperlinkText(trn("Show 1 similar file", "Show {0} similar files", similarResultsCount));
    }
}