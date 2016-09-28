package org.slf4j.impl;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This class handles three issues:
 * <ul>
 *     <li>It allows you to log or suppress DEBUG messages, simply by setting {@link #DEBUG} appropriately. See the field doc for more info.</li>
 *     <li>Logs ALL exceptions, including UnknownHostException and all exceptions caused by this exception. Android filters out any exceptions which were caused by UnknownHostException.
 *     See http://stackoverflow.com/questions/28897239/log-e-does-not-print-the-stack-trace-of-unknownhostexception for details.
 *     </li>
 *     <li>Process the exception message to allow it to log on Android Studio 1.5. See https://code.google.com/p/android/issues/detail?id=194446 and https://code.google.com/p/android/issues/detail?id=194974</li>
 * </ul>
 * @author mvy
 */
public class HandroidLoggerAdapter extends AndroidLoggerAdapter {

    /**
     * True if the debug messages should be logged, false if not. Defaults to false.
     * <p></p>
     * Good practice is to log debug during development, while not logging debug messages during production.
     * Put this into your android.app.Application's constructor to achieve this:
     * <pre>
     * HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG;
     * </pre>
     */
    public static boolean DEBUG = false;

    HandroidLoggerAdapter(String tag) {
        super(tag);
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable. As opposed to Android logging mechanism, it logs even UnknownHostExceptions.
     * @param tr An exception to log
     */
    @NonNull
    public static String getStackTraceString(@NonNull Throwable tr) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    @Override
    protected boolean isLoggable(int priority) {
        return DEBUG ? true : super.isLoggable(priority);
    }

    @Override
    protected void logInternal(int priority, String message, Throwable throwable) {
        if (throwable != null) {
            message += '\n' + getStackTraceString(throwable);
        }
        message = postprocessMessage(message).trim();
        Log.println(priority, name, message);
    }

    @NonNull
    private static String postprocessMessage(@NonNull String message) {
        // we need to do the following, to work around Android Studio 1.5 bugs:
        // 1. remove all characters with code point 0..31 (for example \r) - if those characters are present in the message, the message is not simply logged at all by Android (!!!)
        // see https://code.google.com/p/android/issues/detail?id=194446
        // see https://code.google.com/p/android/issues/detail?id=194974
        // 2. remove two or more consecutive \n: https://code.google.com/p/android/issues/detail?id=195164
        final StringBuilder sb = new StringBuilder(message.length());
        boolean lastCharWasNewLine = false;
        for (int i = 0; i < message.length(); i++) {
            final char c = message.charAt(i);
            if (c == '\n' && lastCharWasNewLine) {
                // do nothing
            } else if (c >= 32 || c == '\n') {
                sb.append(c);
            } else {
                sb.append(' ');
            }
            lastCharWasNewLine = c == '\n';
        }
        return sb.toString();
    }
}
