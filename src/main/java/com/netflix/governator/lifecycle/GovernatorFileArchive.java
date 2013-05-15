/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.lifecycle;

import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ArchiveIterator;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * My workaround until https://issues.apache.org/jira/browse/XBEAN-207 is fixed
 */
class GovernatorFileArchive implements Archive
{
    private final ClassLoader loader;
    private final File dir;
    private final String basePackage;
    private List<String> list;

    GovernatorFileArchive(ClassLoader loader, URL url, String basePackage) {
        this.loader = loader;
        this.basePackage = basePackage;
        this.dir = toFile(url);
    }

    public File getDir() {
        return dir;
    }

    public InputStream getBytecode(String className) throws IOException, ClassNotFoundException {
        int pos = className.indexOf("<");
        if (pos > -1) {
            className = className.substring(0, pos);
        }
        pos = className.indexOf(">");
        if (pos > -1) {
            className = className.substring(0, pos);
        }
        if (!className.endsWith(".class")) {
            className = className.replace('.', '/') + ".class";
        }

        URL resource = loader.getResource(className);
        if ( resource != null )
        {
            return new BufferedInputStream(resource.openStream());
        }

        throw new ClassNotFoundException(className);
    }


    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loader.loadClass(className);
    }

    public Iterator<Entry> iterator() {
        return new ArchiveIterator(this, _iterator());
    }

    public Iterator<String> _iterator() {
        if ( list != null )
        {
            return list.iterator();
        }

        list = file(dir);
        return list.iterator();
    }

    private List<String> file(File dir) {
        List<String> classNames = new ArrayList<String>();
        if (dir.isDirectory()) {
            scanDir(dir, classNames, (basePackage.length() > 0) ? (basePackage + ".") : "");
        }
        return classNames;
    }

    @SuppressWarnings("ConstantConditions")
    private void scanDir(File dir, List<String> classNames, String packageName) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                scanDir(file, classNames, packageName + file.getName() + ".");
            } else if (file.getName().endsWith(".class")) {
                String name = file.getName();
                name = name.replaceFirst(".class$", "");
                if ( name.contains(".") )
                {
                    continue;
                }
                classNames.add(packageName + name);
            }
        }
    }

    private static File toFile(URL url) {
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

    public static String decode(String fileName) {
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
