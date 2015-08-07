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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates files within a directory-based classpath resource that are contained with a particular base package.
 */
class DirectoryClassFilter
{
    private final ClassLoader loader;

    DirectoryClassFilter(ClassLoader loader) {
        this.loader = loader;
    }

    public List<String> filesInPackage(URL url, String basePackage) {
        File dir = ClasspathUrlDecoder.toFile(url);
        List<String> classNames = new ArrayList<String>();
        if (dir.isDirectory()) {
            scanDir(dir, classNames, (basePackage.length() > 0) ? (basePackage + ".") : "");
        }
        return classNames;
    }

    public InputStream bytecodeOf(String className) throws IOException {
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
        throw new IOException("Unable to open class with name " + className + " because the class loader was unable to locate it");
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
                if (name.contains(".")) {
                    continue;
                }
                classNames.add(packageName + name);
            }
        }
    }
}
