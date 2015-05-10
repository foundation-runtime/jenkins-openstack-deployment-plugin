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
public class NodeConfiguration implements Describable<NodeConfiguration> {

    private String name;
    private String imageName;
    private String imageId;
    private String flavorName;
    private String flavorId;
    private String quantityMin;
    private String quantityMax;

    @DataBoundConstructor
    public NodeConfiguration(String name, String imageName, String imageId, String flavorName,String flavorId, String quantityMin, String quantityMax) {
        this.name = name;
        this.imageName = convertToValidString(imageName);
        this.imageId = convertToValidString(imageId);
        this.flavorName = convertToValidString(flavorName);
        this.flavorId = convertToValidString(flavorId);
        this.imageName = convertToValidString(imageName);
        if (Integer.valueOf(quantityMin) > Integer.valueOf(quantityMax))
            quantityMin = quantityMax;
        this.quantityMin = quantityMin;
        this.quantityMax = quantityMax;
    }

    private String convertToValidString(String orig) {
        if (orig == null)
            return "";
        else
            return orig;
    }

    public Descriptor<NodeConfiguration> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }
    @Extension
    public static class DescriptorImpl extends Descriptor<NodeConfiguration> {

        @Override
        public String getDisplayName() {
            return "Node Configuration";
        }

        public ListBoxModel doFillQuantityMaxItems() {
            ListBoxModel items = new ListBoxModel();
            for (int i=1; i<=10; i++) {
                items.add(String.valueOf(i),String.valueOf(i));
            }
            return items;
        }

        public ListBoxModel doFillQuantityMinItems() {
            ListBoxModel items = new ListBoxModel();
            for (int i=1; i<=10; i++) {
                items.add(String.valueOf(i),String.valueOf(i));
            }
            return items;
        }
    }

    public String getQuantityMin() {
        return quantityMin;
    }

    public void setQuantityMin(String quantityMin) {
        this.quantityMin = quantityMin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = convertToValidString(imageName);
    }

    public String getFlavorName() {
        return flavorName;
    }

    public void setFlavorName(String flavorName) {
        this.flavorName = convertToValidString(flavorName);
    }

    public String getQuantityMax() {
        return quantityMax;
    }

    public void setQuantityMax(String quantityMax) {
        this.quantityMax = quantityMax;
    }

    public String getFlavorId() {
        return flavorId;
    }

    public void setFlavorId(String flavorId) {
        this.flavorId = convertToValidString(flavorId);
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = convertToValidString(imageId);
    }

}
