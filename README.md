# Jenkins OpenStack Deployment Plugin 

The OpenStack Deployment Plugin enables packaging multi-rpm products, and deploying the products on an Openstack environment
The plugin assumes:
  - Product components are packaged as RPMs
  - Product artifacts are uploaded to Nexus
  - Artifacts which are not in Nexus can be retrieved via http 
  - HEAT templates exist in a git repository
  - Deployment scripts exist in either a git repository or are attached as a tar.gz file to each artifact in Nexus

The HEAT repositoryis expected to have this structure:
```sh
`-- <product name>
    |-- <product name>.env.yaml
    |-- <product name>.yaml
    `-- <product name>.yaml.dependencies
        |-- embedded yaml.yaml
        |-- some bash.sh
        |-- some other bash.sh
        `-- some other bash.sh
```
For example - this is the structure of the demo heat template:
```
`-- demo
    |-- demo.env.yaml
    |-- demo.yaml
    `-- demo.yaml.dependencies
        |-- __stand_alone_external_instance.yaml
        |-- check_installation.sh
        |-- component_installation.sh
        |-- general_user_data.sh
        `-- vci-utility-scripts
```
The heat stack can be found in [Sample HEAT template](https://github.com/naamab/jenkins-openstack-deployment-plugin/tree/master/Sample%20HEAT%20template/demo)

Prior to adding create/deploy product/profile, you should set your Nexus server credentials in the Manage Jenkins / configure System page:
![Jenkins general settings](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/general_settings.jpg)

To package your product:
1. Create a free-style job 

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/create_1.jpg) 

2. Add a 'Create Product' step: 

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/create_2.jpg)

3.  Select group ID, artifact ID, and set git repository paths for your heat templates and deployment scripts
HEAT git repo is mandatory.  Scripts and puppets repos are optional.

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/create_3.jpg)

4. Add dependent products, artifacts and external dependencies.
External dependencies are resources which do not exist in Nexus.
They should be accessible via http.

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/create_5.jpg)

Once you build your product, 3 artifacts are uploaded to Nexus:
1. POM file, describing your dependencies
2. TAR file containing your heat templates, deployment scripts and external dependencies
3. RPM containing your Puppet scripts (built from the Puppet git repo and from the puppet scripts tar uploaded with each component.

Now you can define a 'deploy' job to deploy the product you packaged in an OpenStack environment
1. Create a free-style job

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/deploy_1.jpg)

2. Mark job as 'parametrized' and define a string parameter named 'version'

3.  Add a 'Deploy Product' built step

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/deploy_2.jpg)

4. Set product parameters and OpenStack credentials 

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/deploy_3.jpg)

5. You may define product-specific parameters, and set value to common parameters

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/deploy_5.jpg)

6. You'll have to prepare a machine which will act as a YUM repo.
The machine should have some WebServer installed on it, and point 
/var/www/html to /ci-repo.
Jenkins will copy the artifacts to this yum repo.
To do that, it needs root's private key.

![enter image description here](https://raw.githubusercontent.com/foundation-runtime/jenkins-openstack-deployment-plugin/master/resources/images/deploy_9.jpg)