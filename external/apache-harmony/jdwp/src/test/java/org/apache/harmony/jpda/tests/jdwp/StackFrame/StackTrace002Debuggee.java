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

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Debuggee for GetValues002Test and SetValues002Test.
 */
public class StackTrace002Debuggee extends SyncDebuggee {
    // Signals to select which method the debuggee needs to call.
    static final String BOOLEAN_SIGNAL = "runBreakpointBoolean";
    static final String BYTE_SIGNAL = "runBreakpointByte";
    static final String CHAR_SIGNAL = "runBreakpointChar";
    static final String SHORT_SIGNAL = "runBreakpointShort";
    static final String INT_SIGNAL = "runBreakpointInt";
    static final String INT_METHOD2_SIGNAL = "runBreakpointInt2";
    static final String LONG_METHOD_SIGNAL = "runBreakpointLong";
    static final String FLOAT_METHOD = "runBreakpointFloat";
    static final String DOUBLE_METHOD = "runBreakpointDouble";
    static final String OBJECT_SIGNAL = "runBreakpointObject";
    static final String ARRAY_SIGNAL = "runBreakpointArray";
    static final String CLASS_SIGNAL = "runBreakpointClass";
    static final String CLASS_LOADER_SIGNAL = "runBreakpointClassLoader";
    static final String STRING_SIGNAL = "runBreakpointString";
    static final String THREAD_SIGNAL = "runBreakpointThread";
    static final String THREAD_GROUP_SIGNAL = "runBreakpointThreadGroup";
    static final String ARRAY_AS_OBJECT_SIGNAL = "runBreakpointArrayAsObject";
    static final String CLASS_AS_OBJECT_SIGNAL = "runBreakpointClassAsObject";
    static final String CLASS_LOADER_AS_OBJECT_SIGNAL = "runBreakpointClassLoaderAsObject";
    static final String STRING_AS_OBJECT_SIGNAL = "runBreakpointStringAsObject";
    static final String THREAD_AS_OBJECT_SIGNAL = "runBreakpointThreadAsObject";
    static final String THREAD_GROUP_AS_OBJECT_SIGNAL = "runBreakpointThreadGroupAsObject";

    // Values used to check StackFrame.GetValues.
    static final boolean BOOLEAN_PARAM_VALUE = true;
    static final byte BYTE_PARAM_VALUE = 123;
    static final char CHAR_PARAM_VALUE = '@';
    static final short SHORT_PARAM_VALUE = 12345;
    static final int INT_PARAM_VALUE = 123456789;
    static final long LONG_PARAM_VALUE = 102030405060708090L;
    static final float FLOAT_PARAM_VALUE = 123.456f;
    static final double DOUBLE_PARAM_VALUE = 0.123456789;
    static final Object OBJECT_PARAM_VALUE = new Object();
    static final int[] ARRAY_PARAM_VALUE = new int[]{1, 2, 3, 4, 5};
    static final Class<?> CLASS_PARAM_VALUE = StackTrace002Debuggee.class;
    static final ClassLoader CLASS_LOADER_PARAM_VALUE = CLASS_PARAM_VALUE.getClassLoader();
    static final String STRING_PARAM_VALUE = "this is a string object";
    static final Thread THREAD_PARAM_VALUE = new Thread("this is a thread");
    static final ThreadGroup THREAD_GROUP_PARAM_VALUE = THREAD_PARAM_VALUE.getThreadGroup();

