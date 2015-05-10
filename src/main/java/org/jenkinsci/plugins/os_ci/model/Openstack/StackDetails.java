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

    @JsonProperty("stackStatus")
    private StackStatus stackStatus;
    @JsonProperty("outputs")
    private Map<String, String> outputs = new HashMap<String, String>();

    public StackDetails() {
    }

    public StackDetails(StackStatus stackStatus, Map<String, String> outputs) {
        this.stackStatus = stackStatus;
        this.outputs.putAll(outputs);
    }

    @JsonProperty("stackStatus")
    public StackStatus getStackStatus() {
        return stackStatus;
    }
    @JsonProperty("stackStatus")
    public void setStackStatus(StackStatus stackStatus) {
        this.stackStatus = stackStatus;
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
                "stackStatus=" + stackStatus +
                ", outputs=" + outputs +
                '}';
    }
}
