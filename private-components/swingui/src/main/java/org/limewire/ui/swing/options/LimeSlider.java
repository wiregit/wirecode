package org.limewire.ui.swing.options;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

/**
 * Basic component that provides the user with a slider for picking a percentage
 * of bandwidth to use. The slider lets the user select from 25-100% of their
 * bandwidth for use. It displays the expected bandwidth as a label after the
 * slider.
 */
public abstract class LimeSlider extends JComponent {
    private JSlider slider;

    private JLabel label;

    public LimeSlider(int minSlider, int maxSlider) {
        setOpaque(false);
        setLayout(new MigLayout("nogrid, fill"));
        label = new JLabel();
        slider = new JSlider(minSlider, maxSlider);
        slider.setOpaque(false);
        slider.setMajorTickSpacing(10);
        slider.addChangeListener(new SliderChangeListener(slider, label));
        add(slider);
        add(label);
    }

    public int getValue() {
        return slider.getValue();
    }

    public void setValue(int value) {
        slider.setValue(value);
    }
    
    public BoundedRangeModel getModel() {
        return slider.getModel();
    }

    /**
     * Changes the label for the slider based on the slider's current value.
     */
    private class SliderChangeListener implements ChangeListener {

        private JSlider slider;

        private JLabel label;

        public SliderChangeListener(JSlider slider, JLabel label) {
            this.slider = slider;
            this.label = label;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            int value = slider.getValue();
            label.setText(getMessage(value));
        }
    }

    /**
     * Returns a message for the slider based on the given value of the slider.
     */
    public abstract String getMessage(int value);
}