    // Values used to check StackFrame.SetValues.
    static final boolean BOOLEAN_PARAM_VALUE_TO_SET = !BOOLEAN_PARAM_VALUE;
    static final byte BYTE_PARAM_VALUE_TO_SET = -BYTE_PARAM_VALUE;
    static final char CHAR_PARAM_VALUE_TO_SET = '%';
    static final short SHORT_PARAM_VALUE_TO_SET = -SHORT_PARAM_VALUE;
    static final int INT_PARAM_VALUE_TO_SET = -INT_PARAM_VALUE;
    static final long LONG_PARAM_VALUE_TO_SET = -LONG_PARAM_VALUE;
    static final float FLOAT_PARAM_VALUE_TO_SET = -FLOAT_PARAM_VALUE;
    static final double DOUBLE_PARAM_VALUE_TO_SET = -DOUBLE_PARAM_VALUE;
    static final Object OBJECT_PARAM_VALUE_TO_SET = new Object();
    static final int[] ARRAY_PARAM_VALUE_TO_SET = new int[]{5, 4, 3, 2, 1};
    static final Class<?> CLASS_PARAM_VALUE_TO_SET = Object.class;
    static final ClassLoader CLASS_LOADER_PARAM_VALUE_TO_SET =
            CLASS_PARAM_VALUE_TO_SET.getClassLoader();
    static final String STRING_PARAM_VALUE_TO_SET = "this is another string object";
    static final Thread THREAD_PARAM_VALUE_TO_SET = new Thread("this is another thread");
    static final ThreadGroup THREAD_GROUP_PARAM_VALUE_TO_SET =
            THREAD_PARAM_VALUE_TO_SET.getThreadGroup();

    // A reference to 'this' debuggee.
    static Object THIS_OBJECT;

    public static void main(String[] args) {
        runDebuggee(StackTrace002Debuggee.class);
    }

    @Override
    public void run() {
        THIS_OBJECT = this;

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Wait for test setup.
        String signal = synchronizer.receiveMessage();

        // Invoke the method requested by the test.
        switch (signal) {
            case BOOLEAN_SIGNAL:
                runBreakpointBoolean(BOOLEAN_PARAM_VALUE);
                break;
            case BYTE_SIGNAL:
                runBreakpointByte(BYTE_PARAM_VALUE);
                break;
            case CHAR_SIGNAL:
                runBreakpointChar(CHAR_PARAM_VALUE);
                break;
            case SHORT_SIGNAL:
                runBreakpointShort(SHORT_PARAM_VALUE);
                break;
            case INT_SIGNAL:
                runBreakpointInt(INT_PARAM_VALUE);
                break;
            case INT_METHOD2_SIGNAL:
                runBreakpointInt2(INT_PARAM_VALUE);
                break;
            case LONG_METHOD_SIGNAL:
                runBreakpointLong(LONG_PARAM_VALUE);
                break;
            case FLOAT_METHOD:
                runBreakpointFloat(FLOAT_PARAM_VALUE);
                break;
            case DOUBLE_METHOD:
                runBreakpointDouble(DOUBLE_PARAM_VALUE);
                break;
            case OBJECT_SIGNAL:
                runBreakpointObject(OBJECT_PARAM_VALUE);
                break;
            case ARRAY_SIGNAL:
                runBreakpointArray(ARRAY_PARAM_VALUE);
                break;
            case CLASS_SIGNAL:
                runBreakpointClass(CLASS_PARAM_VALUE);
                break;
            case CLASS_LOADER_SIGNAL:
                runBreakpointClassLoader(CLASS_LOADER_PARAM_VALUE);
                break;
            case STRING_SIGNAL:
                runBreakpointString(STRING_PARAM_VALUE);
                break;
            case THREAD_SIGNAL:
                runBreakpointThread(THREAD_PARAM_VALUE);
                break;
            case THREAD_GROUP_SIGNAL:
                runBreakpointThreadGroup(THREAD_GROUP_PARAM_VALUE);
                break;
            case ARRAY_AS_OBJECT_SIGNAL:
                runBreakpointObject(ARRAY_PARAM_VALUE);
                break;
            case CLASS_AS_OBJECT_SIGNAL:
                runBreakpointObject(CLASS_PARAM_VALUE);
                break;
            case CLASS_LOADER_AS_OBJECT_SIGNAL:
                runBreakpointObject(CLASS_LOADER_PARAM_VALUE);
                break;
            case STRING_AS_OBJECT_SIGNAL:
                runBreakpointObject(STRING_PARAM_VALUE);
                break;
            case THREAD_AS_OBJECT_SIGNAL:
                runBreakpointObject(THREAD_PARAM_VALUE);
                break;
            case THREAD_GROUP_AS_OBJECT_SIGNAL:
                runBreakpointObject(THREAD_GROUP_PARAM_VALUE);
                break;
            default:
                throw new TestErrorException("Unexpected signal \"" + signal + "\"");
        }

    }

