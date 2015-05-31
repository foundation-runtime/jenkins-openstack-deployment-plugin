package org.jenkinsci.plugins.os_ci;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.exceptions.ProductDeployPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;
import org.jenkinsci.plugins.os_ci.model.Openstack.DeployParmeters;
import org.jenkinsci.plugins.os_ci.model.OpenstackParameters;
import org.jenkinsci.plugins.os_ci.model.Product;
import org.jenkinsci.plugins.os_ci.model.YumRepoParameters;
import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;
import org.jenkinsci.plugins.os_ci.repohandlers.OpenStackClient;
import org.jenkinsci.plugins.os_ci.utils.LogUtils;
import org.jenkinsci.plugins.os_ci.utils.VersionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
 */public class ProductDeployer extends Builder {

    private final String groupId;
    private final String artifactId;
    private final OpenstackParameters osParameters;
    private final YumRepoParameters yumRepoParameters;

    @DataBoundConstructor
    public ProductDeployer(String groupId, String artifactId, OpenstackParameters osParameters, YumRepoParameters yumRepoParameters) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.osParameters = osParameters;
        this.yumRepoParameters = yumRepoParameters;
    }


    public String getGroupId() {return groupId;}

    public String getArtifactId() {return artifactId;}

    public OpenstackParameters getOsParameters() {
        return osParameters;
    }

    public YumRepoParameters getYumRepoParameters() {
        return yumRepoParameters;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        DeployParmeters deployParmeters = new DeployParmeters();

        try {
            ArtifactParameters artifact = new ArtifactParameters(groupId, artifactId, VersionUtils.getVersionParameterValue(build),"pom");
            OpenStackClient openStackClient = new OpenStackClient(listener, osParameters.getOpenstackUser(), osParameters.getOpenstackPassword(), osParameters.getOpenstackTenant(), "/v2.0/tokens", osParameters.getOpenstackIP(), 5000);
            NexusClient nexusClient = new NexusClient(artifact, build, listener);

            LogUtils.logSection(listener, "Build Parameters");
            LogUtils.log(listener, yumRepoParameters.toString());
            LogUtils.log(listener, osParameters.toString());
            if(artifact.getVersion().equalsIgnoreCase("LATEST")){
                artifact.setVersion(nexusClient.getLatestVersion());
            }
            Product product = new Product(artifact, build, listener);

            product.cleanOpenstackBeforeDeployment(nexusClient, osParameters, deployParmeters);

            product.deploy(nexusClient, openStackClient, yumRepoParameters, deployParmeters);

        } catch (IOException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            releaseIPs(listener,deployParmeters);
            return false;
        } catch (ProductDeployPluginException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            LogUtils.log(listener, "Failed to launch stack: Please check your Openstack instances logs.");
            e.printStackTrace();
            return false;
        } catch (OsCiPluginException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            e.printStackTrace();
            releaseIPs(listener,deployParmeters);
            return false;
        } catch (InterruptedException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            e.printStackTrace();
            releaseIPs(listener, deployParmeters);
            return false;
        } catch (TimeoutException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            LogUtils.log(listener, "Timeout Exception: check if all stacks have been deleted : " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            e.printStackTrace();
            releaseIPs(listener, deployParmeters);
            return false;
        }
        return true;
    }

    private void releaseIPs(BuildListener listener, DeployParmeters deployParmeters)
    {

        LogUtils.logSection(listener, "Release Floating IPs");
        OpenStackClient openStackClient = new OpenStackClient(listener, osParameters.getOpenstackUser(), osParameters.getOpenstackPassword(), osParameters.getOpenstackTenant(), "/v2.0/tokens", osParameters.getOpenstackIP(), 5000);
        for (Map.Entry<String, String> entry : deployParmeters.getOverridingParameters().entrySet())
            if (entry.getKey().contains("floatingipid") || entry.getKey().contains("floating_ip_id")) {
                openStackClient.releaseFloatingIP(entry.getValue());
                LogUtils.log(listener, "Released Floating IP with ID: " + entry.getValue());
            }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl(String name, String version, String groupid, OpenstackParameters osParameters) {
            super();
        }

        public DescriptorImpl() {

            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Deploy Product";
        }
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return true;
        }
    }

}
