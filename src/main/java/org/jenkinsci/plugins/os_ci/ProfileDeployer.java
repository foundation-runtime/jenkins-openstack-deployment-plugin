package org.jenkinsci.plugins.os_ci;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.os_ci.model.OpenstackParameters;
import org.jenkinsci.plugins.os_ci.model.YumRepoParameters;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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
public class ProfileDeployer extends Builder {

    private final String groupId;
    private final String artifactId;
    private final OpenstackParameters osParameters;
    private final YumRepoParameters yumRepoParameters;

    @DataBoundConstructor
    public ProfileDeployer(String groupId, String artifactId, OpenstackParameters osParameters, YumRepoParameters yumRepoParameters) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.osParameters = osParameters;
        this.yumRepoParameters = yumRepoParameters;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public OpenstackParameters getOsParameters() {return osParameters;}


    public YumRepoParameters getYumRepoParameters() {return yumRepoParameters;}

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        return new ProductDeployer(groupId, artifactId, osParameters, yumRepoParameters).perform(build, launcher, listener);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Deploy Profile";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            return true;
        }

    }

}
