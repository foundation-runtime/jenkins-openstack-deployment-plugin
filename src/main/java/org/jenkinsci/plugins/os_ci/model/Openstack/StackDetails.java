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
        "stackStatus",
        "outputs",
})
public class StackDetails {

    @JsonProperty("stackHref")
    private String stackHref;
    @JsonProperty("stackId")
    private String stackId;
    @JsonProperty("stackStatus")
    private StackStatus stackStatus;
    @JsonProperty("parameters")
    private Map<String, String> parameters= new HashMap<String, String>();
    @JsonProperty("outputs")
    private Map<String, String> outputs = new HashMap<String, String>();


    public StackDetails() {
        this.stackStatus = StackStatus.UNDEFINED;
    }

    public StackDetails(String stackHref, String stackId, StackStatus stackStatus, Map<String, String> parameters, Map<String, String> outputs) {
        this.stackHref = stackHref;
        this.stackId = stackId;
        this.stackStatus = stackStatus;
        this.parameters.putAll(parameters);
        this.outputs.putAll(outputs);
    }
    @JsonProperty("stackHref")
    public String getStackHref() {
        return stackHref;
    }
    @JsonProperty("stackHref")
    public void setStackHref(String stackHref) {
        this.stackHref = stackHref;
    }

    @JsonProperty("stackId")
    public String getStackId() {
        return stackId;
    }
    @JsonProperty("stackId")
    public void setStackId(String stackId) {
        this.stackId = stackId;
    }
    @JsonProperty("stackStatus")
    public StackStatus getStackStatus() {
        return stackStatus;
    }
    @JsonProperty("stackStatus")
    public void setStackStatus(StackStatus stackStatus) {
        this.stackStatus = stackStatus;
    }
    @JsonProperty("parameters")
    public Map<String, String> getParameters() {
        return parameters;
    }
    @JsonProperty("outputs")
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    @JsonProperty("outputs")
    public Map<String, String> getOutputs() {
        return outputs;
    }
    @JsonProperty("outputs")
    public void setOutputs(Map<String, String> outputs) {
        this.outputs = outputs;
    }

    @Override
    public String toString() {
        return "StackDetails{" +
                "stackHref=" + stackHref +  '\'' +
                ", stackId=" + stackId +  '\'' +
                ", stackStatus=" + stackStatus +  '\'' +
                ", parameters=" + parameters +  '\'' +
                ", outputs=" + outputs +
                '}';
    }
}
