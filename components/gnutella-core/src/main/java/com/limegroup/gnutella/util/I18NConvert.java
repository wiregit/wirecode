package com.limegroup.gnutella.util;

public class I18NConvert {

    I18NData _data;
    
    private static I18NConvert _instance = new I18NConvert();
    
    private I18NConvert() {
        initialize();
    }

    public static I18NConvert instance() {
        return _instance;
    }

    private void initialize() {
        _data = new I18NData();
    }

    public String getNorm(String s) {
        return getN(s);
    } 
    
    public String[] getKeywords(String s) {
        return StringUtils.split(blockSplit(getKC(getDK(s))), " ");
    }
    
    private String getN(String s) {
        return blockSplit(getKC(getDK(s)));
    }

    private String getDK(String s) {
        if(s.length() == 0) return  s;
        else {
            StringBuffer buf = new StringBuffer();
            for(int i = 0, n = s.length(); i < n; i++)
                buf.append(_data.getDK(s.charAt(i)));
            return buf.toString();
        }
    }
    
    private String getKC(String s) {
        if(s.length() == 0) return s;
        else {
            char first = s.charAt(0);
            StringBuffer b = new StringBuffer();
            String comped = "";
            
            //need to check for more than two
            for(int i = 1, n = s.length(); i < n; i++) {
                comped = _data.getKC(String.valueOf(first) + String.valueOf(s.charAt(i)));
                if(comped != null) 
                    first = comped.charAt(0);
                else {
                    b.append(first);
                    first = s.charAt(i);
                }
            }
            
            b.append(first);
        return b.toString();
        }
    }

    private String blockSplit(String s) {
        if(s.length() == 0) return s;
        else {
            int blockb4 = of(s.charAt(0));
            int curBlock;
            StringBuffer buf = new StringBuffer();
            buf.append(s.charAt(0));
            for(int i = 1, n = s.length(); i < n; i++) {
                curBlock = of(s.charAt(i));
                if(curBlock != blockb4 && 
                   (s.charAt(i) != '\u0020' && s.charAt(i - 1) != '\u0020'))
                    buf.append("\u0020");
                buf.append(s.charAt(i));
                blockb4 = curBlock;
            }
            
            return buf.toString().trim();
        }
    }

	private int of(char c) {
	    int top, bottom, current;
	    bottom = 0;
	    top = blockStarts.length;
	    current = top/2;
	    while (top - bottom > 1) {
		if (c >= blockStarts[current]) {
		    bottom = current;
		} else {
		    top = current;
		}
		current = (top + bottom) / 2;
	    }
	    return current;
	}

    //copy from Character.java
	private static final char blockStarts[] = {
        '\u0000',
        '\u0080',
	    '\u0100',
	    '\u0180',
	    '\u0250',
	    '\u02B0',
	    '\u0300', 
	    '\u0370',
	    '\u0400',
	    '\u0500', // unassigned
	    '\u0530',
	    '\u0590',
	    '\u0600',
	    '\u0700', // unassigned
	    '\u0900',
	    '\u0980',
	    '\u0A00',
	    '\u0A80',
	    '\u0B00',
	    '\u0B80',
	    '\u0C00',
	    '\u0C80',
	    '\u0D00',
	    '\u0D80', // unassigned
	    '\u0E00',
	    '\u0E80',
	    '\u0F00',
	    '\u0FC0', // unassigned
	    '\u10A0',
	    '\u1100',
	    '\u1200', // unassigned
	    '\u1E00',
	    '\u1F00',
	    '\u2000',
	    '\u2070',
	    '\u20A0',
	    '\u20D0',
	    '\u2100',
	    '\u2150',
	    '\u2190',
	    '\u2200',
	    '\u2300',
	    '\u2400',
	    '\u2440',
	    '\u2460',
	    '\u2500',
	    '\u2580',
	    '\u25A0',
	    '\u2600',
	    '\u2700',
	    '\u27C0', // unassigned
	    '\u3000',
	    '\u3040',
	    '\u30A0',
	    '\u3100',
	    '\u3130',
	    '\u3190',
	    '\u3200',
	    '\u3300',
	    '\u3400', // unassigned
	    '\u4E00',
	    '\uA000', // unassigned
	    '\uAC00',
	    '\uD7A4', // unassigned
	    '\uD800',
	    '\uE000',
	    '\uF900',
	    '\uFB00',
	    '\uFB50',
	    '\uFE00', // unassigned
	    '\uFE20',
	    '\uFE30',
	    '\uFE50',
	    '\uFE70',
	    '\uFEFF', // special
	    '\uFF00',
	    '\uFFF0'
	};


}



