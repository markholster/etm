package com.jecstar.etm.slf4j;

import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class to determine the location of the log caller. Based on the log4j's
 * LocationInfo class.
 *
 * @author Mark Holster
 */
class LogLocation implements Serializable {

    private static final long serialVersionUID = 514776389276203707L;
    private final static String LINE_SEP = System.getProperty("line.separator");
    private final static int LINE_SEP_LEN = LINE_SEP.length();
    private final static String NA = "?";

    private transient String lineNumber;
    private transient String fileName;
    private transient String className;
    private transient String methodName;
    public String fullInfo;

    private static final StringWriter sw = new StringWriter();
    private static final PrintWriter pw = new PrintWriter(sw);

    private static Method getStackTraceMethod;
    private static Method getClassNameMethod;
    private static Method getMethodNameMethod;
    private static Method getFileNameMethod;
    private static Method getLineNumberMethod;

    // Check if we are running in IBM's visual age.
    private static boolean inVisualAge = false;

    static {
        try {
            inVisualAge = Class.forName("com.ibm.uvm.tools.DebugSupport") != null;
        } catch (Throwable e) {
        }
        try {
            getStackTraceMethod = Throwable.class.getMethod("getStackTrace", (Class<?>[]) null);
            Class<?> stackTraceElementClass = Class.forName("java.lang.StackTraceElement");
            getClassNameMethod = stackTraceElementClass.getMethod("getClassName", (Class<?>[]) null);
            getMethodNameMethod = stackTraceElementClass.getMethod("getMethodName", (Class<?>[]) null);
            getFileNameMethod = stackTraceElementClass.getMethod("getFileName", (Class<?>[]) null);
            getLineNumberMethod = stackTraceElementClass.getMethod("getLineNumber", (Class<?>[]) null);
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
        }
    }

