package com.netflix.governator.lifecycle;

import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Type.*;

public final class AnnotationFinder extends ClassVisitor {
	private static Logger log = LoggerFactory.getLogger(AnnotationFinder.class);
	
    private Set<Type> annotationTypes;

    private Set<Class<?>> annotatedClasses = Collections.emptySet();
    private Set<Method> annotatedMethods = new HashSet<>();
    private Set<Constructor> annotatedConstructors = new HashSet<>();
    private Set<Field> annotatedFields = new HashSet<>();

    private String className;
    private Class<?> clazz;
    private ClassLoader classLoader;

    private Class<?> selfClass() {
        if(clazz == null)
            clazz = classFromInternalName(className);
        return clazz;
    }

    private Class<?> classFromInternalName(String name) {
        try {
            return Class.forName(name.replace('/', '.'), false, classLoader);
        } 
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public AnnotationFinder(ClassLoader classLoader, Collection<Class<? extends Annotation>> annotations) {
        super(Opcodes.ASM5);
        annotationTypes = new HashSet<>();
        for (Class<?> annotation : annotations)
            annotationTypes.add(getType(annotation));
        this.classLoader = classLoader;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        Type type = getType(desc);
        for (Type annotationType : annotationTypes)  {
            if (annotationType.equals(type)) {
                annotatedClasses = Collections.<Class<?>>singleton(selfClass());
                break;
            }
        }

        return super.visitAnnotation(desc, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return new AnnotationSeekingFieldVisitor(name, super.visitField(access, name, desc, signature, value));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new AnnotationSeekingMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), name, desc);
    }

    private class AnnotationSeekingFieldVisitor extends FieldVisitor {
        String name;

        public AnnotationSeekingFieldVisitor(String name, FieldVisitor fv) {
            super(Opcodes.ASM5, fv);
            this.name = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            Type type = getType(desc);
            for (Type annotationType : annotationTypes) {
                if (annotationType.equals(type)) {
                    try {
                        annotatedFields.add(selfClass().getDeclaredField(name));
                        break;
                    } 
                    catch (NoSuchFieldException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }

            return super.visitAnnotation(desc, visible);
        }
    }

    private class AnnotationSeekingMethodVisitor extends MethodVisitor {
        String name;
        String methodDesc;

        public AnnotationSeekingMethodVisitor(MethodVisitor mv, String name, String desc) {
            super(Opcodes.ASM5, mv);
            this.name = name;
            this.methodDesc = desc;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            Type type = getType(desc);
            for (Type annotationType : annotationTypes) {
                if (annotationType.equals(type)) {
                    Type[] args = methodDesc == null ? new Type[0]
                            : getArgumentTypes(methodDesc);
                    Class[] argClasses = new Class[args.length];
                    for (int i = 0; i < args.length; i++) {
                        switch (args[i].getSort()) {
                        case OBJECT:
                        case ARRAY:
                            argClasses[i] = classFromInternalName(args[i]
                                    .getInternalName());
                            break;
                        case BOOLEAN:
                            argClasses[i] = boolean.class;
                            break;
                        case BYTE:
                            argClasses[i] = byte.class;
                            break;
                        case CHAR:
                            argClasses[i] = char.class;
                            break;
                        case DOUBLE:
                            argClasses[i] = double.class;
                            break;
                        case FLOAT:
                            argClasses[i] = float.class;
                            break;
                        case INT:
                            argClasses[i] = int.class;
                            break;
                        case LONG:
                            argClasses[i] = long.class;
                            break;
                        case SHORT:
                            argClasses[i] = short.class;
                            break;
                        }
                    }

                    try {
                        if ("<init>".equals(name))
                            annotatedConstructors.add(selfClass()
                                    .getDeclaredConstructor(argClasses));
                        else
                            annotatedMethods.add(selfClass().getDeclaredMethod(
                                    name, argClasses));
                    } 
                    catch (NoClassDefFoundError e) {
                    	log.info("Unable to scan constructor of '{}' NoClassDefFoundError looking for '{}'", selfClass().getName(), e.getMessage());
                    }
                    catch (NoSuchMethodException e) {
                        throw new IllegalStateException(e);
                    }

                    break;
                }
            }

            return super.visitAnnotation(desc, visible);
        }
    }

    /**
     * @return a 0 or 1 element Set, depending on whether the class being
     *         visited has a matching class annotation
     */
    public Set<Class<?>> getAnnotatedClasses() {
        return annotatedClasses;
    }

    public Set<Method> getAnnotatedMethods() {
        return annotatedMethods;
    }

    public Set<Constructor> getAnnotatedConstructors() {
        return annotatedConstructors;
    }

    public Set<Field> getAnnotatedFields() {
        return annotatedFields;
    }
}
