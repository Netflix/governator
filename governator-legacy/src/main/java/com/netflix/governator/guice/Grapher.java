/*
 * Copyright 2013, 2014 Netflix, Inc.
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

package com.netflix.governator.guice;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.grapher.graphviz.GraphvizGrapher;
import com.google.inject.grapher.graphviz.GraphvizModule;

/**
 * An object that can generate a graph showing a Guice Dependency Injection graph.
 *
 * @see <a href="http://www.graphviz.org/">GraphViz</a>
 * @see <a href="http://code.google.com/p/google-guice/wiki/Grapher">Guice Grapher</a>
 * @see <a href="http://code.google.com/p/jrfonseca/wiki/XDot">XDot, an interactive viewer for Dot files</a>
 */
public class Grapher
{
    private final Injector injector;
    private final Set<Key<?>> roots;

    /*
     * Constructors
     */

    /**
     * Creates a new Grapher.
     *
     * @param injector the Injector whose dependency graph will be generated
     */
    @Inject
    public Grapher(Injector injector) {
        this.injector = injector;
        this.roots = new HashSet<>();
    }

    /**
     * Creates a new Grapher.
     *
     * @param injector the Injector whose dependency graph will be generated
     * @param keys {@code Key}s for the roots of the graph
     */
    public Grapher(Injector injector, Key<?>... keys) {
        this.injector = injector;
        this.roots = Sets.newHashSet(keys);
    }

    /**
     * Creates a new Grapher.
     *
     * @param injector the Injector whose dependency graph will be generated
     * @param classes {@code Class}es for the roots of the graph
     */
    public Grapher(Injector injector, Class<?>... classes) {
        this.injector = injector;
        this.roots = Sets.newHashSetWithExpectedSize(classes.length);
        for (Class<?> cls : classes) {
            roots.add(Key.get(cls));
        }
    }

    /**
     * Creates a new Grapher.
     *
     * @param injector the Injector whose dependency graph will be generated
     * @param packages names of {@code Package}s for the roots of the graph
     */
    public Grapher(Injector injector, String... packages) {
        this.injector = injector;
        // Scan all the injection bindings to find the root keys
        this.roots = new HashSet<Key<?>>();
        for (Key<?> k : injector.getAllBindings().keySet()) {
            Package classPackage = k.getTypeLiteral().getRawType().getPackage();
            if (classPackage == null) {
                continue;
            }
            String packageName = classPackage.getName();
            for (String p : packages) {
                if (packageName.startsWith(p)) {
                    roots.add(k);
                    break;
                }
            }
        }
    }

    /*
     * Graphing methods
     */

    /**
     * Writes the "Dot" graph to a new temp file.
     *
     * @return the name of the newly created file
     */
    public String toFile() throws Exception {
        File file = File.createTempFile("GuiceDependencies_", ".dot");
        toFile(file);
        return file.getCanonicalPath();
    }

    /**
     * Writes the "Dot" graph to a given file.
     *
     * @param file file to write to
     */
    public void toFile(File file) throws Exception {
        PrintWriter out = new PrintWriter(file, "UTF-8");
        try {
            out.write(graph());
        }
        finally {
            Closeables.close(out, true);
        }
    }

    /**
     * Returns a String containing the "Dot" graph definition.
     *
     * @return the "Dot" graph definition
     */
    public String graph() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        Injector localInjector = Guice.createInjector(new GraphvizModule());
        GraphvizGrapher renderer = localInjector.getInstance(GraphvizGrapher.class);
        renderer.setOut(out);
        renderer.setRankdir("TB");
        if (roots != null) {
            renderer.graph(injector, roots);
        }
        renderer.graph(injector);
        return fixupGraph(baos.toString("UTF-8"));
    }

    /*
     * Work-arounds for bugs in the Grapher package
     * See http://stackoverflow.com/questions/9301007/is-there-any-way-to-get-guice-grapher-to-work
     */

    private String fixupGraph(String s) {
        s = fixGrapherBug(s);
        s = hideClassPaths(s);
        return s;
    }

    private String hideClassPaths(String s) {
      s = s.replaceAll("\\w[a-z\\d_\\.]+\\.([A-Z][A-Za-z\\d_\\$]*)", "$1");
      s = s.replaceAll("value=[\\w-]+", "random");
      return s;
   }

    private String fixGrapherBug(String s) {
      s = s.replaceAll("style=invis", "style=solid");
      s = s.replaceAll("margin=(\\S+), ", "margin=\"$1\", ");
      return s;
   }

} // Grapher
