package org.limewire.ui.swing.search.filter;

import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.RangeSlider;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Filter component to select search results according to their file sizes.
 */
class FileSizeFilter extends AbstractFilter {
    /** Array of size options in bytes. */
    private static final long[] SIZES = {
        0, 
        512,
        1024, 
        1024 * 512,
        1024 * 1024, 
        1024 * 1024 * 512,
        1024 * 1024 * 1024, 
        (long) 1024 * 1024 * 1024 * 512,  
        (long) 1024 * 1024 * 1024 * 1024
    };
    
    private final JPanel panel = new JPanel();
    private final JLabel sizeLabel = new JLabel();
    private final JLabel sizeValue = new JLabel();
    private final RangeSlider sizeSlider = new RangeSlider();
    
    private boolean resetAdjusting;

    /**
     * Constructs a FileSizeFilter.
     */
    public FileSizeFilter() {
        panel.setLayout(new MigLayout("insets 1 1 0 0", 
                "[left]3[left]",
                "[top]3[top]"));
        panel.setOpaque(false);

        sizeLabel.setText(I18n.tr("Size:"));
        sizeLabel.setFont(sizeLabel.getFont().deriveFont(Font.BOLD));
        
        sizeSlider.setMinimum(0);
        sizeSlider.setMaximum(SIZES.length - 1);
        sizeSlider.setOpaque(false);
        sizeSlider.setRequestFocusEnabled(false);
        
        // Set initial values.
        sizeSlider.setValue(sizeSlider.getMinimum());
        sizeSlider.setUpperValue(sizeSlider.getMaximum());
        sizeValue.setText(createRangeText());

        // Add listener to update filter.
        sizeSlider.addChangeListener(new SliderListener());
        
        panel.add(sizeLabel , "");
        panel.add(sizeValue , "wrap");
        panel.add(sizeSlider, "spanx 2, growx");
    }
    
    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        // Reset slider values.
        resetAdjusting = true;
        sizeSlider.setValue(sizeSlider.getMinimum());
        sizeSlider.setUpperValue(sizeSlider.getMaximum());
        sizeValue.setText(createRangeText());
        resetAdjusting = false;
        
        // Deactivate filter.
        deactivate();
    }
    
    @Override
    public void dispose() {
        // Do nothing.
    }

    /**
     * Creates a text string describing the current size range.
     */
    private String createRangeText() {
        String minText = valueToText(sizeSlider.getValue());
        String maxText = valueToText(sizeSlider.getUpperValue());
        return I18n.tr("{0} to {1}", minText, maxText);
    }
    
    /**
     * Converts the specified slider value to its size text.
     */
    private String valueToText(int value) {
        // Return text for edge cases.
        if (value == sizeSlider.getMinimum()) {
            return GuiUtils.toUnitbytes(0);
        } else if (value == sizeSlider.getMaximum()) {
            return I18n.tr("max");
        }
        
        // Return text for value.
        return GuiUtils.toUnitbytes(SIZES[value]);
    }
    
    /**
     * Listener to handle slider change events.
     */
    private class SliderListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            // Skip event if filter is being reset.
            if (resetAdjusting) {
                return;
            }
            
            // Update size display.
            RangeSlider slider = (RangeSlider) e.getSource();
            String rangeText = createRangeText();
            sizeValue.setText(rangeText);
            
            // When slider movement has ended, apply filter for selected size 
            // range.
            if (!slider.getValueIsAdjusting()) {
                if ((slider.getValue() > 0) || (slider.getUpperValue() < SIZES.length - 1)) {
                    // Get size range.
                    long minSize = SIZES[slider.getValue()];
                    long maxSize = SIZES[slider.getUpperValue()];

                    // Create new matcher and activate.
                    Matcher<VisualSearchResult> newMatcher = new FileSizeMatcher(minSize, maxSize);
                    activate(rangeText, newMatcher);
                    
                } else {
                    // Deactivate to clear matcher.
                    deactivate();
                }
                
                // Notify filter listeners.
                fireFilterChanged(FileSizeFilter.this);
            }
        }
    }
}
