package org.jenkinsci.plugins.os_ci.model;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;

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
 */
public class ArtifactParameters implements Describable<ArtifactParameters> {
    private String groupId;
    private String artifactId;
    private String version;
    private String osVersion = "rpm";


    @DataBoundConstructor
    public ArtifactParameters(String groupId, String artifactId, String version, String osVersion) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.osVersion = osVersion;
    }

    public String getOsVersion() {
            return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Descriptor<ArtifactParameters> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ArtifactParameters> {
        public ListBoxModel doFillOsVersionItems() {
            return new ListBoxModel(
                    new ListBoxModel.Option("RH5", "rpm"),
                    new ListBoxModel.Option("RH6", "rh6")
            );
        }
        @Override
        public String getDisplayName() {
            return "Artifact Parameters";
        }
    }

}
