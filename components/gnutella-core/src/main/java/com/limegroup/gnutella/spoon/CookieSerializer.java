package com.limegroup.gnutella.spoon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

public class CookieSerializer {
    
    public static final Log LOG = LogFactory.getLog(CookieSerializer.class);

    private static final String NAME = "name";
    private static final String VALUE= "value";
    private static final String EXPIRY_DATE = "expirydate";
    private static final String DOMAIN = "domain";
    private static final String PATH = "path";
    private static final String VERSION = "version";
    private static final String SECURE = "secure";
    
    public static String toString(List<Cookie> cookies) {
        try {
            return toJsonArray(cookies).toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @return empty list if cookies could to be parsed correctly
     */
    public static List<Cookie> toCookies(String cookies) {
        try {
            return toCookies(new JSONArray(cookies));
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }
    
    public static JSONArray toJsonArray(List<Cookie> cookies) throws JSONException {
        JSONArray array = new JSONArray();
        for (Cookie cookie: cookies) {
            if (cookie.isPersistent()) {
                array.put(toJsonObject(cookie));
            }
        }
        return array;
    }
    
    public static JSONObject toJsonObject(Cookie cookie) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(NAME, cookie.getName());
        json.put(VALUE, cookie.getValue());
        Date date = cookie.getExpiryDate();
        if (date != null) {
            json.put(EXPIRY_DATE, DateUtils.formatDate(date));
        }
        json.put(DOMAIN, cookie.getDomain());
        String path = cookie.getPath();
        if (path != null) {
            json.put(PATH, path);
        }
        json.put(VERSION, cookie.getVersion());
        json.put(SECURE, cookie.isSecure());
        return json;
    }
    
    public static Cookie toCookie(JSONObject json) throws JSONException {
        BasicClientCookie2 cookie = new BasicClientCookie2(json.getString(NAME), json.getString(VALUE));
        String date = json.optString(EXPIRY_DATE, null);
        if (date != null) {
            try {
                cookie.setExpiryDate(DateUtils.parseDate(date));
            } catch (DateParseException e) {
                throw new JSONException(e);
            }
        }
        cookie.setDomain(json.getString(DOMAIN));
        cookie.setPath(json.optString(PATH, null));
        cookie.setVersion(json.getInt(VERSION));
        cookie.setSecure(json.getBoolean(SECURE));
        return cookie;
    }
    
    public static List<Cookie> toCookies(JSONArray json) throws JSONException {
        List<Cookie> cookies = new ArrayList<Cookie>(json.length());
        for (int i = 0; i < json.length(); i++) {
            try {
                cookies.add(toCookie(json.getJSONObject(i)));
            } catch (JSONException e) {
                // just ignore this cookie
                LOG.debugf(e, "error parsing {0}", json);
            }
        }
        return cookies;
    }
    
}
