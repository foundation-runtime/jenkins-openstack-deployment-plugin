package org.jenkinsci.plugins.os_ci.model.maven;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

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
 */public class MavenInvoker {
    public static void createRPM(String rpmPath, AbstractBuild build, BuildListener listener) {
        try {


            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(rpmPath));
            request.setGoals(Collections.singletonList("package"));
            EnvVars envVars = new EnvVars();
            envVars = build.getEnvironment(listener);
            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(new File(envVars.get("M2_HOME")));
            InvocationResult result = invoker.execute(request);
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
