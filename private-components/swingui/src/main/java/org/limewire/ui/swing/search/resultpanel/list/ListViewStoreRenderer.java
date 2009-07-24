package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer.NoDancingHtmlLabel;
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
    
    private JXPanel searchResultTextPanel = new JXPanel();
    private JEditorPane heading = new JEditorPane();
    private JLabel subheadingLabel = new NoDancingHtmlLabel();
    private JLabel metadataLabel = new NoDancingHtmlLabel();
    
    /**
     * Constructs a ListViewStoreRenderer.
     */
    @Inject
    public ListViewStoreRenderer() {
        
        GuiUtils.assignResources(this);
        
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        heading.setContentType("text/html");
        heading.setEditable(false);
        heading.setCaretPosition(0);
        heading.setSelectionColor(HTMLLabel.TRANSPARENT_COLOR);       
        heading.setOpaque(false);
        heading.setFocusable(false);
        StyleSheet mainStyle = ((HTMLDocument)heading.getDocument()).getStyleSheet();
        String rules = "body { font-family: " + headingFont.getFamily() + "; }" +
                ".title { color: " + headingColor + "; font-size: " + headingFont.getSize() + "; }" +
                "a { color: " + headingColor + "; }";
        StyleSheet newStyle = new StyleSheet();
        newStyle.addRule(rules);
        mainStyle.addStyleSheet(newStyle); 
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        subheadingLabel.setForeground(subHeadingLabelColor);
        subheadingLabel.setFont(subHeadingFont);

        metadataLabel.setForeground(metadataLabelColor);
        metadataLabel.setFont(metadataFont);
    }
    
    private void layoutComponents() {
        searchResultTextPanel.setLayout(new MigLayout("nogrid, ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        searchResultTextPanel.setOpaque(false);
        searchResultTextPanel.add(heading, "left, shrinkprio 200, growx, wmax pref, hidemode 3, wrap");
        searchResultTextPanel.add(subheadingLabel, "left, shrinkprio 200, growx, hidemode 3, wrap");
        searchResultTextPanel.add(metadataLabel, "left, shrinkprio 200, hidemode 3");

        setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        //add(similarResultIndentation, "growy, hidemode 3, shrinkprio 0");
        //add(itemIconButton, "left, aligny 50%, gapleft 4, shrinkprio 0");
        add(searchResultTextPanel, "left, , aligny 50%, gapleft 4, growx, shrinkprio 200, growprio 200, push");
        //add(downloadSourceCount, "gapbottom 3, gapright 2, shrinkprio 0");
        //add(new JLabel(dividerIcon), "shrinkprio 0");
        //TODO: better number for wmin
        //add(fromWidget, "wmin 90, left, shrinkprio 0");
        //Tweaked the width of the icon because display gets clipped otherwise
        //add(similarButton, "gapright 4, hidemode 0, hmax 25, wmax 27, shrinkprio 0");
        //add(propertiesButton, "gapright 4, hmax 25, wmax 27, shrinkprio 0");
    }

    public void update(VisualSearchResult vsr, RowDisplayResult result) {
        System.out.println("storeRenderer update...");
        
        updateLabelVisibility(result.getConfig());
        updateHeading(result);
        updateSubHeading(result);
        updateMetaData(result);
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
            subheadingLabel.setVisible(true);
            metadataLabel.setVisible(true);
        }
    }
    
    private void updateHeading(RowDisplayResult result) {
        heading.setText(result.getHeading());
    }
    
    private void updateSubHeading(RowDisplayResult result) {
        subheadingLabel.setText(result.getSubheading());
    }
    
    private void updateMetaData(RowDisplayResult result) {
        
    }
}
