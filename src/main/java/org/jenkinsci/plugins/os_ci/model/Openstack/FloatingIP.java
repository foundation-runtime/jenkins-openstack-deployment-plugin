package org.jenkinsci.plugins.os_ci.model.Openstack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Created by erivni on 16/12/2014.
 */

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonPropertyOrder({
        "floatingIpId",
        "floatingIpAddress",
})
public class FloatingIP {

    @JsonProperty("floatingIpId")
    private String floatingIpId;
    @JsonProperty("floatingIpAddress")
    private String floatingIpAddress;

    public FloatingIP() {
    }

    public FloatingIP(String floatingIpId, String floatingIpAddress){
        this.floatingIpId = floatingIpId;
        this.floatingIpAddress = floatingIpAddress;
    }

    @JsonProperty("floatingIpId")
    public String getFloatingIpId() {
        return floatingIpId;
    }

    @JsonProperty("floatingIpId")
    public void setFloatingIpId(String floatingIpId) {
        this.floatingIpId = floatingIpId;
    }

    @JsonProperty("floatingIpAddress")
    public String getFloatingIpAddress() {
        return floatingIpAddress;
    }

    @JsonProperty("floatingIpAddress")
    public void setFloatingIpAddress(String floatingIpAddress) {
        this.floatingIpAddress = floatingIpAddress;
    }

    @Override
    public String toString() {
        return "FloatingIP{" +
                "floatingIpId='" + floatingIpId + '\'' +
                ", floatingIpAddress='" + floatingIpAddress +
                '}';
    }
}
