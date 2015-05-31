package org.jenkinsci.plugins.os_ci.model.Openstack;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by agrosmar on 5/11/2015.
 */
public class DeployParmeters {

    Map<String, String> globalOutputs = new HashMap<String, String>();
    Map<String, String> overridingParameters = new HashMap<String, String>();
    int deployCounter = 0;


    public DeployParmeters(Map<String, String> globalOutputs, Map<String, String> overridingParameters, int deployCounter) {
        this.globalOutputs = globalOutputs;
        this.overridingParameters = overridingParameters;
        this.deployCounter = deployCounter;
    }

    public DeployParmeters() {
    }

    public Map<String, String> getGlobalOutputs() {
        return globalOutputs;
    }

    public void setGlobalOutputs(Map<String, String> globalOutputs) {
        this.globalOutputs = globalOutputs;
    }

    public Map<String, String> getOverridingParameters() {
        return overridingParameters;
    }

    public void setOverridingParameters(Map<String, String> overridingParameters) {
        this.overridingParameters = overridingParameters;
    }

    public int getDeployCounter() {
        return deployCounter;
    }

    public void setDeployCounter(int deployCounter) {
        this.deployCounter = deployCounter;
    }

    public void increaseDeployCounter() {
        deployCounter = +1;
    }

    public void resetDeployCounter() {
        deployCounter = 0;
    }

    public void setGlobalOutputsWithNewOutputs(Map<String, String> newGlobalOutputs) {
        this.globalOutputs.putAll(newGlobalOutputs);
    }
}
