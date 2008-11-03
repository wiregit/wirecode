package org.limewire.ui.swing.util;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.util.OSUtils;



public class GuiUtils {

    private static final Log LOG = LogFactory.getLog(GuiUtils.class);
    
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
        setLocale(Locale.getDefault());
    }
    
    static void setLocale(Locale locale) {
        NUMBER_FORMAT0 = NumberFormat.getNumberInstance(locale);
        NUMBER_FORMAT0.setMaximumFractionDigits(0);
        NUMBER_FORMAT0.setMinimumFractionDigits(0);
        NUMBER_FORMAT0.setGroupingUsed(true);
        
        NUMBER_FORMAT1 = NumberFormat.getNumberInstance(locale);
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
     * Returns the application's default frame.
     */
    public static JFrame getMainFrame() {
        if(AppFrame.isStarted()) {
            Application app = Application.getInstance();
            if(app instanceof SingleFrameApplication) {
                return ((SingleFrameApplication)app).getMainFrame();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
	/**
	 * Inject fields from AppFrame.properties into object. Fields to be injected
	 * should be annotated <code>@Resource</code> and defined in AppFrame.properties as
	 * <code>ClassNameWithoutPackage.variableName=resource</code>
	 * 
	 * @param object the object whose fields will be injected
	 */
	public static void assignResources(Object object) {

		Application.getInstance().getContext().getResourceMap(AppFrame.class)
				.injectFields(object);
	}
    
    /**
     * Convert a color object to a hex string
     */
    public static String colorToHex(Color colorCode){
        int r = colorCode.getRed();
        int g = colorCode.getGreen();
        int b = colorCode.getBlue();
        
        return toHex(r) + toHex(g) + toHex(b);   
    }   

    
    /** Returns the int as a hex string. */
    private static String toHex(int i) {
        String hex = Integer.toHexString(i).toUpperCase(Locale.US);
        if (hex.length() == 1)
            return "0" + hex;
        else
            return hex;
    }
    
    /**
     * Updates the component to use the native UI resource.
     */
    public static ComponentUI getNativeUI(JComponent c) {
        ComponentUI ret = null;
        String name = UIManager.getSystemLookAndFeelClassName();
        if (name != null) {
            try {
                Class clazz = Class.forName(name);
                LookAndFeel lf = (LookAndFeel) clazz.newInstance();
                lf.initialize();
                UIDefaults def = lf.getDefaults();
                ret = def.getUI(c);
            } catch (ExceptionInInitializerError e) {
            } catch (ClassNotFoundException e) {
            } catch (LinkageError e) {
            } catch (IllegalAccessException e) {
            } catch (InstantiationException e) {
            } catch (SecurityException e) {
            } catch (ClassCastException e) {
            }
        }

        // if any of those failed, default to the current UI.
        if (ret == null)
            ret = UIManager.getUI(c);

        return ret;
    }
    
    /**
     * Returns <code>text</code> wrapped by an HTML table tag that is set to a
     * fixed width.
     * <p>
     * Note: It seems to be a possible to trigger a NullPointerException in
     * Swing when this is used in a JLabel: GUI-239.
     */
    public static String restrictWidth(String text, int width) {
        return "<html><table width=\"" + width + "\"><tr><td>" + text
                + "</td></tr></table></html>";
    }
    
    /**
     * Using a little reflection here for a lack of any better way 
     * to access locale-specific char codes for menu mnemonics.
     * We could at least defer this in the future.
     *
     * @param str the key for the locale-specific char resource to
     *  look up -- the key as it appears in the locale-specific
     *  properties file
     * @return the code for the passed-in key as defined in 
     *  <tt>java.awt.event.KeyEvent</tt>, or -1 if no key code
     *  could be found
     */
    public static int getCodeForCharKey(String str) {
        int charCode = -1;
        String charStr = str.toUpperCase(Locale.US);
        if(charStr.length()>1) return -1;
        try {
            Field charField = KeyEvent.class.getField("VK_"+charStr);
            charCode = charField.getInt(KeyEvent.class);
        } catch (NoSuchFieldException e) {
            LOG.error("can't get key for: " + charStr, e);
        } catch (SecurityException e) {
            LOG.error("can't get key for: " + charStr, e);
        } catch (IllegalAccessException e) {
            LOG.error("can't get key for: " + charStr, e);
        }
        return charCode;
    }
    
    private static int getAmpersandPosition(String text) {
        int index = -1;
        while ((index = text.indexOf('&', index + 1)) != -1) {
            if (index < text.length() - 1 && Character.isLetterOrDigit(text.charAt(index + 1))) {
                break;
            }
        }
        return index;
    }
    
    /**
     * Strips the first ampersand '&' in <code>text</code> that appears
     * before a letter or digit.
     * 
     * @return the original text if there is no such ampersand
     */
    public static String stripAmpersand(String text) {
        int index = getAmpersandPosition(text);
        if (index >= 0) {
            return text.substring(0, index) + text.substring(index + 1);
        }
        return text;
    }
    
    /**
     * Finds the first ampersand '&' in <code>text</code> that appears
     * before a letter or a digit and returns the key code for the letter
     * or digit after it.
     */
    public static int getMnemonicKeyCode(String text) {
        // parse out mnemonic key
        int index = getAmpersandPosition(text);
        if (index >= 0) {
            return getCodeForCharKey(text.substring(index + 1, index + 2));
        }
        return -1;
    }
    
    /**
     * Determines if the Start On Startup option is availble.
     */
    public static boolean shouldShowStartOnStartupWindow() {
        return OSUtils.isMacOSX() ||
               WindowsUtils.isLoginStatusAvailable();
    }
    
}
