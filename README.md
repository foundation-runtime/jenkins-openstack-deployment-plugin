# Jenkins OpenStack Deployment Plugin 

The OpenStack Deployment Plugin enables packaging multi-rpm products, and deploying the products on an Openstack environment
The plugin assumes:
  - Product components are packaged as RPMs
  - Product artifacts are uploaded to Nexus
  - Artifacts which are not in Nexus can be retrieved via http 
  - HEAT templates exist in a git repository
  - Deployment scripts exist in either a git repository or are attached as a tar.gz file to each artifact in Nexus

The HEAT repositories expected to have this structure:
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
