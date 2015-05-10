package org.jenkinsci.plugins.os_ci.model;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
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
public class YumRepoParameters implements Describable<YumRepoParameters> {

    private final String yumRepoPrivateKey;
    private final String yumRepoIP;

    @DataBoundConstructor
    public YumRepoParameters(String yumRepoPrivateKey, String yumRepoIP) {
        this.yumRepoPrivateKey = yumRepoPrivateKey;
        this.yumRepoIP = yumRepoIP;
    }

    @Override
    public String toString() {
        return "YumRepoParameters{" +
                "yumRepoPrivateKey='" + yumRepoPrivateKey.substring(0,10) + "...'" +
                ", yumRepoIP='" + yumRepoIP + '\'' +
                '}';
    }

    public String getYumRepoPrivateKey() {
        return yumRepoPrivateKey;
    }

    public String getYumRepoIP() {
        return yumRepoIP;
    }

    public Descriptor<YumRepoParameters> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }
    @Extension
    public static class DescriptorImpl extends Descriptor<YumRepoParameters>
    {
        @Override
        public String getDisplayName()
        {
            return "YUM Repo Parameters:";
        }
    }

}
