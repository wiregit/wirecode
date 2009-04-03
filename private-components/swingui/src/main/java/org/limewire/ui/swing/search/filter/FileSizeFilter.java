package org.limewire.ui.swing.search.filter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Filter component to select search results according to their file sizes.
 */
class FileSizeFilter extends AbstractFilter {
    
    private final JPanel panel = new JPanel();
    private final JLabel minSizeLabel = new JLabel();
    private final JLabel maxSizeLabel = new JLabel();
    private final JSlider sizeSlider = new JSlider();

    /**
     * Constructs a FileSizeFilter.
     */
    public FileSizeFilter() {
        panel.setLayout(new MigLayout("insets 1 1 0 0", 
                "[grow]", "[top]3[top]3[top,grow]"));

        minSizeLabel.setText("Min file size: ");
        maxSizeLabel.setText("Max file size: ");
        
        panel.add(minSizeLabel, "growx, wrap");
        panel.add(maxSizeLabel, "growx, wrap");
        panel.add(sizeSlider  , "growx");
    }
    
    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public MatcherEditor<VisualSearchResult> getMatcherEditor() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void dispose() {
     // TODO Auto-generated method stub
    }

}
