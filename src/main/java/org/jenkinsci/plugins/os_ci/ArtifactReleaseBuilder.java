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

package org.jenkinsci.plugins.os_ci;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;
import org.jenkinsci.plugins.os_ci.model.Nexus.StagingAction;
import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;
import org.jenkinsci.plugins.os_ci.utils.LogUtils;
import org.jenkinsci.plugins.os_ci.utils.VersionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;

public class ArtifactReleaseBuilder extends Builder {

    private String groupId;
    private String artifactId;
    private String osVersion;

    @DataBoundConstructor
    public ArtifactReleaseBuilder(String groupId, String artifactId,String osVersion) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.osVersion = osVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getArtifactId() {

        return artifactId;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        ArtifactParameters myProduct = new ArtifactParameters(groupId, artifactId, VersionUtils.getVersionParameterValue(build),osVersion);
        NexusClient nexusClient = new NexusClient( myProduct,build,listener);

        // Promote to 'release' if promote parameter set to true
        if( getPromoteParameterValue(build)){
            try {
                nexusClient.performStagingAction(StagingAction.PROMOTE);
            } catch (Exception e) {
                LogUtils.log(listener, "Exception during promotion. eror: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else{
            try {
                nexusClient.performStagingAction(StagingAction.DROP);
            } catch (Exception e) {
                LogUtils.log(listener, "Exception during drop. eror: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return true;
    }
    private boolean getPromoteParameterValue(AbstractBuild build) {
        List<ParametersAction> actions =  build.getActions(ParametersAction.class);
        if (actions == null) {
            throw new OsCiPluginException("Boolean job parameter called 'promote' is required but missing");
        }
        ParameterValue promoteValue = null;
        for (ParametersAction action : actions) {
            promoteValue = action.getParameter("promote");
            if (promoteValue != null)
                break;
        }
        if (promoteValue == null) {
            throw new OsCiPluginException("Boolean job parameter called 'promote' is required but missing");
        }
        if (promoteValue.getClass() != BooleanParameterValue.class) {
            throw new OsCiPluginException("Job parameter called 'promote' exists, but is not boolean as should");
        }
        return ((BooleanParameterValue)promoteValue).value;
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
            return "Promote or Drop Artifact";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return true;
        }
    }
}
