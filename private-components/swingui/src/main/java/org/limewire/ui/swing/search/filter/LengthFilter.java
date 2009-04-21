package org.limewire.ui.swing.search.filter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.RangeSlider;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Filter component to select search results according to their lengths.
 */
class LengthFilter extends AbstractFilter {
    /** Array of length options in seconds. */
    private static final long[] LENGTHS = {
        0, 
        10,
        30,
        60, 
        60 * 10, 
        60 * 30, 
        60 * 60,
        60 * 60 * 24
    };
    
    private final JPanel panel = new JPanel();
    private final JLabel lengthLabel = new JLabel();
    private final JLabel lengthValue = new JLabel();
    private final RangeSlider lengthSlider = new RangeSlider();
    
    private boolean resetAdjusting;

    /**
     * Constructs a LengthFilter.
     */
    public LengthFilter() {
        panel.setLayout(new MigLayout("insets 3 0 3 0, gap 0!", 
                "[left][left]",
                "[top][top]3[top]"));
        panel.setOpaque(false);

        lengthLabel.setFont(getHeaderFont());
        lengthLabel.setForeground(getHeaderColor());
        lengthLabel.setText(I18n.tr("Length"));
        
        lengthValue.setFont(getRowFont());
        lengthValue.setForeground(getRowColor());
        
        lengthSlider.setMinimum(0);
        lengthSlider.setMaximum(LENGTHS.length - 1);
        lengthSlider.setOpaque(false);
        lengthSlider.setRequestFocusEnabled(false);
        
        // Set initial values.
        lengthSlider.setValue(lengthSlider.getMinimum());
        lengthSlider.setUpperValue(lengthSlider.getMaximum());
        lengthValue.setText(createRangeText());

        // Add listener to update filter.
        lengthSlider.addChangeListener(new SliderListener());
        
        panel.add(lengthLabel , "wrap");
        panel.add(lengthValue , "wrap");
        panel.add(lengthSlider, "growx");
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }
    
    @Override
    public void reset() {
        // Reset slider values.
        resetAdjusting = true;
        lengthSlider.setValue(lengthSlider.getMinimum());
        lengthSlider.setUpperValue(lengthSlider.getMaximum());
        lengthValue.setText(createRangeText());
        resetAdjusting = false;
        
        // Deactivate filter.
        deactivate();
    }

    @Override
    public void dispose() {
        // Do nothing.
    }

    /**
     * Creates a text string describing the current length range.
     */
    private String createRangeText() {
        String minText = valueToText(lengthSlider.getValue());
        String maxText = valueToText(lengthSlider.getUpperValue());
        return I18n.tr("{0} to {1}", minText, maxText);
    }
    
    /**
     * Converts the specified slider value to its length text.
     */
    private String valueToText(int value) {
        // Return text for edge cases.
        if (value == lengthSlider.getMinimum()) {
            return CommonUtils.seconds2time(0);
        } else if (value == lengthSlider.getMaximum()) {
            return I18n.tr("max");
        }
        
        // Return text for value.
        return CommonUtils.seconds2time(LENGTHS[value]);
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
            
            // Update length display.
            RangeSlider slider = (RangeSlider) e.getSource();
            String rangeText = createRangeText();
            lengthValue.setText(rangeText);
            
            // When slider movement has ended, apply filter for selected length 
            // range.
            if (!slider.getValueIsAdjusting()) {
                if ((slider.getValue() > 0) || (slider.getUpperValue() < LENGTHS.length - 1)) {
                    // Get length range.
                    long minLength = LENGTHS[slider.getValue()];
                    long maxLength = LENGTHS[slider.getUpperValue()];

                    // Create new matcher and activate.
                    Matcher<VisualSearchResult> newMatcher = new LengthMatcher(minLength, maxLength);
                    activate(rangeText, newMatcher);
                    
                } else {
                    // Deactivate to clear matcher.
                    deactivate();
                }
                
                // Notify filter listeners.
                fireFilterChanged(LengthFilter.this);
            }
        }
    }    
}
