package org.jenkinsci.plugins.os_ci.openstack;

import org.jenkinsci.plugins.os_ci.model.SelectItem;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.ext.AvailabilityZones.AvailabilityZone;
import org.openstack4j.model.identity.Tenant;
import org.openstack4j.model.identity.User;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.network.Network;
import org.openstack4j.openstack.OSFactory;

import java.util.ArrayList;
import java.util.List;

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
public class OpenStack4jClient {


    private OSClient osClient;

    public OpenStack4jClient(String openstackIP, String openstackUser, String openstackPassword, String openstackTenant ) {
        this.osClient = OSFactory.builder()
                .endpoint("http://" + openstackIP + ":5000/v2.0")
                .credentials(openstackUser, openstackPassword)
                .tenantName(openstackTenant)
                .authenticate();

    }

    public List<SelectItem> getListOfPrivateNetworks() {
        // List the networks which the current tenant has access to
        List<? extends Network> networks = osClient.networking().network().list();

        List<SelectItem> privateNetworkList = new ArrayList<SelectItem>();
        for (Network network : networks) {
            if (network.isRouterExternal() == false)
                privateNetworkList.add(new SelectItem(network.getName(),network.getId()));
        }
        return privateNetworkList;
    }

    public List<SelectItem> getListOfPrivateSubnetsByNetworkname(String privateNetworkName) {
        // List the private subnets which the current tenant has access to

        List<? extends Network> networks = osClient.networking().network().list();

        List<SelectItem> subnetsList = new ArrayList<SelectItem>();

        for (Network network : networks) {
            // if it's a private network
            if (network.isRouterExternal() == false && network.getName().equalsIgnoreCase(privateNetworkName))
                for (String subnet : network.getSubnets())
                    subnetsList.add(new SelectItem(subnet,subnet));
        }
        return subnetsList;
    }

    public List<SelectItem> getListOfPublicNetworks() {
        // List the networks which the current tenant has access to
        List<? extends Network> networks = osClient.networking().network().list();

        List<SelectItem> publicNetworkList = new ArrayList<SelectItem>();
        for (Network network : networks) {
            if (network.isRouterExternal() == true)
                publicNetworkList.add(new SelectItem(network.getName(),network.getId()));
        }
        return publicNetworkList;
    }

    public List<SelectItem> getListOfKeypairs() {
        // Get all Keypairs the current account making the request has access to
        List<? extends Keypair> keypairs = osClient.compute().keypairs().list();

        List<SelectItem> keypairList = new ArrayList<SelectItem>();
        for (Keypair keypair : keypairs) {
            keypairList.add(new SelectItem(keypair.getName(), keypair.getName()));
        }
        return keypairList;
    }

    public List<SelectItem> getListOfImages() {
        // Get all images from Glance the current account making the request has access to
        List<? extends Image> images = osClient.images().list();

        List<SelectItem> imageList = new ArrayList<SelectItem>();
        for (Image image: images) {
            imageList.add (new SelectItem(image.getName(), image.getId()));
        }
        return imageList;
    }


    public List<SelectItem> getListOfTenants() {
        // Get all tenants from Keystone the current account making the request has access to
        List<? extends Tenant> tenants = osClient.identity().tenants().list();

        List<SelectItem> tenantList = new ArrayList<SelectItem>();
        for (Tenant tenant: tenants) {
            tenantList.add(new SelectItem(tenant.getName(),tenant.getId()));
        }
        return tenantList;
    }

    public List<SelectItem> getListOfUsers() {
        // Get all users from Keystone the current account making the request has access to
        List<? extends User> users = osClient.identity().users().list();

        List<SelectItem> usersList = new ArrayList<SelectItem>();
        for (User user: users) {
            usersList.add(new SelectItem(user.getName(),user.getId()));
        }
        return usersList;
    }

    public List<SelectItem> getListOfAvailibilityZones() {
        // Get all tenants from Keystone the current account making the request has access to
        List<? extends AvailabilityZone> zones = osClient.compute().zones().getAvailabilityZones().getAvailabilityZoneList();

        List<SelectItem> zonelist = new ArrayList<SelectItem>();
        for (AvailabilityZone zone: zones) {
            zonelist.add(new SelectItem(zone.getZoneName(), zone.getZoneName()));
        }
        return zonelist;
    }
}