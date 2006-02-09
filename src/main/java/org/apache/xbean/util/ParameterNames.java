/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Implementation of ParameterNameDiscover that uses the
 * LocalVariableTable information in the method attributes
 * to discover parameter names.
 * <p/>
 * Returns null if the class file was compiled without debug
 * information.
 *
 * This wonderful piece of code was taken from org.springframework.core.LocalVariableTableParameterNameDiscover
 *
 * @author Adrian Colyer
 * @since 2.0
 */
public final class ParameterNames {
    /**
     * Gets the names of the method parameters or null if the class was compiles without debug symbols on.
     * @param method the method for which the parameters should be retrieved
     * @return the parameter names or null if the class was compiles without debug symbols on
     */
    public static String[] get(Method method) {
        try {
            Class declaringClass = method.getDeclaringClass();
            ClassReader reader = createClassReader(declaringClass);
            ParameterNameDiscoveringVisitor visitor = new ParameterNameDiscoveringVisitor(method);
            reader.accept(visitor, false);
            return visitor.getParameterNames();
        } catch (IOException ex) {
        }
        return null;
    }

    /**
     * Gets the names of the constructor parameters or null if the class was compiles without debug symbols on.
     * @param constructor the constructor for which the parameters should be retrieved
     * @return the parameter names or null if the class was compiles without debug symbols on
     */
    public static String[] get(Constructor constructor) {
        try {
            Class declaringClass = constructor.getDeclaringClass();
            ClassReader reader = createClassReader(declaringClass);
            ParameterNameDiscoveringVisitor visitor = new ParameterNameDiscoveringVisitor(constructor);
            reader.accept(visitor, false);
            return visitor.getParameterNames();
        } catch (IOException ex) {
        }
        return null;
    }

    private static ClassReader createClassReader(Class declaringClass) throws IOException {
        InputStream in = null;
        try {
            ClassLoader classLoader = declaringClass.getClassLoader();
            in = classLoader.getResourceAsStream(declaringClass.getName().replace('.', '/') + ".class");
            ClassReader reader = new ClassReader(in);
            return reader;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static class ParameterNameDiscoveringVisitor extends EmptyVisitor {
        private final String methodName;
        private final boolean isConstructor;
        private final String[] parameterNames;
        private final String descriptor;

        public ParameterNameDiscoveringVisitor(Method method) {
            this.methodName = method.getName();
            this.isConstructor = false;
            this.parameterNames = new String[method.getParameterTypes().length];
            this.descriptor = Type.getMethodDescriptor(method);
        }

        public ParameterNameDiscoveringVisitor(Constructor constructor) {
            this.methodName = "<init>";
            this.isConstructor = true;
            this.parameterNames = new String[constructor.getParameterTypes().length];

            Type[] pTypes = new Type[constructor.getParameterTypes().length];
            for (int i = 0; i < pTypes.length; i++) {
                pTypes[i] = Type.getType(constructor.getParameterTypes()[i]);
            }
            this.descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, pTypes);
        }

        public String[] getParameterNames() {
            // if we didn't find the method or if the class was not compiled
            // with debug symbols the parameter names will null
            if (parameterNames[0] == null) {
                return null;
            }

            return parameterNames;
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (!name.equals(this.methodName) || !desc.equals(this.descriptor)) {
                return null;
            }
            return new EmptyVisitor() {
                public void visitLocalVariable(String name, String description, String signature, Label start, Label end, int index) {
                    if (!isConstructor) {
                        parameterNames[index] = name;
                    } else if (index > 0) {
                        parameterNames[(index -1)] = name;
                    }
                }
            };
        }
    }
}