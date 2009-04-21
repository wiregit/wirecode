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

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Filter component to select search results according to their bit rates.
 */
class BitRateFilter extends AbstractFilter {
    /** Array of bit rate options. */
    private static final long[] RATES = {
        0, 
        64,
        96,
        128, 
        160,
        192, 
        256
    };
    
    private final JPanel panel = new JPanel();
    private final JLabel rateLabel = new JLabel();
    private final JLabel rateValue = new JLabel();
    private final RangeSlider rateSlider = new RangeSlider();
    
    private boolean resetAdjusting;

    /**
     * Constructs a BitRateFilter.
     */
    public BitRateFilter() {
        panel.setLayout(new MigLayout("insets 3 0 3 0, gap 0!", 
                "[left][left]",
                "[top][top]3[top]"));
        panel.setOpaque(false);

        rateLabel.setFont(getHeaderFont());
        rateLabel.setForeground(getHeaderColor());
        rateLabel.setText(I18n.tr("Bitrate"));
        
        rateValue.setFont(getRowFont());
        rateValue.setForeground(getRowColor());
        
        rateSlider.setMinimum(0);
        rateSlider.setMaximum(RATES.length - 1);
        rateSlider.setOpaque(false);
        rateSlider.setRequestFocusEnabled(false);
        rateSlider.setUpperThumbEnabled(false);
        
        // Set initial values.
        rateSlider.setValue(rateSlider.getMinimum());
        rateValue.setText(createRangeText());

        // Add listener to update filter.
        rateSlider.addChangeListener(new SliderListener());
        
        panel.add(rateLabel , "wrap");
        panel.add(rateValue , "wrap");
        panel.add(rateSlider, "growx");
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        // Reset slider values.
        resetAdjusting = true;
        rateSlider.setValue(rateSlider.getMinimum());
        rateValue.setText(createRangeText());
        resetAdjusting = false;
        
        // Deactivate filter.
        deactivate();
    }

    @Override
    public void dispose() {
        // Do nothing.
    }

    /**
     * Creates a text string describing the current bit rate range.
     */
    private String createRangeText() {
        int value = rateSlider.getValue();
        if (value == rateSlider.getMinimum()) {
            return I18n.tr("all"); 
        } else {
            String text = String.valueOf(RATES[value]);
            return I18n.tr("{0} or above", text);
        }
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
            
            // Update value display.
            RangeSlider slider = (RangeSlider) e.getSource();
            String rangeText = createRangeText();
            rateValue.setText(rangeText);
            
            // When slider movement has ended, apply filter for selected value.
            if (!slider.getValueIsAdjusting()) {
                if (slider.getValue() > 0) {
                    // Get bit rate.
                    long bitRate = RATES[slider.getValue()];

                    // Create new matcher and activate.
                    Matcher<VisualSearchResult> newMatcher = new BitRateMatcher(bitRate);
                    activate(rangeText, newMatcher);
                    
                } else {
                    // Deactivate to clear matcher.
                    deactivate();
                }
                
                // Notify filter listeners.
                fireFilterChanged(BitRateFilter.this);
            }
        }
    }
}