    /**
     * Instantiate location information based on a Throwable. We expect the
     * Throwable <code>t</code>, to be in the format
     * <p>
     * <pre>
     * java.lang.Throwable
     * ...
     * at org.apache.log4j.PatternLayout.format(PatternLayout.java:413)
     * at org.apache.log4j.FileAppender.doAppend(FileAppender.java:183)
     * at org.apache.log4j.Category.callAppenders(Category.java:131)
     * at org.apache.log4j.Category.log(Category.java:512)
     * at callers.fully.qualified.className.methodName(FileName.java:74)
     * ...
     * </pre>
     * <p>
     * <p>
     * However, we can also deal with JIT compilers that "lose" the location
     * information, especially between the parentheses.
     *
     * @param t                 throwable used to determine location, may be null.
     * @param fqnOfCallingClass class name of first class considered part of the logging
     *                          framework. Location will be site that calls a method on this
     *                          class.
     */
    public LogLocation(Throwable t, String fqnOfCallingClass) {
        if (t == null || fqnOfCallingClass == null)
            return;
        if (getLineNumberMethod != null) {
            try {
                Object[] elements = (Object[]) getStackTraceMethod.invoke(t, (Object[]) null);
                String prevClass = NA;
                for (int i = elements.length - 1; i >= 0; i--) {
                    String thisClass = (String) getClassNameMethod.invoke(elements[i], (Object[]) null);
                    if (fqnOfCallingClass.equals(thisClass)) {
                        int caller = i + 1;
                        if (caller < elements.length) {
                            className = prevClass;
                            methodName = (String) getMethodNameMethod.invoke(elements[caller], (Object[]) null);
                            fileName = (String) getFileNameMethod.invoke(elements[caller], (Object[]) null);
                            if (fileName == null) {
                                fileName = NA;
                            }
                            int line = (Integer) getLineNumberMethod.invoke(elements[caller], (Object[]) null);
                            if (line < 0) {
                                lineNumber = NA;
                            } else {
                                lineNumber = String.valueOf(line);
                            }
                            String buf = className +
                                    "." +
                                    methodName +
                                    "(" +
                                    fileName +
                                    ":" +
                                    lineNumber +
                                    ")";
                            this.fullInfo = buf;
                        }
                        return;
                    }
                    prevClass = thisClass;
                }
                return;
            } catch (IllegalAccessException | RuntimeException ex) {
            } catch (InvocationTargetException ex) {
                if (ex.getTargetException() instanceof InterruptedException
                        || ex.getTargetException() instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        String s;
        // Protect against multiple access to sw.
        synchronized (sw) {
            t.printStackTrace(pw);
            s = sw.toString();
            sw.getBuffer().setLength(0);
        }
        // System.out.println("s is ["+s+"].");
        int ibegin, iend;

        // Given the current structure of the package, the line
        // containing "org.apache.log4j.Category." should be printed just
        // before the caller.

        // This method of searching may not be fastest but it's safer
        // than counting the stack depth which is not guaranteed to be
        // constant across JVM implementations.
        ibegin = s.lastIndexOf(fqnOfCallingClass);
        if (ibegin == -1)
            return;

        //
        // if the next character after the class name exists
        // but is not a period, see if the classname is
        // followed by a period earlier in the trace.
        // Minimizes mistakeningly matching on a class whose
        // name is a substring of the desired class.
        // See bug 44888.
        if (ibegin + fqnOfCallingClass.length() < s.length() && s.charAt(ibegin + fqnOfCallingClass.length()) != '.') {
            int i = s.lastIndexOf(fqnOfCallingClass + ".");
            if (i != -1) {
                ibegin = i;
            }
        }

        ibegin = s.indexOf(LINE_SEP, ibegin);
        if (ibegin == -1)
            return;
        ibegin += LINE_SEP_LEN;

        // determine end of line
        iend = s.indexOf(LINE_SEP, ibegin);
        if (iend == -1)
            return;

        // VA has a different stack trace format which doesn't
        // need to skip the inital 'at'
        if (!inVisualAge) {
            // back up to first blank character
            ibegin = s.lastIndexOf("at ", iend);
            if (ibegin == -1)
                return;
            // Add 3 to skip "at ";
            ibegin += 3;
        }
        // everything between is the requested stack item
        this.fullInfo = s.substring(ibegin, iend);
    }

    /**
     * Appends a location fragment to a buffer to build the full location info.
     *
     * @param buf      StringBuffer to receive content.
     * @param fragment fragment of location (class, method, file, line), if null the
     *                 value of NA will be appended.
     * @since 1.2.15
     */
    private static void appendFragment(final StringBuilder buf, final String fragment) {
        if (fragment == null) {
            buf.append(NA);
        } else {
            buf.append(fragment);
        }
    }

    /**
     * Create new instance.
     *
     * @param file      source file name
     * @param classname class name
     * @param method    method
     * @param line      source line number
     * @since 1.2.15
     */
    public LogLocation(final String file, final String classname, final String method, final String line) {
        this.fileName = file;
        this.className = classname;
        this.methodName = method;
        this.lineNumber = line;
        StringBuilder buf = new StringBuilder();
        appendFragment(buf, classname);
        buf.append(".");
        appendFragment(buf, method);
        buf.append("(");
        appendFragment(buf, file);
        buf.append(":");
        appendFragment(buf, line);
        buf.append(")");
        this.fullInfo = buf.toString();
    }

    /**
     * Return the fully qualified class name of the caller making the logging
     * request.
     */
    public String getClassName() {
        if (fullInfo == null)
            return NA;
        if (className == null) {
            // Starting the search from '(' is safer because there is
            // potentially a dot between the parentheses.
            int iend = fullInfo.lastIndexOf('(');
            if (iend == -1)
                className = NA;
            else {
                iend = fullInfo.lastIndexOf('.', iend);

                // This is because a stack trace in VisualAge looks like:

                // java.lang.RuntimeException
                // java.lang.Throwable()
                // java.lang.Exception()
                // java.lang.RuntimeException()
                // void test.test.B.print()
                // void test.test.A.printIndirect()
                // void test.test.Run.main(java.lang.String [])
                int ibegin = 0;
                if (inVisualAge) {
                    ibegin = fullInfo.lastIndexOf(' ', iend) + 1;
                }

                if (iend == -1)
                    className = NA;
                else
                    className = this.fullInfo.substring(ibegin, iend);
            }
        }
        return className;
    }

    /**
     * Return the file name of the caller.
     * <p>
     * <p>
     * This information is not always available.
     */
    public String getFileName() {
        if (fullInfo == null)
            return NA;

        if (fileName == null) {
            int iend = fullInfo.lastIndexOf(':');
            if (iend == -1)
                fileName = NA;
            else {
                int ibegin = fullInfo.lastIndexOf('(', iend - 1);
                fileName = this.fullInfo.substring(ibegin + 1, iend);
            }
        }
        return fileName;
    }

    /**
     * Returns the line number of the caller.
     * <p>
     * <p>
     * This information is not always available.
     */
    public String getLineNumber() {
        if (fullInfo == null)
            return NA;

        if (lineNumber == null) {
            int iend = fullInfo.lastIndexOf(')');
            int ibegin = fullInfo.lastIndexOf(':', iend - 1);
            if (ibegin == -1)
                lineNumber = NA;
            else
                lineNumber = this.fullInfo.substring(ibegin + 1, iend);
        }
        return lineNumber;
    }

    /**
     * Returns the method name of the caller.
     */
    public String getMethodName() {
        if (fullInfo == null)
            return NA;
        if (methodName == null) {
            int iend = fullInfo.lastIndexOf('(');
            int ibegin = fullInfo.lastIndexOf('.', iend);
            if (ibegin == -1)
                methodName = NA;
            else
                methodName = this.fullInfo.substring(ibegin + 1, iend);
        }
        return methodName;
    }

}
