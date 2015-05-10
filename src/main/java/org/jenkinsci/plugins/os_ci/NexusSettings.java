package org.jenkinsci.plugins.os_ci;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

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
 */public class NexusSettings extends JobProperty<Job<?, ?>> {

    @Override
    public NexusSettingsDescriptor getDescriptor() {
        return (NexusSettingsDescriptor) Jenkins.getInstance().getDescriptor(getClass());
    }

    public static NexusSettingsDescriptor getNexusSettingsDescriptor() {
        return (NexusSettingsDescriptor) Jenkins.getInstance().getDescriptor(NexusSettings.class);
    }


    @Extension
    public static final class NexusSettingsDescriptor extends JobPropertyDescriptor {

        private String nexusUrl;
        private String nexusDocUrl;
        private String nexusUser;
        private Secret nexusPassword;

        public NexusSettingsDescriptor() {
            super(NexusSettings.class);
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
//            nexusUrl = formData.getString("nexusUrl");
//            nexusUser = formData.getString("nexusUser");
//            nexusPassword = Secret.fromString( formData.getString("nexusPassword") );
            save();
            return super.configure(req, formData);
        }

        @DataBoundConstructor
        public NexusSettingsDescriptor(String nexusUrl, String nexusDocUrl, String nexusUser, Secret nexusPassword) {
            this.nexusUrl = nexusUrl;
            this.nexusDocUrl = nexusDocUrl;
            this.nexusUser = nexusUser;
            this.nexusPassword = nexusPassword;
            load();
        }

        @Override
        public String getDisplayName() {
            return "Nexus Credentials";
        }

        public FormValidation doTestConnection(
                @QueryParameter("nexusUrl") final String nexusUrl,
                @QueryParameter("nexusUser") final String nexusUser,
                @QueryParameter("nexusPassword") final String nexusPassword) throws IOException, ServletException {
            try {
                NexusClient nexusClient = new NexusClient(null, null, null);

                if( nexusClient.testConnection(nexusUrl, nexusUser, Secret.fromString( nexusPassword )) ) {
                    return FormValidation.ok("Success. Connection with Nexus Repository verified.");
                }
                return FormValidation.error("Failed. Please check the configuration. HTTP Status: isn't 200" );
            } catch (Exception e) {
                System.out.println("Exception " + e.getMessage() );
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }
        public String getNexusUrl() {
            return nexusUrl;
        }
        public String getNexusUser() {
            return nexusUser;
        }
        public Secret getNexusPassword() {
            return nexusPassword;
        }

        public String getNexusDocUrl() { return nexusDocUrl; }

        public void setNexusDocUrl(String nexusDocUrl) { this.nexusDocUrl = nexusDocUrl; }

        public void setNexusUrl(String nexusUrl) {
            this.nexusUrl = nexusUrl;
        }

        public void setNexusUser(String nexusUser) {
            this.nexusUser = nexusUser;
        }

        public void setNexusPassword(Secret nexusPassword) {
            this.nexusPassword = nexusPassword;
        }
    }
}