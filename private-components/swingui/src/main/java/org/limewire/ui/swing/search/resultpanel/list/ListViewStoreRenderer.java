package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer.NoDancingHtmlLabel;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/**
 * Renderer component for store results.
 */
class ListViewStoreRenderer extends JXPanel {

    @Resource(key="ListViewTableEditorRenderer.headingColor") private String headingColor;
    @Resource(key="ListViewTableEditorRenderer.headingFont") private Font headingFont;
    @Resource(key="ListViewTableEditorRenderer.subHeadingLabelColor") private Color subHeadingLabelColor;
    @Resource(key="ListViewTableEditorRenderer.subHeadingFont") private Font subHeadingFont;
    @Resource(key="ListViewTableEditorRenderer.metadataLabelColor") private Color metadataLabelColor;
    @Resource(key="ListViewTableEditorRenderer.metadataFont") private Font metadataFont;
    
    private final CategoryIconManager categoryIconManager;
    
    private JButton itemIconButton = new IconButton();
    private JXPanel searchResultTextPanel = new JXPanel();
    private JEditorPane headingPane = new JEditorPane();
    private JLabel subheadingLabel = new NoDancingHtmlLabel();
    private JLabel metadataLabel = new NoDancingHtmlLabel();

    private JXPanel trackResultPanel = new JXPanel();

    /**
     * Constructs a ListViewStoreRenderer.
     */
    @Inject
    public ListViewStoreRenderer(
            CategoryIconManager categoryIconManager) {
        this.categoryIconManager = categoryIconManager;
        
        GuiUtils.assignResources(this);
        
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        itemIconButton.setBorderPainted(true);
        itemIconButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        searchResultTextPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        headingPane.setContentType("text/html");
        headingPane.setEditable(false);
        headingPane.setCaretPosition(0);
        headingPane.setSelectionColor(HTMLLabel.TRANSPARENT_COLOR);       
        headingPane.setOpaque(false);
        headingPane.setFocusable(false);
        StyleSheet mainStyle = ((HTMLDocument)headingPane.getDocument()).getStyleSheet();
        String rules = "body { font-family: " + headingFont.getFamily() + "; }" +
                ".title { color: " + headingColor + "; font-size: " + headingFont.getSize() + "; }" +
                "a { color: " + headingColor + "; }";
        StyleSheet newStyle = new StyleSheet();
        newStyle.addRule(rules);
        mainStyle.addStyleSheet(newStyle); 
        headingPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        subheadingLabel.setForeground(subHeadingLabelColor);
        subheadingLabel.setFont(subHeadingFont);

        metadataLabel.setForeground(metadataLabelColor);
        metadataLabel.setFont(metadataFont);
        
        trackResultPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    }
    
    private void layoutComponents() {
        setBackground(Color.LIGHT_GRAY);
        setOpaque(true); // TODO does not work
        setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        
        searchResultTextPanel.setLayout(new MigLayout("nogrid, ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        searchResultTextPanel.setOpaque(false);
        
        searchResultTextPanel.add(headingPane, "left, shrinkprio 200, growx, wmax pref, hidemode 3, wrap");
        searchResultTextPanel.add(subheadingLabel, "left, shrinkprio 200, growx, hidemode 3, wrap");
        searchResultTextPanel.add(metadataLabel, "left, shrinkprio 200, hidemode 3");
        
        trackResultPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, fill, novisualpadding"));

        //add(similarResultIndentation, "growy, hidemode 3, shrinkprio 0");
        add(itemIconButton, "alignx left, aligny 50%, gapleft 4, shrinkprio 0, growprio 0");
        add(searchResultTextPanel, "alignx left, aligny 50%, gapleft 4, growx, shrinkprio 200, growprio 200, pushx 200, wrap");
        //add(downloadSourceCount, "gapbottom 3, gapright 2, shrinkprio 0");
        //add(new JLabel(dividerIcon), "shrinkprio 0");
        //TODO: better number for wmin
        //add(fromWidget, "wmin 90, left, shrinkprio 0");
        //Tweaked the width of the icon because display gets clipped otherwise
        //add(similarButton, "gapright 4, hidemode 0, hmax 25, wmax 27, shrinkprio 0");
        //add(propertiesButton, "gapright 4, hmax 25, wmax 27, shrinkprio 0");
        
        add(trackResultPanel, "span 2, left, aligny top, gap 16 16 6 0, grow");
    }

    public void update(VisualSearchResult vsr, RowDisplayResult result) {
        System.out.println("storeRenderer update...");
        
        itemIconButton.setIcon(categoryIconManager.getIcon(vsr));
        
        updateLabelVisibility(result.getConfig());
        updateHeading(result);
        updateSubHeading(result);
        updateMetaData(result);
        updateTracks(vsr);
    }
    
    private void updateLabelVisibility(RowDisplayConfig config) {
        switch(config) {
        case HeadingOnly:
            subheadingLabel.setVisible(false);
            metadataLabel.setVisible(false);
            break;
        case HeadingAndSubheading:
            subheadingLabel.setVisible(true);
            metadataLabel.setVisible(false);
            break;
        case HeadingAndMetadata:
            subheadingLabel.setVisible(false);
            metadataLabel.setVisible(true);
            break;
        case HeadingSubHeadingAndMetadata:
        default:
            subheadingLabel.setVisible(true);
            metadataLabel.setVisible(true);
            break;
        }
    }
    
    private void updateHeading(RowDisplayResult result) {
        headingPane.setText(result.getHeading());
    }
    
    private void updateSubHeading(RowDisplayResult result) {
        subheadingLabel.setText(result.getSubheading());
    }
    
    private void updateMetaData(RowDisplayResult result) {
        
    }
    
    private void updateTracks(VisualSearchResult vsr) {
        trackResultPanel.removeAll();
        
        if (vsr.isStore() && (vsr.getStoreResult().getFileList().size() > 0)) {
            List<SearchResult> fileList = vsr.getStoreResult().getFileList();
            for (SearchResult result : fileList) {
                Component comp = createTrackComponent(result);
                trackResultPanel.add(comp, "align left, growx, wrap");
            }
        }
    }
    
    private Component createTrackComponent(SearchResult result) {
        JLabel label = new JLabel();
        
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        label.setForeground(subHeadingLabelColor);
        label.setFont(subHeadingFont);
        label.setText((String) result.getProperty(FilePropertyKey.NAME));
        System.out.println("prefSize=" + label.getPreferredSize());
        
        return label;
    }
}
