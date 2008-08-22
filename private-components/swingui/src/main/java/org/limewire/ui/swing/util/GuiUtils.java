package org.limewire.ui.swing.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
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
    public static Window getMainFrame() {
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
	 * <code><Object>.<field> = <data></code>
	 * 
	 * @param object the object whose fields will be injected
	 */
	public static void assignResources(Object object) {
		Application.getInstance().getContext().getResourceMap(AppFrame.class)
				.injectFields(object);
	}
    
    /**
     * Returns a {@link MouseListener} that listens for events
     * and changes the mouse into a hand (or out of a hand)
     * when it enters or exits the component the listener is added
     * to.  If the mouse is clicked, it forwards the action
     * to the listener.
     */
    public static MouseListener getActionHandListener(final ActionListener listener) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JComponent comp = (JComponent) e.getComponent();
                comp.getTopLevelAncestor()
                        .setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JComponent comp = (JComponent) e.getComponent();
                comp.getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                listener.actionPerformed(new ActionEvent(e.getComponent(),
                        ActionEvent.ACTION_PERFORMED, null));
            }
        };
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

    
    /**
     * Returns the int as a hex string.
     */
    public static String toHex(int i) {
        String hex = Integer.toHexString(i).toUpperCase(Locale.US);
        if (hex.length() == 1)
            return "0" + hex;
        else
            return hex;
    }
    /**
     * Convert a hex string to a color object
     */
    public static Color hexToColor(String hexString){
        int decimalColor;
        decimalColor = Integer.parseInt(hexString, 16);
        return new Color(decimalColor);
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
    
    public static JButton createIconButton(Icon icon, Icon rolloverIcon, Icon pressedIcon) {
        JButton button = new JButton();
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setIcon(icon);
        button.setRolloverIcon(rolloverIcon);
        button.setPressedIcon(pressedIcon);
        button.setHideActionText(true);
        return button;
    }  
}
