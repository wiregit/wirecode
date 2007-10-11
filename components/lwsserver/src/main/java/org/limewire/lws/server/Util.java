package org.limewire.lws.server;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.limewire.service.ErrorService;

/**
 * Fine, here's a note.
 */
public final class Util {

    final static Map<String, String> EMPTY_MAP_OF_STRING_X_STRING = new HashMap<String, String>();

    private Util() {
    }

    static boolean isEmpty(final String s) {
        return s == null || s.equals("");
    }

    /**
     * Parses and returns CGI arguments.
     * 
     * @param rest the string to parse
     * @return CGI arguments from <tt>rest</tt>
     */
    static Map<String, String> parseArgs(final String rest) {
        return genericParse(rest, "&");
    }

    /**
     * This is the grammar. <br>
     * <br>
     * 
     * <pre>
     * header             = [ element ] *( &quot;,&quot; [ element ] ) 
     * element            = name [ &quot;=&quot; [ value ] ] *( &quot;;&quot; [ param ] )
     * param              = name [ &quot;=&quot; [ value ] ] name = token value = ( token | quoted-string ) 
     * token              = 1*&lt;any
     * char except &quot; = &quot;, &quot;,&quot;, &quot;;&quot;, &lt;&quot;&gt; and white space&gt; 
     * quoted-string      = &lt;&quot;&gt; *( text | quoted-char ) &lt;&quot;&gt;
     * text               = any char except &lt;&quot;&gt; 
     * quoted-char        = &quot;\&quot; char
     * </pre>
     * 
     * @param rest the part <b>after</b> the <tt>&lt;name&gt; ':'</tt>
     * @return
     */
    static Map<String, String> parseHeader(final String rest) {
        return genericParse(rest, ";");
    }

    private static Map<String, String> genericParse(final String rest,
            final String delim) {
        if (isEmpty(rest))
            return EMPTY_MAP_OF_STRING_X_STRING;
        final Map<String, String> res = new HashMap<String, String>(3);
        for (StringTokenizer st = new StringTokenizer(rest, delim, false); st
                .hasMoreTokens();) {
            final String pair = st.nextToken();
            final int ieq = pair.indexOf('=');
            String key, val;
            if (ieq == -1) {
                key = pair;
                val = null;
            } else {
                key = pair.substring(0, ieq);
                val = pair.substring(ieq + 1);
            }
            res.put(key.trim(), val == null ? val : val.trim());
        }
        return res;
    }

    /**
     * Returns a string suitable for the date for a cookie.
     * 
     * @return a string suitable for the date for a cookie
     */
    static String createCookieDate() {
        // Wdy, DD-Mon-YYYY HH:MM:SS GMT
        Format f = new SimpleDateFormat("E, dd-MM-yyyy kk:mm:ss");
        Date date = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime();
        return f.format(date) + " GMT";
    }

    /**
     * Returns the simple name for the class <tt>c</tt>.
     * 
     * @param c the class in question
     * @return the simple name for the class <tt>c</tt>
     */
    static String simpleName(final Class c) {
        String s = c.getName();
        int ilastDot = s.lastIndexOf(".");
        return ilastDot == -1 ? s : s.substring(ilastDot + 1);
    }

    private final static String ERROR_START = "ERROR:";

    /**
     * Returns error code fonud in {@link ErrorCodes} or <tt>null</tt> if it's
     * not an error.
     * 
     * @param line
     * @return error code fonud in {@link ErrorCodes} or <tt>null</tt> if it's
     *         not an error
     */
    static String unwrapError(final String line) {
        if (line.startsWith(ERROR_START)) {
            return line.substring(ERROR_START.length()).trim();
        } else {
            return null;
        }
    }

    /**
     * Returns the appropriate string for identifying an error.
     * 
     * @param error the error message
     * @return the appropriate string for identifying an error
     */
    static String wrapError(final String error) {
        return ERROR_START + error;
    }

    /**
     * Will remove the method application to <tt>res</tt>.
     * 
     * @param res
     * @return
     */
    static String removeCallback(final String res) {
        if (res == null)
            return null;
        final String start = "(" + DispatcherSupport.Constants.CALLBACK_QUOTE_STRING;
        final String end = DispatcherSupport.Constants.CALLBACK_QUOTE_STRING + ")";
        int istart = res.indexOf(start);
        if (istart == -1)
            return res;
        int iend = res.lastIndexOf(end);
        if (iend == -1)
            return res;
        return res.substring(istart + start.length(), iend);
    }

