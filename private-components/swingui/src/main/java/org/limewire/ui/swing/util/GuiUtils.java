package org.limewire.ui.swing.util;

import java.text.NumberFormat;

import org.jdesktop.application.Application;
import org.limewire.ui.swing.mainframe.AppFrame;


public class GuiUtils {

    /**
     * Localizable Number Format constant for the current default locale
     * set at init time.
     */
    private static NumberFormat NUMBER_FORMAT0; // localized "#,##0"
    private static NumberFormat NUMBER_FORMAT1; // localized "#,##0.0"
    
      
    /**
     * Localizable constants
     */
    
    public static String GENERAL_UNIT_BYTES;
    public static String GENERAL_UNIT_KILOBYTES;
    public static String GENERAL_UNIT_MEGABYTES;
    public static String GENERAL_UNIT_GIGABYTES;
    public static String GENERAL_UNIT_TERABYTES;
    /* ambiguous name: means kilobytes/second, not kilobits/second! */
    public static String GENERAL_UNIT_KBPSEC;    
   

    static {       
        resetLocale();
    }
    
    static void resetLocale() {
        NUMBER_FORMAT0 = NumberFormat.getNumberInstance();
        NUMBER_FORMAT0.setMaximumFractionDigits(0);
        NUMBER_FORMAT0.setMinimumFractionDigits(0);
        NUMBER_FORMAT0.setGroupingUsed(true);
        
        NUMBER_FORMAT1 = NumberFormat.getNumberInstance();
        NUMBER_FORMAT1.setMaximumFractionDigits(1);
        NUMBER_FORMAT1.setMinimumFractionDigits(1);
        NUMBER_FORMAT1.setGroupingUsed(true);
               
        GENERAL_UNIT_BYTES =
            I18n.tr("B");
        GENERAL_UNIT_KILOBYTES =
            I18n.tr("KB");
        GENERAL_UNIT_MEGABYTES =
            I18n.tr("MB");
        GENERAL_UNIT_GIGABYTES =
            I18n.tr("GB");
        GENERAL_UNIT_TERABYTES =
            I18n.tr("TB");
        GENERAL_UNIT_KBPSEC =
            I18n.tr("KB/s");
    }
    
    /**
     * This static method converts the passed in number
     * into a localizable representation of an integer, with
     * digit grouping using locale dependant separators.
     *
     * @param value the number to convert to a numeric String.
     *
     * @return a localized String representing the integer value
     */
    public static String toLocalizedInteger(long value) {
        return NUMBER_FORMAT0.format(value);
    }
    
    /**
     * This static method converts the passed in number of bytes into a
     * kilobyte string grouping digits with locale-dependant thousand separator
     * and with "KB" locale-dependant unit at the end.
     *
     * @param bytes the number of bytes to convert to a kilobyte String.
     *
     * @return a String representing the number of kilobytes that the
     *         <code>bytes</code> argument evaluates to, with "KB" appended
     *         at the end.  If the input value is negative, the string
     *         returned will be "? KB".
     */
    public static String toKilobytes(long bytes) {
        if (bytes < 0)
            return "? " + GENERAL_UNIT_KILOBYTES;
        long kbytes = bytes / 1024;
         // round to nearest multiple, or round up if size below 1024
        if ((bytes & 512) != 0 || (bytes > 0 && bytes < 1024)) kbytes++;
        // result formating, according to the current locale
        return NUMBER_FORMAT0.format(kbytes) + GENERAL_UNIT_KILOBYTES;
    }
    
    /**
     * Converts the passed in number of bytes into a byte-size string.
     * Group digits with locale-dependant thousand separator if needed, but
     * with "B", or "KB", or "MB" or "GB" or "TB" locale-dependant unit at the end,
     * and a limited precision of 4 significant digits. 
     * 
     *
     * @param bytes the number of bytes to convert to a size String.
     * @return a String representing the number of kilobytes that the
     *         <code>bytes</code> argument evaluates to, with
     *         "B"/"KB"/"MB"/"GB"/TB" appended at the end. If the input value is
     *         negative, the string returned will be "? KB".
     */
    public static String toUnitbytes(long bytes) {
        if (bytes < 0) {
            return "? " + GENERAL_UNIT_KILOBYTES;
        }
        long   unitValue; // the multiple associated with the unit
        String unitName;  // one of localizable units
        
        if (bytes < 100) {
            unitName = GENERAL_UNIT_BYTES;
            unitValue = 1;
        } else if (bytes < 0xA00000) {                // below 10MB, use KB
            unitValue = 0x400;
            unitName = GENERAL_UNIT_KILOBYTES;
        } else if (bytes < 0x280000000L) {     // below 10GB, use MB
            unitValue = 0x100000;
            unitName = GENERAL_UNIT_MEGABYTES;
        } else if (bytes < 0xA0000000000L) {   // below 10TB, use GB
            unitValue = 0x40000000;
            unitName = GENERAL_UNIT_GIGABYTES;
        } else {                                // at least 10TB, use TB
            unitValue = 0x10000000000L;
            unitName = GENERAL_UNIT_TERABYTES;
        }
        NumberFormat numberFormat; // one of localizable formats
        
        if(bytes < 100) {
            numberFormat = NUMBER_FORMAT0;
        }
        else if ((double)bytes * 100 / unitValue < 99995) {
            // return a minimum "100.0xB", and maximum "999.9xB"
            numberFormat = NUMBER_FORMAT1; // localized "#,##0.0"
        }
        else {
            // return a minimum "1,000xB"
            numberFormat = NUMBER_FORMAT0; // localized "#,##0"
        }
        
        try {
            return numberFormat.format((double)bytes / unitValue) + " " + unitName;
        } catch(ArithmeticException ae) {
            return "0 " + unitName;
            // internal java error, just return 0.
        }
    }
    
    /**
     * Converts an rate into a human readable and localized KB/s speed.
     */
    public static String rate2speed(double rate) {
        return NUMBER_FORMAT0.format(rate) + " " + GENERAL_UNIT_KBPSEC;
    }
    
	/**
	 * Inject fields from AppFrame.properties into object. Fields to be injected
	 * should be annotated <code>@Resource</code> and defined in AppFrame.properties as
	 * <code><Object>.<field> = <data></code>
	 * 
	 * @param object the object whose fields will be injected
	 */
	public static void assignResources(Object object) {
		Application.getInstance().getContext().getResourceMap(AppFrame.class)
				.injectFields(object);
	}
	
    /**
     * Acts as a proxy for the Launcher class so that other classes only need
     * to know about this mediator class.
     *
     * <p>Opens the specified url in a browser.
     *
     * @param url the url to open
     * @return an int indicating the success of the browser launch
     */
    public static final int openURL(String url) {
        // TODO: Fix dependencies so this works!
        throw new RuntimeException("Implement me!");
//        try {
//            return Launcher.openURL(url);
//        } catch(IOException ioe) {
//            // TODO: Show an error
//            //GUIMediator.showError(I18n.tr("LimeWire could not locate your web browser to display the following webpage: {0}.", url));
//            return -1;
//        }
    }

}
