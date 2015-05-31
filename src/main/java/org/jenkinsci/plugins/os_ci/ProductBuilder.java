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
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;
import org.jenkinsci.plugins.os_ci.model.Product;
import org.jenkinsci.plugins.os_ci.model.UrlParameters;
import org.jenkinsci.plugins.os_ci.model.maven.MavenInvoker;
import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;
import org.jenkinsci.plugins.os_ci.repohandlers.UrlClient;
import org.jenkinsci.plugins.os_ci.utils.CompressUtils;
import org.jenkinsci.plugins.os_ci.utils.LogUtils;
import org.jenkinsci.plugins.os_ci.utils.VersionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
public class ProductBuilder extends Builder {

    private final String artifactId;
    private final String groupId;
    private final String heatGitUrl;
    private final String scriptsGitUrl;
    private final String puppetBaseGitUrl;
    private final String increaseVersionOption;
    private final ArtifactParameters[] artifacts;
    private final UrlParameters[] externalDependencies;
    private final ArtifactParameters[] dependentProducts;

    @DataBoundConstructor
    public ProductBuilder(String artifactId, String groupId, String heatGitUrl, String scriptsGitUrl,
                          String puppetBaseGitUrl,
                          String increaseVersionOption, ArtifactParameters[] artifacts,
                          UrlParameters[] externalDependencies,
                          ArtifactParameters[] dependentProducts) {
        this.artifactId = artifactId.trim();
        this.groupId = groupId.trim();
        this.heatGitUrl = heatGitUrl;
        this.puppetBaseGitUrl = puppetBaseGitUrl;
        this.scriptsGitUrl = scriptsGitUrl;
        this.increaseVersionOption = increaseVersionOption;
        this.artifacts = artifacts;
        this.externalDependencies = externalDependencies;
        this.dependentProducts = dependentProducts;
    }


    public String getArtifactId() {
        return artifactId;
    }

    public String getHeatGitUrl() {
        return heatGitUrl;
    }

    public String getIncreaseVersionOption() {
        return increaseVersionOption;
    }

    public String getPuppetBaseGitUrl() {
        return puppetBaseGitUrl;
    }

    public String getScriptsGitUrl() {
        return scriptsGitUrl;
    }

    public ArtifactParameters[] getArtifacts() {
        return artifacts;
    }

    public UrlParameters[] getExternalDependencies() {
        return externalDependencies;
    }

    public ArtifactParameters[] getDependentProducts() {
        return dependentProducts;
    }

    public String getGroupId() {
        return groupId;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {


        FileUtils.cleanDirectory(new File(build.getRootBuild().getModuleRoot().getRemote()));
        LogUtils.log(listener, "Cleaned directory: " + build.getRootBuild().getModuleRoot().getRemote());
        final String tempTargetFolder = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "temp_archive");
        final String targetFolder = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "archive");
        final String scriptsFolder = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "deployment-scripts");

        // Get product version from Nexus and increase minor version
        ArtifactParameters artifact = null;
        if (VersionUtils.checkVersionParameterExists(build)) {
            artifact = new ArtifactParameters(groupId, artifactId, VersionUtils.getVersionParameterValue(build), "pom");
        } else {
            artifact = new ArtifactParameters(groupId, artifactId, "LATEST", "pom");
        }
        Product product = new Product(artifact, build, listener);
        artifact.setVersion(new NexusClient(artifact, build, listener).increaseVersion(increaseVersionOption));
        artifact.setOsVersion("pom");
        // Resolve version information on all Products
        LogUtils.logSection(listener, "Getting Nexus dependentProducts versions");
        ArrayList<ArtifactParameters> productDependencies = VersionUtils.resolveVersionInformation(dependentProducts, build, listener);

        // Resolve version information on all Modules
        LogUtils.logSection(listener, "Getting Nexus component versions");
        ArrayList<ArtifactParameters> artifactDependencies = VersionUtils.resolveVersionInformation(artifacts, build, listener);


        // download GIT repos
        if (!product.downloadGitRepos(tempTargetFolder, targetFolder, scriptsFolder, scriptsGitUrl, heatGitUrl, puppetBaseGitUrl))
            return false;

        // download all non-Nexus artifacts
        LogUtils.logSection(listener, "Downloading URLs");
        if (externalDependencies != null) {
            for (UrlParameters externalDependency : externalDependencies) {
                UrlClient uc = new UrlClient(externalDependency, build, listener);
                uc.downloadRPM();
                LogUtils.log(listener, "Downloaded url " + externalDependency.getUrl());
            }
        }

        // Download Puppet scripts
        if (artifactDependencies != null) {
            List<String> fileTypes = new ArrayList<String>();
            fileTypes.add("puppet.tar.gz");
            for (ArtifactParameters ap : artifactDependencies) {
                FileUtils.forceMkdir(new File(Joiner.on(File.separator).join(scriptsFolder, "puppets", "modules")));
                NexusClient nc = new NexusClient(ap, build, listener);
                List<String> names = nc.downloadProductArtifacts(ap, Joiner.on(File.separator).join(scriptsFolder, "puppets", "modules"), fileTypes);

                if (names.size() > 0) {
                    File puppetTarFile = new File(Joiner.on(File.separator).join(scriptsFolder, "puppets", "modules", "external_dependencies.tar.gz"));
                    if (puppetTarFile.exists()) {
                        CompressUtils.untarFile(puppetTarFile);
                        FileUtils.deleteQuietly(puppetTarFile);
                    }
                }
            }
        }

        // Build external_dependencies.tar.gz from artifacts and scripts
        String tarPath = product.createExternalDependenciesTar(targetFolder);
        if (tarPath == null) {
            return false;
        }

        // Create pom.xml
        String pomPath = product.createPom(targetFolder, productDependencies, artifactDependencies);
        String rpmPomPath = product.createRPMPom(scriptsFolder);


        // upload pom, rpm and tar to Nexus
        NexusClient nexusClient = new NexusClient(artifact, build, listener);

        if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            try {
                FileUtils.deleteDirectory(new File(Joiner.on(File.separator).join(scriptsFolder, "target")));
            } finally {
                LogUtils.logSection(listener, "Puppet scrips RPM creation");
                MavenInvoker.createRPM(rpmPomPath, build, listener);
                LogUtils.log(listener, "Created rpm from pom:" + rpmPomPath);
            }
            nexusClient.uploadPomWithExternalDependencies("versionDesc", pomPath, "pom.xml", tarPath, "external_dependencies.tar.gz", Joiner.on(File.separator).join(scriptsFolder, "target", "rpm", "nds_" + artifact.getArtifactId() + "_deployment-scripts", "RPMS", "noarch", "nds_" + artifact.getArtifactId() + "_deployment-scripts-" + artifact.getVersion() + "_1.noarch.rpm"));

        } else {
            nexusClient.uploadPomWithExternalDependencies("versionDesc", pomPath, "pom.xml", tarPath, "external_dependencies.tar.gz", null);
        }
        build.setDescription(artifact.getVersion());
        return true;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
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
            return "Create Product";
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