    // Test boolean type.
    public void runBreakpointBoolean(boolean param) {
        breakpointBoolean(param);
        breakpointBoolean(param);
    }

    public void breakpointBoolean(boolean param) {
        logWriter.println("breakpointBoolean(param=" + param + ")");
    }

    // Test byte type.
    public void runBreakpointByte(byte param) {
        breakpointByte(param);
        breakpointByte(param);
    }

    public void breakpointByte(byte param) {
        logWriter.println("breakpointByte(param=" + param + ")");
    }

    // Test char type.
    public void runBreakpointChar(char param) {
        breakpointChar(param);
        breakpointChar(param);
    }

    public void breakpointChar(char param) {
        logWriter.println("breakpointChar(param=" + param + ")");
    }

    // Test short type.
    public void runBreakpointShort(short param) {
        breakpointShort(param);
        breakpointShort(param);
    }

    public void breakpointShort(short param) {
        logWriter.println("breakpointShort(param=" + param + ")");
    }

    // Test int type.
    public void runBreakpointInt(int param) {
        breakpointInt(param);
        breakpointInt(param);
    }

    public void breakpointInt(int param) {
        logWriter.println("breakpointInt(param=" + param + ")");
    }

    public void runBreakpointInt2(int param) {
        int local = param;
        breakpointInt2(local);
        local = local + param;
        breakpointInt2(local);
    }

    public void breakpointInt2(int param) {
        logWriter.println("breakpointInt2(param=" + param + ")");
    }

    // Test long type.
    public void runBreakpointLong(long param) {
        breakpointLong(param);
        breakpointLong(param);
    }

    public void breakpointLong(long param) {
        logWriter.println("breakpointLong(param=" + param + ")");
    }

    // Test float type.
    public void runBreakpointFloat(float param) {
        breakpointFloat(param);
        breakpointFloat(param);
    }

    public void breakpointFloat(float param) {
        logWriter.println("breakpointFloat(param=" + param + ")");
    }

    // Test double type.
    public void runBreakpointDouble(double param) {
        breakpointDouble(param);
        breakpointDouble(param);
    }

    public void breakpointDouble(double param) {
        logWriter.println("breakpointDouble(param=" + param + ")");
    }

    // Test java.lang.Object type.
    public void runBreakpointObject(Object param) {
        breakpointObject(param);
        breakpointObject(param);
    }

    public void breakpointObject(Object param) {
        logWriter.println("breakpointObject(param=\"" + param + "\")");
    }

    // Test array type.
    public void runBreakpointArray(int[] param) {
        breakpointArray(param);
        breakpointArray(param);
    }

    public void breakpointArray(int[] param) {
        logWriter.println("breakpointArray(param=\"" + param + "\")");
    }

    // Test java.lang.Class type.
    public void runBreakpointClass(Class<?> param) {
        breakpointClass(param);
        breakpointClass(param);
    }

    public void breakpointClass(Class<?> param) {
        logWriter.println("breakpointClass(param=\"" + param + "\")");
    }

    // Test java.lang.ClassLoader type.
    public void runBreakpointClassLoader(ClassLoader param) {
        breakpointClassLoader(param);
        breakpointClassLoader(param);
    }

    public void breakpointClassLoader(ClassLoader param) {
        logWriter.println("breakpointClassLoader(param=\"" + param + "\")");
    }

    // Test java.lang.String type.
    public void runBreakpointString(String param) {
        breakpointString(param);
        breakpointString(param);
    }

    public void breakpointString(String param) {
        logWriter.println("breakpointString(param=\"" + param + "\")");
    }

    // Test java.lang.Thread type.
    public void runBreakpointThread(Thread param) {
        breakpointThread(param);
        breakpointThread(param);
    }

    public void breakpointThread(Thread param) {
        logWriter.println("breakpointThread(param=\"" + param + "\")");
    }

    // Test java.lang.ThreadGroup type.
    public void runBreakpointThreadGroup(ThreadGroup param) {
        breakpointThreadGroup(param);
        breakpointThreadGroup(param);
    }

    public void breakpointThreadGroup(ThreadGroup param) {
        logWriter.println("breakpointThreadGroup(param=\"" + param + "\")");
    }
}