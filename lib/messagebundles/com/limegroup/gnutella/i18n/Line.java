package com.limegroup.gnutella.i18n;

/**
 * Describes a line in a properties file.
 */
class Line {
    private final String wholeLine;

    private final String key;

    private final String value;

    private final int braces;

    /**
     * TODO: does not handle all comment lines properly! TODO: does not separate
     * key=value pairs properly! TODO: does not decode continuation lines
     * properly (continuation lines should be already joined in a upper layer
     * before passing data line here.)
     * 
     * @param data
     *            a data line to parse and store.
     */
    Line(String data) {
        if (data == null)
            throw new NullPointerException("null data");
        wholeLine = data;
        if (data.startsWith("#") || data.trim().equals("")) {
            key = null;
            value = null;
            braces = 0;
        } else {
            int eq = data.indexOf("=");
            if (eq == -1)
                throw new IllegalArgumentException("can't decode line: " + data);
            key = data.substring(0, eq);
            if (eq == data.length() || eq == data.length() - 1)
                value = "";
            else
                value = data.substring(eq + 1);

            braces = parseBraceCount(value);
        }
    }

    static int parseBraceCount(String value) {
        int count = 0;
        int startIdx = value.indexOf("{");
        while (startIdx != -1) {
            int endIdx = value.indexOf("}", startIdx);
            if (endIdx != -1) {
                try {
                    Integer.parseInt(value.substring(startIdx + 1, endIdx));
                    count++;
                } catch (NumberFormatException ignored) {
                }
                startIdx = endIdx + 1;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * @return true if this is a not a key=value line.
     */
    boolean isComment() {
        return key == null;
    }

    /**
     * @return this whole text line (comment or key=value).
     */
    String getLine() {
        return wholeLine;
    }

    /**
     * @return this line key, or null if isComment() is true.
     */
    String getKey() {
        return key;
    }

    /**
     * @return this line value, or null if isComment() is true.
     */
    String getValue() {
        return value;
    }

    /**
     * Gets the number of braces this line had.
     */
    int getBraceCount() {
        return braces;
    }
}
