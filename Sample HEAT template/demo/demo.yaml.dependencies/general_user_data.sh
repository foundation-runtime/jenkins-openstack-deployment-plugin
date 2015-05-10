#!/bin/bash -x

# -------- edit /etc/hosts file
   
hostname=`hostname`
echo "0.0.0.0   localhost localhost.localdomain localhost4 localhost4.localdomain4 ${hostname}"> /etc/hosts

# internal dns registration
$vci_utility_scripts
vciUtil startAll

$component_installation

$check_installation