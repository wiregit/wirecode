package org.limewire.ui.swing.options;

import java.text.NumberFormat;

/**
 * Basic component that provides the user with a slider for picking a percentage
 * of bandwidth to use. The slider lets the user select from 25-100% of their
 * bandwidth for use. It displays the expected bandwidth as a label after the
 * slider.
 */
public class SeedRatioSlider extends LimeSlider {

    public static final int DEAFULT_SLIDER = 2;

    public static final int MIN_SLIDER = 1;

    public static final int MAX_SLIDER = 10;

    public SeedRatioSlider() {
        super(MIN_SLIDER * 10, MAX_SLIDER * 10);
    }

    @Override
    public String getMessage(int value) {
        Float f = new Float(value / (float) 10);
        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setMaximumFractionDigits(2);
        String labelText = String.valueOf(formatter.format(f));
        return labelText;
    }
}
