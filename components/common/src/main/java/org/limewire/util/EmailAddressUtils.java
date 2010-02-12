package org.limewire.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailAddressUtils {
    private static final int LABEL_LENGTH = 63;

    private static final int DOMAIN_LENGTH = 255;

    private static final int LOCAL_PART_LENGTH = 64;

    private static final int ADDRESS_LENGTH = 256;

    /**
     * Checks that address is a valid address based on
     * <a href="http://tools.ietf.org/html/rfc5322">http://tools.ietf.org/html/rfc532</a>
     * 
     * @param address the email address to validate
     * @throws NullPointerException if the given <code>address</code> is <code>null</code>
     */
    public static boolean isValidAddress(String address) {

        /*
         * The upper limit on address lengths should normally be considered to
         * be 256. The maximum total length of a reverse-path or forward-path is
         * 256
         */
        if (address.length() > ADDRESS_LENGTH)
            return false;

        final int atIndex = address.lastIndexOf('@');
        if (atIndex == -1 || atIndex == address.length() - 1)
            return false;

        int braceDepth = 0;
        boolean inQuote = false;
        boolean escapeThisChar = false;

        for (int i = 0; i < address.length(); ++i) {
            final char chr = address.charAt(i);
            boolean replaceChar = false;

            // Escape the next character?
            if (chr == '\\')
                escapeThisChar = !escapeThisChar;
            else {
                switch (chr) {
                case '(':
                    if (escapeThisChar)
                        replaceChar = true;
                    else if (inQuote)
                        replaceChar = true;
                    else // Increment brace depth
                    if (braceDepth++ > 0)
                        replaceChar = true;

                    break;
                case ')':
                    if (escapeThisChar)
                        replaceChar = true;
                    else if (inQuote)
                        replaceChar = true;
                    else {
                        // Decrement brace depth
                        if (--braceDepth > 0)
                            replaceChar = true;
                        if (braceDepth < 0)
                            braceDepth = 0;
                    }
                    break;
                case '"':
                    if (escapeThisChar)
                        replaceChar = true;
                    else if (braceDepth == 0)
                        // Are we inside a quoted string?
                        inQuote = !inQuote;
                    else
                        replaceChar = true;
                    break;
                case '.':
                    // Dots don't help either
                    if (escapeThisChar)
                        replaceChar = true;
                    else if (braceDepth > 0)
                        replaceChar = true;
                    break;

                default:
                }
                escapeThisChar = false;
                if (replaceChar)
                    // Replace the offending character
                    address = StringUtils.replaceCharAt(address, i, 'x');
            }
        }

        final String localPart = address.substring(0, atIndex);
        final String domain = address.substring(atIndex + 1);

        /*
         * compiled and shared regex pattern used throughout the code.
         */
        final String FOLDING_WHITE_SPACE = "(?:(?:(?:[ \\t]*(?:\\r\\n))?[ \\t]+)|(?:[ \\t]+(?:(?:\\r\\n)[ \\t]+)*))";
        final String EMPTY_STR = "";

        final Pattern LEAD_TRAIL_FOLDING_WHITE_SPACE_PATTERN = Pattern.compile("^"
                + FOLDING_WHITE_SPACE + "|" + FOLDING_WHITE_SPACE + "$");
        final Pattern ADDRESS_LITERAL_PATTERN = Pattern
                .compile("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
        final Pattern ADDRESS_LOCAL_PATTERN = Pattern
                .compile("\\.(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*(?![^\\\"]*\\\"))");
        final Pattern DOMAIN_PATTERN = Pattern
                .compile("[\\.|/](?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*(?![^\\\"]*\\\"))");
        final Pattern DOMAIN_NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
        final Pattern SPLIT_BY_COLON_PATTERN = Pattern.compile(":");
        final Pattern ILLEGAL_COMMENT_CHAR_OPEN_P_PATTERN = Pattern.compile("(?<!\\\\)[\\(\\)]");
        final Pattern ILLEGAL_COMMENT_CHAR_CLOSE_P_PATTERN = Pattern
                .compile("(?<!\\\\)(?:[\\(\\)])");
        final Pattern ESCAPE_CR_LF_NULL_PATTERN = Pattern
                .compile("(?<!\\\\|^)[\"\\r\\n\\x00](?!$)|\\\\\"$|\"\"");
        final Pattern CHARS_IN_QUOTED_STR_PATTERN = Pattern
                .compile("[\\x00-\\x20\\(\\)<>\\[\\]:;@\\\\,\\.\"]");
        final Pattern DBL_BACK_SLASH_PATTERN = Pattern.compile("\\\\\\\\");
        final Pattern QUOTED_STR_PATTERN = Pattern.compile("^\"(?:.)*\"$", Pattern.DOTALL);
        final Pattern DOMAIN_ADDR_LITERAL_PATTERN = Pattern.compile("^\\[(.)+]$");
        final Pattern QUOTED_STR_FOLDING_WHITE_SPACE_PATTERN = Pattern.compile("(?<!\\\\)"
                + FOLDING_WHITE_SPACE);
        final Pattern ADDRESS_LABEL_PATTERN = Pattern
                .compile("[\\x00-\\x20\\(\\)<>\\[\\]:;@\\\\,\\.\"]|^-|-$");

        /*
         * Check the local part for RFC compliance
         */
        String[] dotArray = ADDRESS_LOCAL_PATTERN.split(localPart, -1);
        int partLength = 0;

        for (String element : dotArray) {
            // Remove any leading or trailing folding white space
            element = LEAD_TRAIL_FOLDING_WHITE_SPACE_PATTERN.matcher(element).replaceAll(EMPTY_STR);

            if (element.length() == 0)
                return false;

            /*
             * Can't have empty element (consecutive dots or dots at the start
             * or end) We need to remove any valid comments (i.e. those at the
             * start or end of the element)
             */
            if (element.charAt(0) == '(') {
                final int indexBrace = element.indexOf(')');

                if (indexBrace != -1) {
                    if (indexBrace > 1
                            && ILLEGAL_COMMENT_CHAR_OPEN_P_PATTERN.matcher(
                                    element.substring(1, indexBrace - 1)).find())
                        return false; // Illegal characters in comment
                    element = element.substring(indexBrace + 1);
                }
            }

            if (element.charAt(element.length() - 1) == ')') {
                final int indexBrace = element.lastIndexOf('(');
                if (indexBrace != -1) {
                    if (ILLEGAL_COMMENT_CHAR_CLOSE_P_PATTERN.matcher(
                            element.substring(indexBrace + 1, element.length() - 1)).find())
                        return false; // Illegal characters in comment

                    element = element.substring(0, indexBrace);
                }
            }
            /*
             * Remove any leading or trailing FWS around the element (inside any
             * comments)
             */
            element = LEAD_TRAIL_FOLDING_WHITE_SPACE_PATTERN.matcher(element).replaceAll(EMPTY_STR);
            // What's left counts towards the maximum length for this part
            if (partLength > 0)
                partLength++; // for the dot

            // Each dot-delimited component can be an atom or a quoted string
            partLength += element.length();

            if (QUOTED_STR_PATTERN.matcher(element).find()) {
                // Quoted-string tests: // // Remove any FWS
                element = QUOTED_STR_FOLDING_WHITE_SPACE_PATTERN.matcher(element).replaceAll(
                        EMPTY_STR);

                // So remove all \\ from the string first...
                element = DBL_BACK_SLASH_PATTERN.matcher(element).replaceAll(" ");

                // ", CR, LF and NUL must be escaped, "" is too short
                if (ESCAPE_CR_LF_NULL_PATTERN.matcher(element).find())
                    return false;
            } else {
                /*
                 * Period (".") may...appear, but may not be used to start or
                 * end the local part, nor may two or more consecutive periods
                 * appear. A zero-length element implies a period at the
                 * beginning or end of the local part, or two periods together.
                 * Either way it's not allowed.
                 */
                if (element.isEmpty())
                    return false; // Dots in wrong place
                /*
                 * Any ASCII graphic (printing) character other than the at-sign
                 * ("@"), backslash, double quote, comma, or square brackets may
                 * appear without quoting. If any of that list of excluded
                 * characters are to appear, they must be quoted Any excluded
                 * characters? i.e. 0x00-0x20, (, ), <, >, [, ], :, ;, @, \,
                 * comma, period, "
                 */

                if (CHARS_IN_QUOTED_STR_PATTERN.matcher(element).find())
                    return false; // Characters must be in a quoted string
            }

        }

        if (partLength > LOCAL_PART_LENGTH)
            return false;

        /*
         * domain name can also be replaced by an IP address in square brackets
         * No need to share the pattern; used once only
         */
        if (DOMAIN_ADDR_LITERAL_PATTERN.matcher(domain).find()) {
            // It's an address-literal
            final String addressLiteral = domain.substring(1, domain.length() - 1);

            final List<String> matchesIP = new ArrayList<String>();

            // Extract IPv4 part from the end of the address-literal (if there
            // is one)

            final Matcher matcher = ADDRESS_LITERAL_PATTERN.matcher(addressLiteral);

            while (matcher.find())
                matchesIP.add(matcher.group());

            String ipv6 = null;
            int groupMax = 0;

            if (matchesIP.size() > 0) {
                final int index = addressLiteral.lastIndexOf(matchesIP.get(0));

                if (index == 0)
                    // Nothing there except a valid IPv4 address, so...
                    return true;
                else {
                    // Assume it's an attempt at a mixed address (IPv6 + IPv4)
                    if (addressLiteral.charAt(index - 1) != ':')
                        // character preceding IPv4 address must be ':'
                        return false;
                    if (!addressLiteral.substring(0, 5).equals("IPv6:"))
                        return false;

                    if (index == 7)
                        ipv6 = addressLiteral.substring(5, index);
                    else
                        ipv6 = addressLiteral.substring(5, index - 1);

                    groupMax = 6;
                }
            } else {
                // It must be an attempt at pure IPv6
                if (!addressLiteral.substring(0, 5).equals("IPv6:"))
                    return false;
                ipv6 = addressLiteral.substring(5);
                groupMax = 8;
            }

            matchesIP.clear();
            final String[] matches = SPLIT_BY_COLON_PATTERN.split(ipv6);

            final int index = ipv6.indexOf("::");

            if (index == -1) {
                // We need exactly the right number of groups
                if (matches.length != groupMax)
                    return false;
            } else {
                if (index != ipv6.lastIndexOf("::"))
                    return false;
                groupMax = index == 0 || index == ipv6.length() - 2 ? groupMax : groupMax - 1;
                if (matches.length > groupMax)
                    return false;

            }

            for (final String string : matches)
                if (string.length() > 0) {
                    if (string.length() > 4)
                        return false;
                    try {
                        Long.parseLong(string);
                    } catch (final NumberFormatException e) {
                        return false;
                    }
                }
            return true;

        } else {
            dotArray = DOMAIN_PATTERN.split(domain, -1);
            partLength = 0;
            int indexBrace = -1;

            if (dotArray.length == 1)
                return false;

            String element = EMPTY_STR;
            for (final String element2 : dotArray) {
                element = LEAD_TRAIL_FOLDING_WHITE_SPACE_PATTERN.matcher(element2).replaceAll(
                        EMPTY_STR);

                if (element.length() == 0)
                    return false;

                if (element.charAt(0) == '(') {
                    indexBrace = element.indexOf(")");

                    if (indexBrace != -1) {
                        if (ILLEGAL_COMMENT_CHAR_OPEN_P_PATTERN.matcher(
                                element.substring(1, indexBrace - 1)).find())
                            return false; // Illegal characters in comment

                        element = element.substring(indexBrace + 1, element.length());
                    }
                }

                if (element.charAt(element.length() - 1) == ')') {
                    indexBrace = element.indexOf("(");

                    if (indexBrace != -1) {
                        if (ILLEGAL_COMMENT_CHAR_CLOSE_P_PATTERN.matcher(
                                element.substring(indexBrace + 1, element.length() - 1)).find())
                            return false; // Illegal characters in comment

                        element = element.substring(0, indexBrace);

                    }
                }

                // Remove any leading or trailing FWS around the element (inside
                // any comments)
                element = LEAD_TRAIL_FOLDING_WHITE_SPACE_PATTERN.matcher(element).replaceAll(
                        EMPTY_STR);

                // What's left counts towards the maximum length for this part
                if (partLength > 0)
                    partLength++; // for the dot

                partLength += element.length();

                // Label must be 63 characters or less
                if (element.length() > LABEL_LENGTH
                        || ADDRESS_LABEL_PATTERN.matcher(element).find())
                    return false;

            }

            if (partLength > DOMAIN_LENGTH || DOMAIN_NUMERIC_PATTERN.matcher(element).find())
                return false;

            return true;
        }
    }

}
