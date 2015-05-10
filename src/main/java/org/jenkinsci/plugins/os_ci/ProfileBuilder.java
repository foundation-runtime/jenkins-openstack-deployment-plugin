package org.jenkinsci.plugins.os_ci;

import com.google.common.base.Joiner;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;
import org.jenkinsci.plugins.os_ci.model.Product;
import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;
import org.jenkinsci.plugins.os_ci.utils.LogUtils;
import org.jenkinsci.plugins.os_ci.utils.VersionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
public class ProfileBuilder extends Builder{
    private final String artifactId;
    private final String groupId;
    private final String increaseVersionOption;
    private final ArtifactParameters[] dependentProducts;

    @DataBoundConstructor
    public ProfileBuilder(String artifactId, String groupId, String increaseVersionOption, ArtifactParameters[] dependentProducts) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.increaseVersionOption = increaseVersionOption;
        this.dependentProducts = dependentProducts;
    }

    public String getArtifactId() {return artifactId;}

    public String getIncreaseVersionOption() {return increaseVersionOption;}

    public ArtifactParameters[] getDependentProducts() {return dependentProducts;}

    public String getGroupId() {return groupId;}

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {

        final String targetFolder =  Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "archive");
        try {
            FileUtils.forceMkdir(new File(targetFolder));
        } catch (IOException e) {
            LogUtils.log(listener, "Failed to create folder: " + targetFolder);
            throw new OsCiPluginException("Failed to create folder: " + targetFolder);
        }
        // Get product version from Nexus and increase minor version
        ArtifactParameters artifact = new ArtifactParameters(groupId, artifactId,"LATEST",null);


        Product product = new Product(artifact, build, listener);
        artifact.setVersion(new NexusClient(artifact, build, listener).increaseVersion(increaseVersionOption));

        // Resolve version information on all Products
        LogUtils.logSection(listener, "Getting Nexus dependentProducts versions");
        ArrayList<ArtifactParameters> productDependencies = VersionUtils.resolveVersionInformation(dependentProducts, build, listener);

        // Create pom.xml
        String pomPath = product.createPom(targetFolder, productDependencies, null);

        // upload pom and tar to Nexus
        NexusClient nexusClient = new NexusClient( artifact,build,listener);
        nexusClient.uploadPomWithExternalDependencies("versionDesc",pomPath,"pom.xml",null,null,null);
        build.setDescription(artifact.getVersion());
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Create Profile";
        }
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return true;
        }

        public ListBoxModel doFillIncreaseVersionOptionItems() {
            return new ListBoxModel(
                    new ListBoxModel.Option("Major", String.valueOf(VersionUtils.increaseVersionOptions.MAJOR), false),
                    new ListBoxModel.Option("Minor", String.valueOf(VersionUtils.increaseVersionOptions.MINOR), false),
                    new ListBoxModel.Option("Patch", String.valueOf(VersionUtils.increaseVersionOptions.PACTH), false),
                    new ListBoxModel.Option("Build", String.valueOf(VersionUtils.increaseVersionOptions.BUILD), true)

            );
        }
    }

}
