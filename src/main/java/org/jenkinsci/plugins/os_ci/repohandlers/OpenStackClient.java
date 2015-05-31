package org.jenkinsci.plugins.os_ci.repohandlers;

import com.fasterxml.jackson.databind.JsonNode;
import hudson.model.BuildListener;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.*;
import org.jenkinsci.plugins.os_ci.model.Openstack.EntryPoints;
import org.jenkinsci.plugins.os_ci.model.Openstack.FloatingIP;
import org.jenkinsci.plugins.os_ci.model.Openstack.StackDetails;
import org.jenkinsci.plugins.os_ci.model.Openstack.StackStatus;
import org.jenkinsci.plugins.os_ci.openstack.TemplateUtils;
import org.jenkinsci.plugins.os_ci.utils.JsonBuilder;
import org.jenkinsci.plugins.os_ci.utils.JsonParser;
import org.jenkinsci.plugins.os_ci.utils.LogUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Copyright 2015 Cisco Systems, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class OpenStackClient {

    private CloseableHttpClient httpClient = HttpClients.createDefault();
    private EntryPoints entryPoints = new EntryPoints();
    private BuildListener listener;
    private String openstackUser;
    private String openstackPassword;
    private String openstackTenantName;
    private String openstackPath;
    private String openstackHost;
    private int openstackPort;
    private boolean renewed = false;

    public OpenStackClient(final BuildListener listener, final String openstackUser, final String openstackPassword, final String openstackTenantName, final String path, final String openstackHost, int port) {
        this.listener = listener;
        this.openstackUser = openstackUser;
        this.openstackHost = openstackHost;
        this.openstackPassword = openstackPassword;
        this.openstackTenantName = openstackTenantName;
        this.openstackPath = path;
        this.openstackPort = port;
        generateToken();
    }

    public void generateToken() {
        GetToken gett = new GetToken();
        Auth auth = new Auth();
        PasswordCredentials pass = new PasswordCredentials();
        pass.setPassword(openstackPassword);
        pass.setUsername(openstackUser);
        auth.setPasswordCredentials(pass);
        auth.setTenantName(openstackTenantName);
        gett.setAuth(auth);
        try {
            String body = JsonBuilder.createJson(gett);

            URI uri = new URIBuilder().setScheme("http").setHost(openstackHost).setPort(openstackPort).setPath(openstackPath).build();
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Accept", "application/json");
            final StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                int status = response.getStatusLine().getStatusCode();

                if (status != 200) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                this.entryPoints = JsonParser.parseGetTokenAndEntryPoints(response);
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getToken " + e.getMessage());
        } catch (Exception e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        }
    }

    public void renewToken() {
        GetToken getToken = new GetToken();
        Auth auth = new Auth();
        PasswordCredentials pass = new PasswordCredentials();
        pass.setPassword(openstackPassword);
        pass.setUsername(openstackUser);
        auth.setPasswordCredentials(pass);
        auth.setTenantName(openstackTenantName);
        getToken.setAuth(auth);
        String token = null;
        try {
            String body = JsonBuilder.createJson(getToken);

            URI uri = new URIBuilder().setScheme("http").setHost(openstackHost).setPort(openstackPort).setPath(openstackPath).build();
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Accept", "application/json");
            final StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    LogUtils.log(listener, "Request body: " + body + "\nResponse: " + JsonParser.readResponseBody(response));
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                token = JsonParser.parseRenewToken(response);
                this.entryPoints.setToken(token);
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getToken " + e.getMessage());
        } catch (Exception e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        }
    }

    public EntryPoints getEntryPoints() {
        return entryPoints;
    }

    public void setEntryPoints(EntryPoints entryPoints) {
        this.entryPoints = entryPoints;
    }

    public String createStack(final String stackName, Map<String, String> overridingParams, Map<String, String> prevStacksOutputs, final String basePath)
            throws Exception {
        CreateStack cs = new CreateStack();
        cs.setStackName(stackName);
        StackTemplatesData std = TemplateUtils.readStackTemplate(stackName, basePath);

        cs.setTemplate(std.getContent());
        cs.setFiles(std.getDependencies());
        Map<String, Object> params = ((Map<String, Object>) ((Map<String, Object>) new Yaml().load(std.getEnvContent())).get("parameters"));
        Map<String, Object> mainParams = ((Map<String, Object>) ((Map<String, Object>) new Yaml().load(std.getContent())).get("parameters"));

        LogUtils.logSection(listener, "Creating " + stackName + " Stack");

        LogUtils.log(listener, "Overriding parameters");
        overrideParameters(params, mainParams, overridingParams);
        LogUtils.log(listener, "Get previous outputs");
        overrideParameters(params, mainParams, prevStacksOutputs);

        cs.setParameters(params);


        LogUtils.log(listener, "Parameters: " + params);

        String body = JsonBuilder.createJson(cs);
        try {

            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("heat") + "stacks").build();
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("X-Auth-Token", entryPoints.getToken());
            httpPost.addHeader("X-Auth-User",openstackUser );
            httpPost.addHeader("X-Auth-Key",openstackPassword );

            final StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpPost.removeHeaders("X-Auth-Token");
                    httpPost.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpPost);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 201) {
                    LogUtils.log(listener, "Request body: " + body + "\nResponse: " + JsonParser.readResponseBody(response));
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                return JsonParser.getStackLink(response);
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getToken " + e.getMessage());
        }
    }

    private void overrideParameters(Map<String, Object> params, Map<String, Object> mainParams, Map<String, String> overridingParams) {
        if (overridingParams != null && !overridingParams.isEmpty()) {
            for (Map.Entry<String, String> entry : overridingParams.entrySet()) {
                // if key is in env.yaml or in main.yaml - override
                if (params.containsKey(entry.getKey()) || mainParams.containsKey(entry.getKey())) {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public StackDetails getStackDetails(final String stackName)
            throws Exception {

        // GET http://10.56.165.71:8004/v1/<jenkins-tenant_id>/stacks/<stack_name>
        try {

            StackDetails stackDetails = new StackDetails();
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("heat") + "stacks").build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpGet.addHeader("X-Auth-User",openstackUser );
            httpGet.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpGet.removeHeaders("X-Auth-Token");
                    httpGet.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpGet);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 200) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }

                JsonNode stackJson = JsonParser.parseGetStackDetails(response, stackName);

                stackDetails.setStackHref(stackJson.get("links").elements().next().get("href").textValue());
                stackDetails.setStackId(stackJson.get("id").textValue());
                stackDetails.setStackStatus(StackStatus.valueOf(stackJson.get("stack_status").textValue()));

                // fill parameters and outputs to stackDetails.
                uri = new URIBuilder().setPath(stackDetails.getStackHref()).build();
                httpGet = new HttpGet(uri);
                httpGet.addHeader("X-Auth-Token", this.entryPoints.getToken());

                response = httpClient.execute(httpGet);


                status = response.getStatusLine().getStatusCode();

                if (status != 200 && status != 400) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }

                if (status == 200) {
                    // fill parameters
                    stackJson = JsonParser.parseGetFullStackDetails(response);

                    Map<String, String> parameters = new HashMap<String, String>();
                    Iterator<Map.Entry<String, JsonNode>> parameters_elements = stackJson.get("parameters").fields();

                    while (parameters_elements.hasNext()) {
                        Map.Entry<String, JsonNode> parameter_element = parameters_elements.next();
                        String key = parameter_element.getKey();
                        String value = parameter_element.getValue().textValue();
                        if (value == null)
                            value = parameter_element.getValue().toString();
                        parameters.put(key, value);
                    }
                    stackDetails.setParameters(parameters);

                    // fill outputs
                    Map<String, String> outputs = new HashMap<String, String>();
                    if (stackDetails.getStackStatus() == StackStatus.CREATE_COMPLETE) {
                        Iterator<JsonNode> outputs_elements = stackJson.get("outputs").elements();

                        while (outputs_elements.hasNext()) {
                            JsonNode output_element = outputs_elements.next();
                            String key = output_element.get("output_key").textValue();
                            String value = output_element.get("output_value").textValue();
                            LogUtils.log(listener, "Stack " + stackName + " Output info: "+key +" with value - "+value);
                            if (value == null)
                                value = output_element.get("output_value").toString();
                            outputs.put(key, value);
                        }
                    }
                    stackDetails.setOutputs(parameters);

                }
                else if (status == 400)
                {
                    LogUtils.log(listener, "Stack " + stackName + " exists, but filed to get its details. Please check your stack outputs: " +
                            JsonParser.parseGetStackError(response));
                }

                return stackDetails;

            } finally {
                response.close();
            }
        }catch (NullPointerException e){
            return new StackDetails();
        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getStackDetails " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getStackDetails " + e.getMessage());
        }
    }

    public StackStatus getStackStatus(final String stackName)
            throws Exception {

        // get stack details and check its status
        try {

            return getStackDetails(stackName).getStackStatus();

        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getStackStatus " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getStackStatus " + e.getMessage());
        }
    }

    /*public boolean stackExists(final String stackName)
            throws Exception {

        // GET http://10.56.165.71:8004/v1/<jenkins-tenant_id>/stacks
        try {
            return getStackDetails(stackName).getStackStatus() != StackStatus.UNDEFINED;

        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getToken " + e.getMessage());
        }
    }*/

    public void deleteStack(final StackDetails stackDetails)
            throws Exception {

        // delete http://10.56.165.71:8004/v1/<jenkins-tenant_id>/stacks/<stack_name>/<stack_id>
        try {
            URI uri = new URIBuilder().setPath(stackDetails.getStackHref()).build();
            HttpDelete httpDelete = new HttpDelete(uri);
            httpDelete.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpDelete.addHeader("X-Auth-User",openstackUser );
            httpDelete.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpDelete);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpDelete.removeHeaders("X-Auth-Token");
                    httpDelete.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpDelete);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 204) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }

            } finally {
                response.close();
            }

        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getStackDetails " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getStackDetails " + e.getMessage());
        }
    }


    public Map<String, String> getStackOutputs(final String stackName)
            throws Exception {

        // get stack details and check its status
        try {

            return getStackDetails(stackName).getOutputs();

        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getStackStatus " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getStackStatus " + e.getMessage());
        }
    }

    public List<SelectItem> geListOfPublicNetworks() {

        // GET http://10.56.165.71:9696/v2.0/networks
        try {
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("neutron") + "v2.0/networks").build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpGet.addHeader("X-Auth-User",openstackUser );
            httpGet.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpGet.removeHeaders("X-Auth-Token");
                    httpGet.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpGet);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 200) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                return JsonParser.getAllPublicNetworks(response);

            } finally {
                response.close();
            }
        } catch (Exception e) {
            return new ArrayList<SelectItem>();
        }
    }

    public List<SelectItem> getListOfPrivateNetworks() {

        // GET http://10.56.165.71:9696/v2.0/networks
        try {
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("neutron") + "v2.0/networks").build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpGet.addHeader("X-Auth-User",openstackUser );
            httpGet.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpGet.removeHeaders("X-Auth-Token");
                    httpGet.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpGet);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 200) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                return JsonParser.getAllPrivateNetworks(response);

            } finally {
                response.close();
            }
        } catch (Exception e) {
            return new ArrayList<SelectItem>();
        }
    }

    public List<SelectItem> getListOfSubnetsIdsByNetworkName(final String networkName) {

        // GET http://10.56.165.71:9696/v2.0/networks
        try {
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("neutron") + "v2.0/networks").build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpGet.addHeader("X-Auth-User",openstackUser );
            httpGet.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpGet.removeHeaders("X-Auth-Token");
                    httpGet.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpGet);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 200) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                return JsonParser.getAllNetwrokSubnetsIds(response, networkName);

            } finally {
                response.close();
            }
        } catch (Exception e) {
            return new ArrayList<SelectItem>();
        }
    }

    public List<SelectItem> getListOfImages() {

        // GET http://10.56.165.71:9292/v2/images
        try {
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("glance") + "v2/images").build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpGet.addHeader("X-Auth-User",openstackUser );
            httpGet.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpGet.removeHeaders("X-Auth-Token");
                    httpGet.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpGet);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 200) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                return JsonParser.getAllImages(response);

            } finally {
                response.close();
            }
        } catch (Exception e) {
            return new ArrayList<SelectItem>();
        }
    }

    public List<SelectItem> getListOfAvailibilityZones() {

        // GET // delete http://10.56.165.71:8774/v2/<jenkins-tenant_id>/os-aggregates
        try {
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("nova") + "os-aggregates").build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpGet.addHeader("X-Auth-User",openstackUser );
            httpGet.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpGet.removeHeaders("X-Auth-Token");
                    httpGet.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpGet);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 200) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                return JsonParser.getListOfAvailibilityZones(response);

            } finally {
                response.close();
            }
        } catch (Exception e) {
            return new ArrayList<SelectItem>();
        }
    }

    public List<SelectItem> getListOfKeypairs() {

        // GET  http://10.56.165.71:8774/v2/<jenkins-tenant_id>/os-aggregates
        try {
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("nova") + "os-keypairs").build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpGet.addHeader("X-Auth-User",openstackUser );
            httpGet.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpGet.removeHeaders("X-Auth-Token");
                    httpGet.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpGet);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 200) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }
                return JsonParser.getListOfKeypairs(response);

            } finally {
                response.close();
            }
        } catch (Exception e) {
            return new ArrayList<SelectItem>();
        }

    }


    public FloatingIP createFloatingIP(String public_network_name) {

        // POST  http://10.56.165.71:8774/v2/<jenkins-tenant>/os-floating-ips
        try {
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("nova") + "os-floating-ips").build();
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("X-Auth-Token", entryPoints.getToken());
            httpPost.addHeader("X-Auth-User",openstackUser );
            httpPost.addHeader("X-Auth-Key",openstackPassword );

            String body = "{ \"pool\": \"" + public_network_name + "\" }";

            final StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpPost.removeHeaders("X-Auth-Token");
                    httpPost.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpPost);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 200) {
                    LogUtils.log(listener, "Request body: " + body + "\nResponse: " + JsonParser.readResponseBody(response));
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }

                return JsonParser.getFloatingIP(response);
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getToken " + e.getMessage());
        }

    }

    public void releaseStackFloatingIPs(String stackName) throws Exception {

        List<String> floatingIPs = JsonParser.getFloatingIPIdsFromStackDetails(getStackDetails(stackName));
        for (String floating_ip_id : floatingIPs)
            releaseFloatingIP(floating_ip_id);
    }


    public void releaseFloatingIP(String floating_ip_id) {

        // delete  http://10.56.165.71:9696/v2.0/floatingips/<floating_ip_id>
        try {
            URI uri = new URIBuilder().setPath(this.entryPoints.getServiceCatalog().get("neutron") + "v2.0/floatingips/" + floating_ip_id).build();
            HttpDelete httpDelete = new HttpDelete(uri);
            httpDelete.addHeader("X-Auth-Token", this.entryPoints.getToken());
            httpDelete.addHeader("X-Auth-User",openstackUser );
            httpDelete.addHeader("X-Auth-Key",openstackPassword );

            CloseableHttpResponse response = httpClient.execute(httpDelete);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 401 && !renewed) {
                    renewed = true;
                    renewToken();
                    httpDelete.removeHeaders("X-Auth-Token");
                    httpDelete.addHeader("X-Auth-Token", entryPoints.getToken());
                    response = httpClient.execute(httpDelete);
                    status = response.getStatusLine().getStatusCode();
                }
                if (status != 204 && status != 404) {
                    throw new OsCiPluginException("Invalid OpenStack server response " + status);
                }

            } finally {
                response.close();
            }

        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getStackDetails " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getStackDetails " + e.getMessage());
        }

    }

	/*public static String valitadeTemplate(final EntryPoints entryPoints, final String stackName, final String basePath)
            throws Exception {
		CreateStack cs = new CreateStack();
		cs.setStackName(stackName);
		StackTemplatesData std = TemplateUtils.readStackTemplate(stackName, basePath);

		cs.setTemplate(std.getContent());
		cs.setFiles(std.getDependencies());

		Map<String, Object> parameters = new HashMap<String, Object>();
		ParametersUtils.getParameterAsMap(stackName,basePath);
		parameters.put(stackName,basePath);
		cs.setParameters(parameters);

		String body = JsonBuilder.createJson(cs);
		try {
			URI uri = new URIBuilder().setPath(entryPoints.getServiceCatalog().get("heat") + "/validate").build();
			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader("X-Auth-Token", entryPoints.getToken());
			final StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
			httpPost.setEntity(entity);
			CloseableHttpResponse response = httpClient.execute(httpPost);
			try {
				int status = response.getStatusLine().getStatusCode();
				if (status != 200) {
					throw new OsCiPluginException("Invalid OpenStack server response " + status);
				}
				return entryPoints.getToken();
			} finally {
				response.close();
			}
		} catch (IOException e) {
			throw new OsCiPluginException("IOException in getToken " + e.getMessage());
		} catch (URISyntaxException e) {
			throw new OsCiPluginException("URISyntaxException in getToken " + e.getMessage());
		}
	}*/

}
