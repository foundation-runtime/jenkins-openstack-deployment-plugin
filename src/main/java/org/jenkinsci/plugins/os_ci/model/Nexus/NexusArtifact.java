package org.jenkinsci.plugins.os_ci.model.Nexus;



import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;
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
@JsonInclude(JsonInclude.Include.NON_NULL)

@JsonPropertyOrder({
        "groupId",
        "artifactId",
        "version",
        "packaging",
        "profileId",
        "repositoryId"
})
public class NexusArtifact {

    @JsonProperty("groupId")
    private String groupId;
    @JsonProperty("artifactId")
    private String artifactId;
    @JsonProperty("version")
    private String version;
    @JsonProperty("packaging")
    private String packaging;
    @JsonProperty("profileId")
    private String profileId;
    @JsonProperty("repositoryId")
    private String repositoryId;

    /**
     *
     * @return
     * The groupId
     */
    @JsonProperty("groupId")
    public String getGroupId() {
        return groupId;
    }

    /**
     *
     * @param groupId
     * The groupId
     */
    @JsonProperty("groupId")
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public NexusArtifact withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     *
     * @return
     * The artifactId
     */
    @JsonProperty("artifactId")
    public String getArtifactId() {
        return artifactId;
    }

    /**
     *
     * @param artifactId
     * The artifactId
     */
    @JsonProperty("artifactId")
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public NexusArtifact withArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    /**
     *
     * @return
     * The version
     */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    /**
     *
     * @param version
     * The version
     */
    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    public NexusArtifact withVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     *
     * @return
     * The packaging
     */
    @JsonProperty("packaging")
    public String getPackaging() {
        return packaging;
    }

    /**
     *
     * @param packaging
     * The packaging
     */
    @JsonProperty("packaging")
    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public NexusArtifact withPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    /**
     *
     * @return
     * The profileId
     */
    @JsonProperty("profileId")
    public String getProfileId() {
        return profileId;
    }

    /**
     *
     * @param profileId
     * The profileId
     */
    @JsonProperty("profileId")
    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public NexusArtifact withProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    /**
     *
     * @return
     * The repositoryId
     */
    @JsonProperty("repositoryId")
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     *
     * @param repositoryId
     * The repositoryId
     */
    @JsonProperty("repositoryId")
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public NexusArtifact withRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


}

