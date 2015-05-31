package org.jenkinsci.plugins.os_ci.model;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.os_ci.repohandlers.OpenStackClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class OpenstackParameters implements Describable<OpenstackParameters> {
    // basic
    private final String openstackIP;

    // advanced
    private final String openstackTenant;
    private final String openstackPassword;
    private final String openstackUser;
    private final String nameservers;
    private final String privateNetworkID;
    private final String privateSubnetID;
    private final String keyPair;
    private final Map <String,String > additionalParams;
    private final PolicyParameters policyParameters;

    @DataBoundConstructor
    public OpenstackParameters(String openstackIP, String openstackTenant, String openstackPassword,
                               String openstackUser, String privateNetworkID, String privateSubnetID, String keyPair, List<Entry> additionalParams,PolicyParameters policyParameters, String nameservers) {
        this.openstackIP = openstackIP;
        this.openstackTenant = openstackTenant;
        this.openstackPassword = openstackPassword;
        this.openstackUser = openstackUser;
        this.privateNetworkID = privateNetworkID;
        this.privateSubnetID = privateSubnetID;
        this.keyPair = keyPair;
        this.additionalParams = toMap(additionalParams);
        this.policyParameters = policyParameters;
        this.nameservers = nameservers;
    }

    public Map <String, String>  getAdditionalParams() { return additionalParams; }
    public PolicyParameters getPolicyParameters() {
        return policyParameters;
    }

    @Override
    public String toString() {
        return "OpenstackParameters{" +
                "openstackIP='" + openstackIP + '\'' +
                ", openstackTenant='" + openstackTenant + '\'' +
                ", openstackPassword='" + openstackPassword + '\'' +
                ", openstackUser='" + openstackUser + '\'' +
                ", NameServers='" + nameservers + '\'' +
                ", privateNetworkID='" + privateNetworkID + '\'' +
                ", privateSubnetID='" + privateSubnetID + '\'' +
                ", keyPair='" + keyPair + '\'' +
                ", additionalParams=" + additionalParams +
                '}';
    }

    public String getOpenstackIP() {
        return openstackIP;
    }


    public String getOpenstackTenant() {
        return openstackTenant;
    }

    public String getOpenstackPassword() {
        return openstackPassword;
    }

    public String getOpenstackUser() {
        return openstackUser;
    }

    public String getNameservers() {
        return nameservers;
    }

    public String getPrivateNetworkID() {
        return privateNetworkID;
    }

    public String getPrivateSubnetID() {
        return privateSubnetID;
    }

    public String getKeyPair() {
        return keyPair;
    }

    public Descriptor<OpenstackParameters> getDescriptor() { return Hudson.getInstance().getDescriptor(getClass()); }
    @Extension
    public static class DescriptorImpl extends Descriptor<OpenstackParameters>
    {
        @Override
        public String getDisplayName()
        {
            return "Openstack Parameters:";
        }

        public FormValidation doTestOpenstackConnection(@QueryParameter("openstackIP") final String openstackIP,
                                                        @QueryParameter("openstackUser") final String openstackUser,
                                                        @QueryParameter("openstackPassword") final String openstackPassword,
                                                        @QueryParameter("openstackTenant") final String openstackTenant) {
            try {

                OpenStackClient openStackClient = new OpenStackClient(null, openstackUser, openstackPassword, openstackTenant,"/v2.0/tokens",openstackIP,5000);
                load();
                return FormValidation.ok("Success");
            } catch (Exception e) {
                return FormValidation.error("Please check your Openstack credentials");
            }
        }
    }

    public static class Entry {
        public String key, value;
        @DataBoundConstructor
        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
    private static EnvVars toMap(List<Entry> entries) {
        EnvVars map = new EnvVars();
        if (entries!=null)
            for (Entry entry: entries)
                map.put(entry.key,entry.value);
        return map;
    }
}
