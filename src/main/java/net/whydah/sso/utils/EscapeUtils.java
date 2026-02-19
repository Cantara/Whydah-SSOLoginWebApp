package net.whydah.sso.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EscapeUtils {
    private static final Logger log = LoggerFactory.getLogger(EscapeUtils.class);

    public static final HashMap m = new HashMap();

    static {
        m.put(34, "&quot;"); // < - less-than
        m.put(60, "&lt;");   // < - less-than
        m.put(62, "&gt;");   // > - greater-than
        //User needs to map all html entities with their corresponding decimal values.
        //Please refer to below table for mapping of entities and integer value of a char
    }

    public static String escapeHtml(String s) {
        String str = "<script>alert(\"abc\")</script>";
        try {
            StringWriter writer = new StringWriter((int)
                    (s.length() * 1.5));
            escape(writer, s);
            // log.trace("encoded string is " + writer.toString() );
            return writer.toString();
        } catch (IOException ioe) {
            log.warn("Unable to escapeHTML", ioe);
            return null;
        }
    }

    public static void escape(Writer writer, String str) throws IOException {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            int ascii = (int) c;
            String entityName = (String) m.get(ascii);
            if (entityName == null) {
                if (c > 0x7F) {
                    writer.write("&#");
                    writer.write(Integer.toString(c, 10));
                    writer.write(';');
                } else {
                    writer.write(c);
                }
            } else {
                writer.write(entityName);
            }
        }
    }
}