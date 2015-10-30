/*
 * Copyright (C) 2011 The Android Open Source Project
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

package vogar.target.junit;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import vogar.ClassAnalyzer;

/**
 * Utilities for manipulating JUnit tests.
 */
public final class Junit3 {
    private Junit3() {}

    private static final Method setUp;
    private static final Method tearDown;
    private static final Method runTest;
    static {
        try {
            setUp = TestCase.class.getDeclaredMethod("setUp");
            setUp.setAccessible(true);
            tearDown = TestCase.class.getDeclaredMethod("tearDown");
            tearDown.setAccessible(true);
            runTest = TestCase.class.getDeclaredMethod("runTest");
            runTest.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        }
    }

    /**
     * Creates eager JUnit Test instances from the given test case or test
     * suite.
     */
    public static List<Test> classToJunitTests(Class<?> testClass) {
        try {
            try {
                Method suiteMethod = testClass.getMethod("suite");
                return Collections.singletonList((Test) suiteMethod.invoke(null));
            } catch (NoSuchMethodException ignored) {
            }

            if (TestCase.class.isAssignableFrom(testClass)) {
                List<Test> result = new ArrayList<Test>();
                for (Method m : testClass.getMethods()) {
                    if (!m.getName().startsWith("test")) {
                        continue;
                    }
                    if (m.getParameterTypes().length == 0) {
                        TestCase testCase = (TestCase) testClass.newInstance();
                        testCase.setMethod(m);
                        result.add(testCase);
                    } else {
                        // TODO: warn
                    }
                }
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("Unknown test class: " + testClass);
    }

    /**
     * Creates lazy vogar test instances from the given test case or test
     * suite.
     *
     * @param args if non-empty, this is the list of test method names.
     */
    public static List<VogarTest> classToVogarTests(Class<?> testClass, String... args) {
        List<VogarTest> result = new ArrayList<VogarTest>();
        getSuiteMethods(result, testClass, args);
        return result;
    }

    public static boolean isJunit3Test(Class<?> klass) {
        // public class FooTest extends TestCase {...}
        //   or
        // public class FooSuite {
        //    public static Test suite() {...}
        // }
        return (TestCase.class.isAssignableFrom(klass) && !Modifier.isAbstract(klass.getModifiers()))
                || new ClassAnalyzer(klass).hasMethod(true, Test.class, "suite");
    }

    private static void getSuiteMethods(List<VogarTest> out, Class<?> testClass, String... args) {
        /*
         * Handle classes assignable to TestCase
         */
        if (TestCase.class.isAssignableFrom(testClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends TestCase> testCaseClass = (Class<? extends TestCase>) testClass;

            if (args.length == 0) {
                for (Method m : testClass.getMethods()) {
                    if (!m.getName().startsWith("test")) {
                        continue;
                    }
                    if (m.getParameterTypes().length == 0) {
                        out.add(TestMethod.create(testCaseClass, m));
                    } else {
                        // TODO: warn
                    }
                }
            } else {
                for (String arg : args) {
                    try {
                        out.add(TestMethod.create(testCaseClass, testClass.getMethod(arg)));
                    } catch (final NoSuchMethodException e) {
                        out.add(new ConfigurationError(testClass.getName() + "#" + arg, e));
                    }
                }
            }

            return;
        }

        /*
         * Handle classes that define suite()
         */
        try {
            Method suiteMethod = testClass.getMethod("suite");
            junit.framework.Test test;
            try {
                test = (junit.framework.Test) suiteMethod.invoke(null);
            } catch (Throwable e) {
                out.add(new ConfigurationError(testClass.getName() + "#suite", e));
                return;
            }

            if (test instanceof TestCase) {
                out.add(createForTestCase((TestCase) test));
            } else if (test instanceof TestSuite) {
                getTestSuiteTests(out, (TestSuite) test);
            } else {
                out.add(new ConfigurationError(testClass.getName() + "#suite",
                        new IllegalStateException("Unknown suite() result: " + test)));
            }
            return;
        } catch (NoSuchMethodException ignored) {
        }

        out.add(new ConfigurationError(testClass.getName() + "#suite",
                new IllegalStateException("Not a test case: " + testClass)));
    }

    private static void getTestSuiteTests(List<VogarTest> out, TestSuite suite) {
        for (Object testsOrSuite : suite.getTestsAndSuites()) {
            if (testsOrSuite instanceof Class) {
                getSuiteMethods(out, (Class<?>) testsOrSuite);
            } else if (testsOrSuite instanceof TestCase) {
                out.add(createForTestCase((TestCase) testsOrSuite));
            } else if (testsOrSuite instanceof TestSuite) {
                getTestSuiteTests(out, (TestSuite) testsOrSuite);
            } else if (testsOrSuite != null) {
                out.add(new ConfigurationError(testsOrSuite.getClass().getName() + "#getClass",
                        new IllegalStateException("Unknown test: " + testsOrSuite)));
            }
        }
    }

    private static VogarTest createForTestCase(TestCase testCase) {
        return new TestCaseInstance(testCase, testCase.getMethod());
    }

    private static class ConfigurationError implements VogarTest {
        private final String name;
        private final Throwable cause;

        private ConfigurationError(String name, Throwable cause) {
            this.name = name;
            this.cause = cause;
        }

        @Override public void run() throws Throwable {
            throw cause;
        }

        @Override public String toString() {
            return name;
        }
    }

    private abstract static class VogarJUnitTest implements VogarTest {
        protected final Class<? extends TestCase> testClass;
        protected final Method method;

        protected VogarJUnitTest(Class<? extends TestCase> testClass, Method method) {
            this.testClass = testClass;
            this.method = method;
        }

        public void run() throws Throwable {
            TestCase testCase = getTestCase();
            Throwable failure = null;
            try {
                setUp.invoke(testCase);
                method.invoke(testCase);
            } catch (InvocationTargetException t) {
                failure = t.getCause();
            } catch (Throwable t) {
                failure = t;
            }

            try {
                tearDown.invoke(testCase);
            } catch (InvocationTargetException t) {
                if (failure == null) {
                    failure = t.getCause();
                }
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                }
            }

            if (failure != null) {
                throw failure;
            }
        }

        protected abstract TestCase getTestCase() throws Exception;
    }

    /**
     * A JUnit TestCase constructed on demand and then released.
     */
    private static class TestMethod extends VogarJUnitTest {
        private final Constructor<? extends TestCase> constructor;
        private final Object[] constructorArgs;

        private TestMethod(Class<? extends TestCase> testClass, Method method,
                Constructor<? extends TestCase> constructor, Object[] constructorArgs) {
            super(testClass, method);
            this.constructor = constructor;
            this.constructorArgs = constructorArgs;
        }

        public static VogarTest create(Class<? extends TestCase> testClass, Method method) {
            try {
                return new TestMethod(testClass, method, testClass.getConstructor(), new Object[0]);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                return new TestMethod(testClass, method, testClass.getConstructor(String.class),
                        new Object[] { method.getName() });
            } catch (NoSuchMethodException ignored) {
            }
            return new ConfigurationError(testClass.getName() + "#" + method.getName(),
                    new Exception("Test cases must have a no-arg or string constructor."));
        }

        @Override protected TestCase getTestCase() throws Exception {
            return constructor.newInstance(constructorArgs);
        }

        @Override public String toString() {
            return testClass.getName() + "#" + method.getName();
        }
    }

    /**
     * A JUnit TestCase already constructed.
     */
    private static class TestCaseInstance extends VogarJUnitTest {
        private final TestCase testCase;

        private TestCaseInstance(TestCase testCase, Method method) {
            super(testCase.getClass(), method);
            this.testCase = testCase;
        }

        @Override protected TestCase getTestCase() throws Exception {
            return testCase;
        }

        @Override public String toString() {
            return testCase.getClass().getName() + "#" + testCase.getName();
        }
    }
}
