package org.jenkinsci.plugins.os_ci.model.Nexus;

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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * created by agrosmar on 07/01/15
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "stagedRepositoryIds",
        "description",
        "autoDropAfterRelease"
})
public class StagingActionData {

    @JsonProperty("stagedRepositoryIds")
    private String[] stagedRepositoryIds;
    @JsonProperty("description")
    private String description;
    @JsonProperty("autoDropAfterRelease")
    private boolean autoDropAfterRelease;





    public StagingActionData withStagedRepositoryId(String [] stagedRepositoryIdw) {
        this.stagedRepositoryIds = stagedRepositoryIds;
        return this;
    }

    /**
     * @return The description
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * @param description The description
     */
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("stagedRepositoryIds")
    public String[] getStagedRepositoryIds() {
        return stagedRepositoryIds;
    }

    @JsonProperty("stagedRepositoryIds")
    public void setStagedRepositoryIds(String[] stagedRepositoryIds) {
        this.stagedRepositoryIds = stagedRepositoryIds;
    }

    @JsonProperty("autoDropAfterRelease")
    public boolean isAutoDropAfterRelease() {
        return autoDropAfterRelease;
    }

    @JsonProperty("autoDropAfterRelease")
    public void setAutoDropAfterRelease(boolean autoDropAfterRelease) {
        this.autoDropAfterRelease = autoDropAfterRelease;
    }

    public StagingActionData withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


}