    private static boolean isValidKey(final String key) {
        if (key == null)
            return false;
        return key.length() == DispatcherSupport.Constants.KEY_LENGTH;
    }

    /**
     * Returns <tt>true</tt> if <tt>key</tt> is a valid public key,
     * <tt>false</tt> otherwise.
     * 
     * @param key key in question
     * @return <tt>true</tt> if <tt>key</tt> is a valid public key,
     *         <tt>false</tt> otherwise
     */
    static boolean isValidPublicKey(final String key) {
        return isValidKey(key);
    }

    /**
     * Returns <tt>true</tt> if <tt>key</tt> is a valid private key,
     * <tt>false</tt> otherwise.
     * 
     * @param key key in question
     * @return <tt>true</tt> if <tt>key</tt> is a valid private key,
     *         <tt>false</tt> otherwise
     */
    static boolean isValidPrivateKey(final String key) {
        return isValidKey(key);
    }

    /**
     * Returns the string IP address for the given address.
     * 
     * @param addr address in question
     * @return the string IP address for the given address
     */
    static String getIPAddress(final InetAddress addr) {
        if (addr == null)
            return null;
        byte[] bs = addr.getAddress();
        return bs[0] + "." + bs[1] + "." + bs[2] + "." + bs[3];
    }

    /**
     * Generates a public or private key. <br>
     * INVARIANT: {@link #isValidPublicKey(String)}({@link #generateKey()})
     * <tt> == true</tt> and {@link #isValidPrivateKey(String)}({@link #generateKey()})
     * <tt> == true</tt>
     * 
     * @return a public or private key
     */
    static String generateKey() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < DispatcherSupport.Constants.KEY_LENGTH;) {
            final int r = 'A' + (int) (Math.random() * ('Z' - 'A'));
            final char c = (char) r;
            if (c == ';')
                continue;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /**
     * Returns a string with the urlencoded arguments removed with these
     * arguments separated by {@link Constants#ARGUMENT_SEPARATOR}. This method
     * has the side-affect of putting these urlencoded arguments into
     * <tt>args</tt>. Arguments with no equals sign will have
     * <code>null</code> values, arguments with nothing after the equal sign
     * will have an empty value, non-empty arguments will be as expected. Here
     * are a couple exampels:
     * <ul>
     * <li>one=1 &rarr; <code>{one=1}</code></li>
     * <li>one= &rarr; <code>{one=}</code></li>
     * <li>one &rarr; <code>{one=null}</code></li>
     * </ul>
     * 
     * @param cmd original command, this can be <code>null</code>
     * @param args original arguments (SIDEEFFECT: these are updated)
     * @return a string with the urlencoded arguments removed with these
     *         arguments separated by {@link Constants#ARGUMENT_SEPARATOR}
     */
    static String addURLEncodedArguments(final String cmd,
            final Map<String, String> args) {
        if (cmd == null) return null;

        int ihuh = cmd.indexOf("?");
        if (ihuh == -1) return cmd;

        final String newCmd = cmd.substring(0, ihuh);
        //
        // We have to decode the rest because the commands are identified using
        // CGI parameters, but the value of the arguments are often parameters,
        // and get encoded.
        //
        String tmpRest = cmd.substring(ihuh + 1);
        try {
            tmpRest = URLDecoder.decode(tmpRest, "UTF-8");
        } catch (Exception e) { ErrorService.error(e); }
        final String rest = tmpRest;
        for (StringTokenizer st = new StringTokenizer(rest, "&", false); 
             st.hasMoreTokens();) {
            final String tok = st.nextToken();
            int ieq = tok.indexOf('=');
            String key, val;
            if (ieq == -1) {
                key = tok;
                val = null;
            } else {
                key = tok.substring(0, ieq);
                val = tok.substring(ieq + 1);
            }
            args.put(key, val);

        }
        return newCmd;

    }
    
    static String getArg(final Map<String, String> args, final String key) {
        final String res = args.get(key);
        return res == null ? "" : res;
    }    

}
