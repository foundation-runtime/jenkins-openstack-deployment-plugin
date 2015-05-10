package org.jenkinsci.plugins.os_ci.model.Openstack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by erivni on 16/12/2014.
 */

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonPropertyOrder({
        "token",
        "tenantID",
        "serviceCatalog"
})
public class EntryPoints {

    @JsonProperty("token")
    private String token;
    @JsonProperty("tenantID")
    private String tenantID;
    @JsonProperty("serviceCatalog")
    private Map<String, String> serviceCatalog = new HashMap<String, String>();

    public EntryPoints() {
    }

    public EntryPoints(String token, String tenantID, Map<String, String> serviceCatalog){
        this.token = token;
        this.tenantID = tenantID;
        this.serviceCatalog.putAll(serviceCatalog);
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("tenantID")
    public String getTenantID() {
        return tenantID;
    }

    @JsonProperty("tenantID")
    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    @JsonProperty("serviceCatalog")
    public Map<String, String> getServiceCatalog() {
        return serviceCatalog;
    }

    @JsonProperty("serviceCatalog")
    public void setServiceCatalog(Map<String, String> serviceCatalog) {
        this.serviceCatalog.putAll(serviceCatalog);
    }

    @Override
    public String toString() {
        return "EntryPoints{" +
                "token='" + token + '\'' +
                ", tenantID='" + tenantID + '\'' +
                ", serviceCatalog=" + serviceCatalog +
                '}';
    }
}
