package com.limegroup.gnutella.http;

import com.limegroup.gnutella.util.*;

public final class ConstantHTTPHeaderValues {

    public static final HTTPHeaderValue SERVER_VALUE = 
        new HTTPHeaderValue() {
            public String httpStringValue() {
                return CommonUtils.getHttpServer()+"\r\n";
            }
        };   
}
