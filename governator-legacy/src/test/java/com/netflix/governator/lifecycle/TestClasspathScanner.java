package com.netflix.governator.lifecycle;

import com.beust.jcommander.internal.Lists;
import com.google.common.io.Resources;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestClasspathScanner {

    @Test
    public void classpathScannerFindsClassesInJarFiles() throws URISyntaxException, MalformedURLException {
        URL jarFile = Resources.getResource("classpathScannerTestJar").toURI().toURL();
        Collection<Class<? extends Annotation>> annotations = Lists.newArrayList();
        annotations.add(Inherited.class);
        ClasspathScanner classpathScanner = new ClasspathScanner(newArrayList("com.netflix.governator.test"), annotations, new URLClassLoader(new URL[]{jarFile}));

        assertThat(getOnlyElement(classpathScanner.getClasses()).getSimpleName(), equalTo("ClasspathScannerTestClass"));
    }
}