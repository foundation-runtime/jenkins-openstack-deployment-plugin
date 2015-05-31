package org.jenkinsci.plugins.os_ci.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import hudson.model.BuildListener;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.os_ci.exceptions.ArtifactVersionNotFoundException;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;
import org.jenkinsci.plugins.os_ci.model.Nexus.NexusArtifact;
import org.jenkinsci.plugins.os_ci.model.Openstack.EntryPoints;
import org.jenkinsci.plugins.os_ci.model.Openstack.FloatingIP;
import org.jenkinsci.plugins.os_ci.model.Openstack.StackDetails;
import org.jenkinsci.plugins.os_ci.model.SelectItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class JsonParser {
//    public static String repoId = null;

    public static JsonNode parseGetStackDetails(CloseableHttpResponse response, String stackName) throws JsonProcessingException, IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        Iterator<JsonNode> stacks = mapper.readTree(stringBuffer.toString()).get("stacks").elements();

        StackDetails stack_details = new StackDetails();

        while (stacks.hasNext()) {
            JsonNode stack = stacks.next();
            if (stack.get("stack_name").textValue().equalsIgnoreCase(stackName))
                return stack;
        }

        return null;


    }

    public static JsonNode parseGetFullStackDetails(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode stack_details = mapper.readTree(stringBuffer.toString()).get("stack");
        return stack_details;

    }

    public static String parseGetStackError(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(stringBuffer.toString());
        return requestNode.get("error").get("message").textValue();
    }

    public static EntryPoints parseGetTokenAndEntryPoints(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        EntryPoints entrypoints = new EntryPoints();
        Map<String, String> openstackServices = new HashMap<String, String>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(stringBuffer.toString());
        entrypoints.setToken(requestNode.get("access").get("token").get("id").textValue());
        entrypoints.setTenantID(requestNode.get("access").get("token").get("tenant").get("id").textValue());


        Iterator<JsonNode> serviceCatalogEndPoints = requestNode.get("access").get("serviceCatalog").elements();
        while (serviceCatalogEndPoints.hasNext()) {
            JsonNode endpoint = serviceCatalogEndPoints.next();
            String endpointService = endpoint.get("name").textValue();
            String publicUrl = getPublicUrl(endpoint.get("endpoints"));
            if (!publicUrl.endsWith("/"))
                openstackServices.put(endpointService, (new StringBuilder(publicUrl).append("/").toString()));
            else
                openstackServices.put(endpointService, publicUrl);
        }

        entrypoints.setServiceCatalog(openstackServices);
        return entrypoints;
    }

    public static String parseRenewToken(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));
        return requestNode.get("access").get("token").get("id").textValue();
    }

    public static NexusArtifact parseNexusPomUpload(CloseableHttpResponse response, BuildListener listener) throws JsonProcessingException, IOException {
        NexusArtifact nexusUpload = new NexusArtifact();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(stringBuffer.toString());
        nexusUpload.setProfileId(requestNode.get("profileId").textValue());
        nexusUpload.setRepositoryId(requestNode.get("repositoryId").textValue());
        nexusUpload.setArtifactId(requestNode.get("artifactId").textValue());
        nexusUpload.setVersion(requestNode.get("version").textValue());
        nexusUpload.setGroupId(requestNode.get("groupId").textValue());
        nexusUpload.setPackaging(requestNode.get("packaging").textValue());
        LogUtils.log(listener, "Artifact details are :" + nexusUpload.toString());
        return nexusUpload;
    }

    public static String getLatestVersion(CloseableHttpResponse response, BuildListener listener, String repoId, ArtifactParameters artifactParameters) throws JsonProcessingException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(stringBuffer.toString());
        Iterator<JsonNode> data = requestNode.get("data").elements();
        String version = null;
        while (data.hasNext()) {
            JsonNode repo = data.next();
            if (repoId != null && repoId.equalsIgnoreCase("snapshots")) {
                version = repo.get("latestSnapshot").textValue();
                break;
            }
            if (repoId != null && repoId.equalsIgnoreCase("cisco_vcs-f_snapshots")) {
                version = repo.get("latestSnapshot").textValue();
                break;
            } else {
                version = repo.get("latestRelease").textValue();
            }
            break;
        }
        if (version == null)
            throw new ArtifactVersionNotFoundException("no version found :" + artifactParameters.getGroupId() +
                    "-" + artifactParameters.getArtifactId() + "-" + artifactParameters.getVersion());
        return version;
    }

    public static int verifyVersionAvailability(CloseableHttpResponse response, BuildListener listener, String repoId, ArtifactParameters artifactParameters) throws JsonProcessingException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(stringBuffer.toString());
        return Integer.parseInt(requestNode.get("totalCount").asText());

    }

    public static String getRepoId(CloseableHttpResponse response, BuildListener listener) throws JsonProcessingException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(stringBuffer.toString());
        Iterator<JsonNode> data = requestNode.get("data").elements();
        String repositoryId = null;
        String version = null;
        while (data.hasNext()) {
            JsonNode repo = data.next();
            version = repo.get("version").textValue();
            if (StringUtils.containsIgnoreCase(version, "snapshot")) {
                repositoryId = repo.get("latestSnapshotRepositoryId").textValue();
            } else {
                repositoryId = repo.get("latestReleaseRepositoryId").textValue();
            }
        }
        LogUtils.log(listener, "Artifact was found in repository with id: " + repositoryId);
        return repositoryId;
    }

    public static String getProfileId(CloseableHttpResponse response, BuildListener listener) throws JsonProcessingException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(stringBuffer.toString());
        Iterator<JsonNode> data = requestNode.get("data").elements();
        String profileId = null;
        while (data.hasNext()) {
            JsonNode repo = data.next();
            if (repo.get("name").asText().equalsIgnoreCase("Release Staging Profile")) {
                profileId = repo.get("id").asText();
                LogUtils.log(listener, "Repository  ProfileId is: " + profileId);

            }
        }
        return profileId;
    }

    public static String getPublicUrl(JsonNode service) throws JsonProcessingException, IOException {

        Iterator<JsonNode> endpoints = service.elements();
        String publicUrl = "";
        while (endpoints.hasNext()) {
            JsonNode endpoint = endpoints.next();
            publicUrl = endpoint.get("publicURL").textValue();
            return publicUrl;
        }

        return publicUrl;
    }

    public static String readResponseBody(CloseableHttpResponse response) throws JsonProcessingException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        return stringBuffer.toString();

    }

    public static String getStackLink(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));

        Iterator<JsonNode> links = requestNode.get("stack").get("links").elements();
        while (links.hasNext()) {
            JsonNode link = links.next();
            return link.get("href").textValue();
        }

        return "";
    }

    public static List<SelectItem> getAllPublicNetworks(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));

        Iterator<JsonNode> jsonNetworks = requestNode.get("networks").elements();
        List<SelectItem> networks = new ArrayList<SelectItem>();

        while (jsonNetworks.hasNext()) {
            JsonNode network = jsonNetworks.next();
            if (network.get("router:external").asBoolean() == true)
                networks.add(new SelectItem(network.get("name").textValue(), network.get("id").textValue()));
        }

        return networks;
    }


    public static List<SelectItem> getAllPrivateNetworks(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));

        Iterator<JsonNode> jsonNetworks = requestNode.get("networks").elements();
        List<SelectItem> networks = new ArrayList<SelectItem>();

        while (jsonNetworks.hasNext()) {
            JsonNode network = jsonNetworks.next();
            if (network.get("router:external").asBoolean() == false)
                networks.add(new SelectItem(network.get("name").textValue(), network.get("id").textValue()));
        }

        return networks;
    }

    public static List<SelectItem> getAllNetwrokSubnetsIds(CloseableHttpResponse response, String networkName) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));

        Iterator<JsonNode> jsonNetworks = requestNode.get("networks").elements();
        List<SelectItem> subnetsIds = new ArrayList<SelectItem>();

        while (jsonNetworks.hasNext()) {
            JsonNode network = jsonNetworks.next();
            if (network.get("name").textValue().equalsIgnoreCase(networkName)) {
                Iterator<JsonNode> subnets = network.get("subnets").elements();
                while (subnets.hasNext()) {
                    JsonNode subnet = subnets.next();
                    subnetsIds.add(new SelectItem(subnet.textValue(), subnet.textValue()));

                }
            }
        }
        return subnetsIds;
    }

    public static List<SelectItem> getAllImages(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));

        Iterator<JsonNode> jsonImages = requestNode.get("images").elements();
        List<SelectItem> images = new ArrayList<SelectItem>();

        while (jsonImages.hasNext()) {
            JsonNode image = jsonImages.next();
            images.add(new SelectItem(image.get("name").textValue(), image.get("id").textValue()));
        }

        return images;
    }

    public static List<SelectItem> getListOfAvailibilityZones(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));

        Iterator<JsonNode> jsonAZs = requestNode.get("aggregates").elements();
        List<SelectItem> azs = new ArrayList<SelectItem>();

        while (jsonAZs.hasNext()) {
            JsonNode az = jsonAZs.next();
            azs.add(new SelectItem(az.get("availability_zone").textValue(), az.get("availability_zone").textValue()));
        }

        return azs;
    }

    public static List<SelectItem> getListOfKeypairs(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));

        Iterator<JsonNode> jsonKeypairs = requestNode.get("keypairs").elements();
        List<SelectItem> keypairs = new ArrayList<SelectItem>();

        while (jsonKeypairs.hasNext()) {
            JsonNode keypair = jsonKeypairs.next();
            keypairs.add(new SelectItem(keypair.get("keypair").get("name").textValue(), keypair.get("keypair").get("name").textValue()));
        }

        return keypairs;
    }


    public static List<String> getResourceURI(CloseableHttpResponse response, BuildListener listener) throws JsonProcessingException, IOException {
        try {

            HttpEntity entity = response.getEntity();
            String xml = EntityUtils.toString(entity);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode requestNode = mapper.readTree(xml);
            Iterator<JsonNode> data = requestNode.get("data").elements();
            List<String> resourceURIs = new ArrayList<String>();
            int i = 0;
            while (data.hasNext()) {
                JsonNode repo = data.next();
                resourceURIs.add(new String(repo.get("resourceURI").textValue()));
                i++;
            }
            Collections.sort(resourceURIs, Collections.reverseOrder());
            return resourceURIs;

        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getResourceURI " + e.getMessage());
        }
    }

    public static String convertMapToJsonString(Map<String, String> jsonMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> rootObject = new HashMap<String, Object>();
        for (Map.Entry<String, String> entry : jsonMap.entrySet()) {
            if (entry.getValue().indexOf(",") != -1) {
                Iterable<String> result = Splitter.on(",").trimResults().split(entry.getValue());
                List<Object> myList = new ArrayList<Object>();
                for (String str : result) {
                    myList.add(str);
                }
                rootObject.put(entry.getKey(), myList);
            } else if (entry.getValue().equalsIgnoreCase("[]")) {
                List<Object> myList = new ArrayList<Object>();
                rootObject.put(entry.getKey(), myList);
            } else {
                rootObject.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            return objectMapper.writeValueAsString(rootObject);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static FloatingIP getFloatingIP(CloseableHttpResponse response) throws JsonProcessingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestNode = mapper.readTree(readResponseBody(response));

        return new FloatingIP(requestNode.get("floating_ip").get("id").textValue(), requestNode.get("floating_ip").get("ip").textValue());
    }

    public static List<String> getFloatingIPIdsFromStackDetails(StackDetails stackDetails) {
        List<String> floatingIPs = new ArrayList<String>();

        for (Map.Entry<String, String> parameter : stackDetails.getParameters().entrySet()) {
            if (parameter.getKey().contains("floating_ip_id") || parameter.getKey().contains("floatingip"))
                floatingIPs.add(parameter.getValue());
        }
        return floatingIPs;

    }

}
