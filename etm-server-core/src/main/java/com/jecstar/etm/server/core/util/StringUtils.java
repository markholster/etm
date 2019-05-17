package com.jecstar.etm.server.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public final class StringUtils {

    public static String escapeToHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // HTML Special Chars
            if (c == '"')
                result.append("&quot;");
            else if (c == '&')
                result.append("&amp;");
            else if (c == '<')
                result.append("&lt;");
            else if (c == '>')
                result.append("&gt;");
            else if (c == '\n')
                // Handle Newline
                result.append("&lt;br/&gt;");
            else {
                int ci = 0xffff & c;
                if (ci < 160)
                    // nothing special only 7 Bit
                    result.append(c);
                else {
                    // Not 7 Bit use the unicode system
                    result.append("&#");
                    result.append(Integer.toString(ci));
                    result.append(';');
                }
            }
        }
        return result.toString();
    }

    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String urlDencode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
