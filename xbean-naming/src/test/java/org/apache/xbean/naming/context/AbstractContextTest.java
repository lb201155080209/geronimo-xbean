/**
 *
 * Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.naming.context;

import junit.framework.TestCase;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.OperationNotSupportedException;
import java.util.Map;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractContextTest extends TestCase {
    public static void assertEq(Map expected, Context actual) throws NamingException {
        AbstractContextTest.assertEq(ContextUtil.buildMapTree(expected), actual, actual, null);
    }

    public static void assertEq(Map expected, String pathInExpected, Context actual) throws NamingException {
        ContextUtil.Node node = ContextUtil.buildMapTree(expected);
        Name parsedName = actual.getNameParser("").parse(pathInExpected);
        for (int i = 0; i < parsedName.size(); i++) {
            String part = parsedName.get(i);
            Object value = node.get(part);
            if (value == null) {
                throw new NamingException("look for " + parsedName.getPrefix(i+1) + " in node tree is null ");
            }
            node = (ContextUtil.Node) value;
        }

        AbstractContextTest.assertEq(node, actual, actual, null);
    }

    private static void assertEq(ContextUtil.Node node, Context rootContext, Context currentContext, String path) throws NamingException {
        for (Iterator iterator = node.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String expectedName = (String) entry.getKey();
            Object expectedValue = entry.getValue();

            String fullName = path == null ? expectedName : path + "/" + expectedName;

            // verify we can lookup by string name and parsed name using the root context and current context
            Object value = AbstractContextTest.assertLookup(expectedValue, currentContext, expectedName);
            Object absoluteValue = AbstractContextTest.assertLookup(expectedValue, rootContext, fullName);
            assertSame(fullName, value, absoluteValue);

            if (expectedValue instanceof ContextUtil.Node) {
                ContextUtil.Node expectedNode = (ContextUtil.Node) expectedValue;

                // verufy listing of this context returns the expected results
                AbstractContextTest.assertList(expectedNode, currentContext, expectedName);
                AbstractContextTest.assertList(expectedNode, rootContext, fullName);

                AbstractContextTest.assertEq(expectedNode, rootContext, (Context) value, fullName);
            }
        }
    }

    public static Object assertLookup(Object expectedValue, Context context, String name) throws NamingException {
        Object value = context.lookup(name);

        String contextName = context.getNameInNamespace();
        if (contextName == null || contextName.length() == 0) contextName = "<root>";

        assertNotNull("lookup of " +  name + " on " + contextName + " returned null", value);

        if (expectedValue instanceof ContextUtil.Node) {
            assertTrue("Expected lookup of " +  name + " on " + contextName + " to return a Context, but got a " + value.getClass().getName(),
                    value instanceof Context);
        } else {
            assertEquals("lookup of " + name + " on " + contextName, expectedValue, value);
        }

        Name parsedName = context.getNameParser("").parse(name);
        Object valueFromParsedName = context.lookup(parsedName);
        assertSame("lookup of " +  name + " on " + contextName + " using a parsed name", value, valueFromParsedName);

        return value;
    }

    public static void assertList(ContextUtil.Node node, Context context, String name) throws NamingException {
        String contextName = context.getNameInNamespace();
        if (contextName == null || contextName.length() == 0) contextName = "<root>";

        AbstractContextTest.assertListResults(node, context.list(name), contextName, name, false);
        AbstractContextTest.assertListResults(node, context.listBindings(name), contextName, name, true);

        Name parsedName = context.getNameParser("").parse(name);
        AbstractContextTest.assertListResults(node, context.list(parsedName), contextName, "parsed name " + name, false);
        AbstractContextTest.assertListResults(node, context.listBindings(parsedName), contextName, "parsed name " + name, true);
    }

    public static void assertListResults(ContextUtil.Node node, NamingEnumeration enumeration, String contextName, String name, boolean wasListBinding) {
        Map actualValues;
        if (wasListBinding) {
            actualValues = ContextUtil.listBindingsToMap(enumeration);
        } else {
            actualValues = ContextUtil.listToMap(enumeration);
        }

        for (Iterator iterator = node.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String expectedName = (String) entry.getKey();
            Object expectedValue = entry.getValue();

            Object actualValue = actualValues.get(expectedName);

            assertNotNull("list of " + name + " on " + contextName + " did not find value for " + name, actualValue);
            if (wasListBinding) {
                if (expectedValue instanceof ContextUtil.Node) {
                    assertTrue("Expected list of " + name + " on " + contextName + " result value for " + expectedName + " to return a Context, but got a " + actualValue.getClass().getName(),
                        actualValue instanceof Context);
                } else {
                    assertEquals("list of " + name + " on " + contextName + " for value for " + expectedName, expectedValue, actualValue);
                }
            } else {
                if (!(expectedValue instanceof ContextUtil.Node)) {
                    assertEquals("list of " + name + " on " + contextName + " for value for " + expectedName, expectedValue.getClass().getName(), actualValue);
                } else {
                    // can't really test this since it the value is the name of a nested node class
                }
            }
        }

        TreeSet extraNames = new TreeSet(actualValues.keySet());
        extraNames.removeAll(node.keySet());
        if (!extraNames.isEmpty()) {
            fail("list of " + name + " on " + contextName + " did not find values: " + extraNames);
        }
    }

    public static void assertUnmodifiable(Context context) throws Exception {
        Object value = "VALUE";
        String nameString = "TEST_NAME";
        Name name = context.getNameParser("").parse(nameString);

        //
        // bind
        //
        try {
            context.bind(nameString, value);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        try {
            context.bind(name, value);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        //
        // rebind
        //
        try {
            context.rebind(nameString, value);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        try {
            context.rebind(name, value);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        //
        // rename
        //
        String newNameString = "NEW_TEST_NAME";
        Name newName = context.getNameParser("").parse(newNameString);
        try {
            context.rename(nameString, newNameString);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        try {
            context.rename(name, newName);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        //
        // unbind
        //
        try {
            context.unbind(nameString);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        try {
            context.unbind(name);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        //
        // createSubcontext
        //
        try {
            context.createSubcontext(nameString);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        try {
            context.createSubcontext(name);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        //
        // destroySubcontext
        //
        try {
            context.destroySubcontext(nameString);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }

        try {
            context.destroySubcontext(name);
            fail("Expected an OperationNotSupportedException");
        } catch(OperationNotSupportedException expected) {
        }
    }
}
