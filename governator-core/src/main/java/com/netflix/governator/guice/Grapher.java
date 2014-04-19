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

package com.netflix.governator.guice;

import com.google.common.io.Closeables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.grapher.GrapherModule;
import com.google.inject.grapher.InjectorGrapher;
import com.google.inject.grapher.graphviz.GraphvizModule;
import com.google.inject.grapher.graphviz.GraphvizRenderer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;


/**
 * An object that can generate a graph showing a Guice Dependency Injection graph.
 *
 * @see <a href="http://www.graphviz.org/">GraphViz</a>
 * @see <a href="http://code.google.com/p/google-guice/wiki/Grapher">Guice Grapher</a>
 * @see <a href="http://code.google.com/p/jrfonseca/wiki/XDot">XDot, an interactive viewer for Dot files</a>
 * @author $Author: slanning $
 * @version $Revision: #1 $
 */
public class Grapher
{
    private final Injector injector;

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
            Closeables.closeQuietly(out);
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
        Injector localInjector = Guice.createInjector(new GrapherModule(), new GraphvizModule());
        GraphvizRenderer renderer = localInjector.getInstance(GraphvizRenderer.class);
        renderer.setOut(out).setRankdir("TB");
        localInjector.getInstance(InjectorGrapher.class).of(injector).graph();
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
      s = s.replaceAll("\\w[a-z\\d_\\.]+\\.([A-Z][A-Za-z\\d_]*)", "");
      s = s.replaceAll("value=[\\w-]+", "random");
      return s;
   }

    private String fixGrapherBug(String s) {
      s = s.replaceAll("style=invis", "style=solid");
      return s;
   }

} // Grapher
