package com.netflix.governator.lifecycle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;

public class ClasspathUrlDecoder {
    public static File toFile(URL url)
    {
        if ( !"file".equals(url.getProtocol()) &&  !"vfs".equals(url.getProtocol()) )
        {
            throw new IllegalArgumentException("not a file or vfs url: " + url);
        }
        String path = url.getFile();
        File dir = new File(decode(path));
        if (dir.getName().equals("META-INF")) {
            dir = dir.getParentFile(); // Scrape "META-INF" off
        }
        return dir;
    }

    public static String decode(String fileName)
    {
        if ( fileName.indexOf('%') == -1 )
        {
            return fileName;
        }

        StringBuilder result = new StringBuilder(fileName.length());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < fileName.length();) {
            char c = fileName.charAt(i);

            if (c == '%') {
                out.reset();
                do {
                    if (i + 2 >= fileName.length()) {
                        throw new IllegalArgumentException("Incomplete % sequence at: " + i);
                    }

                    int d1 = Character.digit(fileName.charAt(i + 1), 16);
                    int d2 = Character.digit(fileName.charAt(i + 2), 16);

                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException("Invalid % sequence (" + fileName.substring(i, i + 3) + ") at: " + String.valueOf(i));
                    }

                    out.write((byte) ((d1 << 4) + d2));

                    i += 3;

                } while (i < fileName.length() && fileName.charAt(i) == '%');


                result.append(out.toString());

                continue;
            } else {
                result.append(c);
            }

            i++;
        }
        return result.toString();
    }
}
