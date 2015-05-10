package org.jenkinsci.plugins.os_ci.model;



import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class GetToken {

        @JsonProperty("auth")
        private Auth auth;


        @JsonProperty("auth")
        public Auth getAuth() {
            return auth;
        }

        /**
         *
         * @param auth
         * The auth
         */
        @JsonProperty("auth")
        public void setAuth(Auth auth) {
            this.auth = auth;
        }

        public GetToken withAuth(Auth auth) {
            this.auth = auth;
            return this;
        }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }




}
