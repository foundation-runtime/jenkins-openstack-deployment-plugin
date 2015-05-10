package org.jenkinsci.plugins.os_ci.utils;

import hudson.model.BuildListener;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Copyright 2015 Cisco Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */public class LogUtils {

    public static void log(final BuildListener listener, String message) {
        if (listener != null)
            log(listener.getLogger(), message);
        else
            System.out.println(message);
    }

    private static void log(PrintStream logger, String message) {
        if (logger != null)
            logger.println(new StringBuilder().append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime())).append(" ").append(message).toString());
    }
    public static void logSection(final BuildListener listener, String message) {
        log (listener, "");
        log (listener, "*****************************************************");
        log (listener, "**" + message);
        log (listener, "*****************************************************");
        log (listener, "");
    }

}
