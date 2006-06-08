package com.limegroup.gnutella.i18n;

/**
 * Describes a line in a properties file.
 */
class Line {
    private final String wholeLine;
    private final String key;
    private final String value;
    private final int braces;
    private final boolean extraComment;

    /**
     * TODO: does not handle all comment lines properly!<br />
     * TODO: does not separate key=value pairs properly!<br />
     * TODO: does not decode continuation lines properly (continuation lines
     * should be already joined in a upper layer before passing data line here.)<br />
     * TODO: ignores lines that don't have an = or #.
     * 
     * @param data
     *            a data line to parse and store.
     */
    Line(String data) {
        if (data == null)
            throw new NullPointerException("null data");
        this.wholeLine = data;
        data.trim();
        if (data.startsWith("#?")) { //$NON-NLS-1$
            data = data.substring(2).trim();
            this.extraComment = true;
        } else {
            this.extraComment = false;
        }
        if (data.startsWith("#") || data.equals("")) { //$NON-NLS-1$//$NON-NLS-2$
            this.key = null;
            this.value = null;
            this.braces = 0;
        } else {
            final int eq = data.indexOf('=');
            if (eq == -1) {
                this.key = null;
                this.value = null;
                this.braces = 0;
            } else {
                this.key = data.substring(0, eq);
                if (eq >= data.length() - 1)
                    this.value = ""; //$NON-NLS-1$
                else
                    this.value = data.substring(eq + 1);
                this.braces = parseBraceCount(this.value);
            }
        }
    }

    static int parseBraceCount(String value) {
        int count = 0;
        int startIdx = value.indexOf('{');
        while (startIdx != -1) {
            int endIdx = value.indexOf('}', startIdx);
            if (endIdx != -1) {
                try {
                    Integer.parseInt(value.substring(startIdx + 1, endIdx));
                    count++;
                } catch (NumberFormatException nfe) {/* ignored */}
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
        return this.key == null;
    }

    /**
     * @return this whole text line (comment or key=value).
     */
    String getLine() {
        return this.wholeLine;
    }

    /**
     * @return this line key, or null if isComment() is true.
     */
    String getKey() {
        return this.key;
    }

    /**
     * @return this line value, or null if isComment() is true.
     */
    String getValue() {
        return this.value;
    }

    /**
     * @return the number of brace pairs this line had
     */
    int getBraceCount() {
        return this.braces;
    }

    /**
     * @return true if the line had an extra "#? " in front.
     */
    boolean hadExtraComment() {
        return this.extraComment;
    }
}
