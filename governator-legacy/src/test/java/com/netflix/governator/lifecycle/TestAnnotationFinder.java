package com.netflix.governator.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.objectweb.asm.ClassReader.SKIP_CODE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.hamcrest.Matchers;
import org.objectweb.asm.ClassReader;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestAnnotationFinder {
    JavaClasspath cp;

    String a = "governator.test.A";
    String b = "governator.test.B";

    @BeforeMethod
    public void before()
    {
        cp = new JavaClasspath();
        cp.compile(
                "package governator.test;" +
                        "public @interface A {}",

                "package governator.test;" +
                        "public @interface B {}");
    }

    @AfterMethod
    public void after()
    {
        cp.cleanup();
    }

    @Test
    public void testFindAnnotationsOnClasses()
    {
        cp.compile(
            "package governator.test;" +
            "@A public class Foo {}"
        );

        assertThat(scan("governator.test.Foo", a).getAnnotatedClasses(), is(Matchers.<Class<?>>iterableWithSize(1)));
        assertThat(scan("governator.test.Foo", b).getAnnotatedClasses(), is(emptyIterable()));
    }

    @Test
    public void testFindAnnotationsOnFields()
    {
        cp.compile(
            "package governator.test;" +
            "public class Foo {" +
            "   @A private String f1;" +
            "   @A transient String f2;" +
            "   @A static String f3;" +
            "   @A static int f4;" +
            "}"
        );

        AnnotationFinder finder = scan("governator.test.Foo", a);
        Set<String> matchingFields = new TreeSet<>();
        for (Field field : finder.getAnnotatedFields())
            matchingFields.add(field.getName());

        assertThat(matchingFields, containsInAnyOrder("f1", "f2", "f3", "f4"));
    }

    @Test
    public void testFindAnnotationsOnMethods()
    {
        cp.compile(
            "package governator.test;" +
            "import java.util.Collection;" +
            "public class Foo {" +
            "   @A private void m1() {}" +
            "   @A void m2(String p) {}" +
            "   @A void m3(String[] p) {}" +
            "   @A void m4(Collection<String> p) {}" +
            "   @A void m5(int p) {}" +
            "}"
        );

        AnnotationFinder finder = scan("governator.test.Foo", a);

        Set<String> matchingMethods = new TreeSet<>();
        for (Method method : finder.getAnnotatedMethods())
            matchingMethods.add(method.getName());

        assertThat(matchingMethods, containsInAnyOrder("m1", "m2", "m3", "m4", "m5"));
    }

    @Test
    public void testFindAnnotationsOnConstructors()
    {
        cp.compile(
            "package governator.test;" +
            "import java.util.Collection;" +
            "public class Foo {" +
            "   @A private Foo() {}" +
            "   @A Foo(String p) {}" +
            "   @A Foo(String[] p) {}" +
            "   @A Foo(Collection<String> p) {}" +
            "   @A Foo(byte p) {}" +
            "}"
        );

        assertThat(scan("governator.test.Foo", a).getAnnotatedConstructors(), is(Matchers.<Constructor>iterableWithSize(5)));
    }

    @Test
    public void testInnerClasses()
    {
        Collection<String> compile = cp.compile(
            "package governator.test;" +
            "public class Foo {" +
            "   @A class Inner {}" +
            "   @A static class StaticInner {}" +
            "}"
        );


        assertThat(scan("governator.test.Foo$Inner", a).getAnnotatedClasses(),
                is(Matchers.<Class<?>>iterableWithSize(1)));

        assertThat(scan("governator.test.Foo$StaticInner", a).getAnnotatedClasses(),
                is(Matchers.<Class<?>>iterableWithSize(1)));
    }

    private AnnotationFinder scan(String clazz, String annotation) {
        Class<? extends Annotation> aClass = cp.loadClass(annotation);
        AnnotationFinder finder = new AnnotationFinder(cp.getClassLoader(), Collections.<Class<? extends Annotation>>singletonList(aClass));
        new ClassReader(cp.classBytes(clazz)).accept(finder, SKIP_CODE);
        return finder;
    }
}
