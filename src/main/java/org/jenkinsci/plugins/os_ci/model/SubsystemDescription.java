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
public class SubsystemDescription implements Describable<SubsystemDescription> {
    private String name;

    public String getName() { return name; }

    public void setName(String name) {
        this.name = name;
    }

    @DataBoundConstructor
    public SubsystemDescription(String name) {
        this.name = name;
    }

    public Descriptor<SubsystemDescription> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SubsystemDescription>
    {
        @Override
        public String getDisplayName()
        {
            return "Subsystem names:";
        }
    }
}
