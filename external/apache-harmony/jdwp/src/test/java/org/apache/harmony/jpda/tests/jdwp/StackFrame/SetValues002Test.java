/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.jpda.tests.jdwp.StackFrame;

import org.apache.harmony.jpda.tests.framework.jdwp.Value;

/**
 * JDWP Unit test for StackFrame.SetValues command.
 */
public class SetValues002Test extends JDWPStackFrameAccessTest {
    /**
     * Tests we correctly write value of boolean variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues001_Boolean() {
        StackFrameTester tester = new StackFrameTester("breakpointBoolean",
                StackTrace002Debuggee.BOOLEAN_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointBoolean");
        methodInfo.addVariable("param", new Value(StackTrace002Debuggee.BOOLEAN_PARAM_VALUE),
                new Value(StackTrace002Debuggee.BOOLEAN_PARAM_VALUE_TO_SET));
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of byte variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues002_Byte() {
        StackFrameTester tester = new StackFrameTester("breakpointByte",
                StackTrace002Debuggee.BYTE_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointByte");
        methodInfo.addVariable("param", new Value(StackTrace002Debuggee.BYTE_PARAM_VALUE),
                new Value(StackTrace002Debuggee.BYTE_PARAM_VALUE_TO_SET));
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of char variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues003_Char() {
        StackFrameTester tester = new StackFrameTester("breakpointChar",
                StackTrace002Debuggee.CHAR_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointChar");
        methodInfo.addVariable("param", new Value(StackTrace002Debuggee.CHAR_PARAM_VALUE),
                new Value(StackTrace002Debuggee.CHAR_PARAM_VALUE_TO_SET));
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of short variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues004_Short() {
        StackFrameTester tester = new StackFrameTester("breakpointShort",
                StackTrace002Debuggee.SHORT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointShort");
        methodInfo.addVariable("param", new Value(StackTrace002Debuggee.SHORT_PARAM_VALUE),
                new Value(StackTrace002Debuggee.SHORT_PARAM_VALUE_TO_SET));
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of int variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues005_Int() {
        StackFrameTester tester = new StackFrameTester("breakpointInt",
                StackTrace002Debuggee.INT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointInt");
        methodInfo.addVariable("param", new Value(StackTrace002Debuggee.INT_PARAM_VALUE),
                new Value(StackTrace002Debuggee.INT_PARAM_VALUE_TO_SET));
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of long variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues006_Long() {
        StackFrameTester tester = new StackFrameTester("breakpointLong",
                StackTrace002Debuggee.LONG_METHOD_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointLong");
        methodInfo.addVariable("param", new Value(StackTrace002Debuggee.LONG_PARAM_VALUE),
                new Value(StackTrace002Debuggee.LONG_PARAM_VALUE_TO_SET));
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of float variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues007_Float() {
        StackFrameTester tester = new StackFrameTester("breakpointFloat",
                StackTrace002Debuggee.FLOAT_METHOD);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointFloat");
        methodInfo.addVariable("param", new Value(StackTrace002Debuggee.FLOAT_PARAM_VALUE),
                new Value(StackTrace002Debuggee.FLOAT_PARAM_VALUE_TO_SET));
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of double variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues008_Double() {
        StackFrameTester tester = new StackFrameTester("breakpointDouble",
                StackTrace002Debuggee.DOUBLE_METHOD);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointDouble");
        methodInfo.addVariable("param", new Value(StackTrace002Debuggee.DOUBLE_PARAM_VALUE),
                new Value(StackTrace002Debuggee.DOUBLE_PARAM_VALUE_TO_SET));
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.Object variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues009_Object() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "OBJECT_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "OBJECT_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointObject", StackTrace002Debuggee.OBJECT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointObject");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of Array variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues010_Array() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "ARRAY_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "ARRAY_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointArray", StackTrace002Debuggee.ARRAY_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointArray");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of Array into a local variable declared as
     * java.lang.Object.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues010_ArrayAsObject() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "ARRAY_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "ARRAY_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointObject", StackTrace002Debuggee.ARRAY_AS_OBJECT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointObject");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.Class variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues011_Class() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "CLASS_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "CLASS_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointClass", StackTrace002Debuggee.CLASS_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointClass");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.Class into a local variable declared as
     * java.lang.Object.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues011_ClassAsObject() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "CLASS_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "CLASS_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointObject", StackTrace002Debuggee.CLASS_AS_OBJECT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointObject");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.ClassLoader variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues012_ClassLoader() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "CLASS_LOADER_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "CLASS_LOADER_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointClassLoader", StackTrace002Debuggee.CLASS_LOADER_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointClassLoader");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.ClassLoader into a local variable declared as
     * java.lang.Object.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues012_ClassLoaderAsObject() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "CLASS_LOADER_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "CLASS_LOADER_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointObject", StackTrace002Debuggee.CLASS_LOADER_AS_OBJECT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointObject");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.String variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues013_String() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "STRING_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "STRING_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointString", StackTrace002Debuggee.STRING_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointString");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.String into a local variable declared as
     * java.lang.Object.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues013_StringAsObject() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "STRING_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "STRING_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointObject", StackTrace002Debuggee.STRING_AS_OBJECT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointObject");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.Thread variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues014_Thread() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "THREAD_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "THREAD_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointThread", StackTrace002Debuggee.THREAD_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointThread");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.Thread into a local variable declared as
     * java.lang.Object.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues014_ThreadAsObject() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "THREAD_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "THREAD_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointObject", StackTrace002Debuggee.THREAD_AS_OBJECT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointObject");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.ThreadGroup variable into the stack.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues015_ThreadGroup() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "THREAD_GROUP_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "THREAD_GROUP_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointThreadGroup", StackTrace002Debuggee.THREAD_GROUP_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointThreadGroup");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }

    /**
     * Tests we correctly write value of java.lang.ThreadGroup into a local variable declared as
     * java.lang.Object.
     *
     * Refer to {@link JDWPStackFrameAccessTest#runStackFrameTest(StackFrameTester)}
     * method for the sequence of the test.
     */
    public void testSetValues015_ThreadGroupAsObject() {
        long classID = getClassIDBySignature(getDebuggeeClassSignature());
        Value actualValue = getStaticFieldValue(classID,
                "THREAD_GROUP_PARAM_VALUE");
        Value expectedValue = getStaticFieldValue(classID,
                "THREAD_GROUP_PARAM_VALUE_TO_SET");

        StackFrameTester tester = new StackFrameTester(
                "breakpointObject", StackTrace002Debuggee.THREAD_GROUP_AS_OBJECT_SIGNAL);
        MethodInfo methodInfo = tester.addTestMethod("runBreakpointObject");
        methodInfo.addVariable("param", actualValue, expectedValue);
        runStackFrameTest(tester);
    }
}
