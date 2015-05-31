package org.jenkinsci.plugins.os_ci.utils;

import hudson.model.*;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.os_ci.exceptions.ArtifactVersionNotFoundException;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;
import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright 2015 Cisco Systems, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class VersionUtils {

    static public enum increaseVersionOptions {
        MAJOR, MINOR, PACTH, BUILD;
    }

    static public boolean checkArtifactVersion(String modulename, String version) throws ArtifactVersionNotFoundException {

        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-(\\d*))?")
                .matcher(version);

        if (m.find()) {
            if (m.group(5) != null)
                return true;
        }

        throw new ArtifactVersionNotFoundException("Please check that module <" + modulename + "> version is from the x.y.z-w pattern.");
    }

    static public String increaseArtifactVersion(String oldVersion, increaseVersionOptions increaseOption) throws ArtifactVersionNotFoundException {

        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-(\\d*))?")
                .matcher(oldVersion);

        if (m.find()) {

            String major = m.group(1);
            String minor = m.group(2);
            String patch = m.group(3);
            String build = m.group(5);

            switch (increaseOption) {

                case MAJOR:
                    major = String.valueOf(Integer.valueOf(major) + 1);
                    break;
                case MINOR:
                    minor = String.valueOf(Integer.valueOf(minor) + 1);
                    break;
                case PACTH:
                    patch = String.valueOf(Integer.valueOf(patch) + 1);
                    break;
                case BUILD:
                    build = String.valueOf(Integer.valueOf(build) + 1);
                    break;
                default:
                    throw new ArtifactVersionNotFoundException("increaseVersionOption not found");
            }

            String newVersion = org.apache.commons.lang.StringUtils.join(new String[]{
                    org.apache.commons.lang.StringUtils.join(new String[]{major, minor, patch}, "."), build}, "-");
            return newVersion;
        }

        return oldVersion;
    }

    static public ArrayList<ArtifactParameters> resolveVersionInformation(ArtifactParameters[] artifacts, AbstractBuild build, BuildListener listener) {
        ArrayList<ArtifactParameters> resolvedModules = new ArrayList<ArtifactParameters>();

        // Resolve version information on all modules
        if (artifacts != null) {
            for (ArtifactParameters artifact : artifacts) {
                String moduleVersion = artifact.getVersion();

                if (artifact.getVersion().equalsIgnoreCase("latest")) {
                    NexusClient nc = new NexusClient(artifact, build, listener);
                    moduleVersion = nc.getLatestVersion();
                } else if (StringUtils.containsIgnoreCase(artifact.getVersion(), "cisco_vcs-f_snapshots")) {
//                    repoId = "cisco_vcs-f_snapshots";
                    NexusClient nc = new NexusClient(artifact, build, listener);
                    moduleVersion = nc.getLatestVersion("cisco_vcs-f_snapshots");
                }

                if (artifact.getVersion().equalsIgnoreCase("latest-snapshot")) {
                    NexusClient nc = new NexusClient(artifact, build, listener);
                    moduleVersion = nc.getLatestVersion("snapshots");
                }

                //checkArtifactVersion(artifact.getArtifactId(), moduleVersion);
                resolvedModules.add(new ArtifactParameters(artifact.getGroupId(), artifact.getArtifactId(), moduleVersion, artifact.getOsVersion()));
            }
        }
        return resolvedModules;
    }

    public static String getVersionParameterValue(AbstractBuild build) {
        List<ParametersAction> actions = build.getActions(ParametersAction.class);
        if (actions == null) {
            throw new OsCiPluginException("Boolean job parameter called 'promote' is required but missing");
        }
        ParameterValue versionValue = null;
        for (ParametersAction action : actions) {
            versionValue = action.getParameter("version");
            if (versionValue != null)
                break;
        }
        if (versionValue == null) {
            throw new OsCiPluginException("String job parameter called 'version' is required but missing");
        }
        if (versionValue.getClass() != StringParameterValue.class) {
            throw new OsCiPluginException("Job parameter called 'string' exists, but is not String as should");
        }
        return ((StringParameterValue) versionValue).value.trim();
    }

    public static boolean checkVersionParameterExists(AbstractBuild build) {
        List<ParametersAction> actions = build.getActions(ParametersAction.class);
        if (actions == null) {
            return false;
        }
        ParameterValue versionValue = null;
        for (ParametersAction action : actions) {
            versionValue = action.getParameter("version");
            if (versionValue != null && !((StringParameterValue) versionValue).value.equalsIgnoreCase("") && !((StringParameterValue) versionValue).value.equalsIgnoreCase("latest")) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
