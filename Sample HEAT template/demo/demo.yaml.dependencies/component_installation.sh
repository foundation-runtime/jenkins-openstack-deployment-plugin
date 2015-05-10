#!/bin/bash -x

if [[ -z "$product_artifact_id" ]]; then
product_artifact_id=$1
fi

if [[ -z "$product_version" ]]; then
product_version=$3
fi

if [[ -z "$component_list" ]]; then
component_list=$3
fi

if [[ -z "$args_list" ]]; then
args_list=$4
fi



echo "gpgcheck = 0" | tee -a /etc/yum.repos.d/infra.repo /etc/yum.repos.d/heat_ci.repo
sudo sed -i "s/mirrorlist=https/mirrorlist=http/" /etc/yum.repos.d/epel.repo

yum clean all

# ---------------------------
# Install puppet & co
# ---------------------------


yum -y install ruby  
yum -y install libselinux-ruby 
yum -y install puppet-2.7.21-1.el6 
yum -y install hiera 
yum -y install hiera-puppet 
yum -y install augeas 
yum -y install vim 
yum -y install wget 
yum -y install expect 
yum -y install sysstat
yum -y install strace
yum -y install dos2unix
yum -y install ntp


yum -y install ld-linux.so.2
# ---------------------------
# Download product RPM
# ---------------------------

if [[ ! -z "$product_version" ]]; then
my_product_version=-$product_version_1
fi

yum -y install nds_$product_artifact_id_deployment-scripts$my_product_version

# ---------------------------
# Configure puppet
# ---------------------------
cd /etc/puppet
sed -i "/\[main\]/a modulepath = \/etc\/puppet\/heatpuppet\/modules:\/etc\/puppet\/heatpuppet\/forge" puppet.conf


mkdir -p /etc/puppet/heatpuppet/modules/role/manifests


IFS=', ' read -a modules_list <<< "$component_list"
includes=''
classes=''

echo ${#modules_list[@]}

for module in "${modules_list[@]}"; do
        includes="${includes}include $module\n"
        if [ ${#modules_list[@]} -gt 1 ]; then
                classes="${classes}Class[$module]->"
        fi
done

classes=`echo $classes | sed -e 's/->$//'`

# ---------------------------
# Create a role/product.pp file
# ---------------------------
cat > /etc/puppet/heatpuppet/modules/role/manifests/$product_artifact_id.pp <<EOFROLEPP
class role::$product_artifact_id {
`echo -e $includes`
`echo -e $classes`
}
EOFROLEPP

# ---------------------------
# Configure Hiera
# ---------------------------
cp /etc/hiera.yaml /etc/hiera.yaml.orig
rm -rf /etc/hiera.yaml
ln -s /etc/puppet/heatpuppet/hieradata/hiera.yaml /etc/hiera.yaml
ln -s /etc/puppet/heatpuppet/hieradata/hiera.yaml /etc/puppet/hiera.yaml
ln -s /etc/puppet/heatpuppet/hieradata /etc/hieradata


# ----------------------------------
# Create a Json file with parameters
# ----------------------------------
mkdir -p /etc/puppet/heatpuppet/hieradata/role

# take arguments list and create a json string
cat > /tmp/convert_to_json.py << ENDPY
import ast

s = '$args_list'
json = {}

if len(s) > 0:
    for arg in s.split('_,_'):
        splitted_arg = arg.split('_=_')
        try:
            json[splitted_arg[0]] =  ast.literal_eval(splitted_arg[1])
        except:
            json[splitted_arg[0]] =  splitted_arg[1]

print str(json).replace("'",'"')

ENDPY

chmod +x /tmp/convert_to_json.py
hiera_params=`python /tmp/convert_to_json.py`

cat > /etc/puppet/heatpuppet/hieradata/role/$product_artifact_id.json << EOFHIERA
$hiera_params
EOFHIERA

# ---------------------------
# Execute puppet module
# ---------------------------
# execute puppet
export FACTER_vmrole=$product_artifact_id
puppet apply -e "include role::$product_artifact_id" --verbose
	