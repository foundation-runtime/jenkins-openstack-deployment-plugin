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
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;
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
import java.util.HashMap;
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

            product.cleanOpenstackBeforeDeployment(nexusClient, osParameters, getDescriptor());

            product.deploy(nexusClient, openStackClient, yumRepoParameters, getDescriptor());

        } catch (IOException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            releaseIPs(listener);
            return false;
        } catch (OsCiPluginException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            e.printStackTrace();
            releaseIPs(listener);
            return false;
        } catch (InterruptedException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            e.printStackTrace();
            releaseIPs(listener);
            return false;
        } catch (TimeoutException e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            LogUtils.log(listener, "Timeout Exception: check if all stacks have been deleted : " + e.getMessage());
            e.printStackTrace();
            releaseIPs(listener);
            return false;
        } catch (Exception e) {
            LogUtils.log(listener, "Got an error! : " + e.getMessage());
            e.printStackTrace();
            releaseIPs(listener);
            return false;
        }
        return true;
    }

    private void releaseIPs(BuildListener listener)
    {

        LogUtils.log(listener, "Release Floating IPs:");
        OpenStackClient openStackClient = new OpenStackClient(listener, osParameters.getOpenstackUser(), osParameters.getOpenstackPassword(), osParameters.getOpenstackTenant(), "/v2.0/tokens", osParameters.getOpenstackIP(), 5000);
        for (Map.Entry<String, String> entry : getDescriptor().getOverridingParameters().entrySet())
            if (entry.getKey().contains("floatingipid") || entry.getKey().contains("floating_ip_id"))
                openStackClient.releaseFloatingIP(entry.getValue());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private int deployCounter = 0;
        private Map<String, String> globalOutputs ;
        private Map<String, String> overridingParameters;

        public DescriptorImpl(String name, String version, String groupid, OpenstackParameters osParameters) {
            super();
            deployCounter = 0;
            globalOutputs = new HashMap<String, String>();
            overridingParameters = new HashMap<String, String>();
        }

        public DescriptorImpl() {
            globalOutputs = new HashMap<String, String>();
            overridingParameters = new HashMap<String, String>();
            load();
        }

        public void setGlobalOutputs(Map<String, String> globalOutputs) {
            this.globalOutputs = globalOutputs;
        }

        public void increaseDeployCounter() {
            deployCounter = +1;
        }

        public void resetDeployCounter() {
            deployCounter = 0;
        }

        public int getDeployCounter() {
            return deployCounter;
        }

        public void setGlobalOutputsWithNewOutputs(Map<String, String> newGlobalOutputs) {
            this.globalOutputs.putAll(newGlobalOutputs);
        }

        public Map<String, String> getGlobalOutputs() {
            return globalOutputs;
        }

        public Map<String, String> getOverridingParameters() {
            return overridingParameters;
        }

        public void setOverridingParameters(Map<String, String> overridingParameters) {
            this.overridingParameters = overridingParameters;
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
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            deployCounter = 0;
            globalOutputs = new HashMap<String, String>();
            overridingParameters = new HashMap<String, String>();
            save();
            return true;
        }

    }

}
