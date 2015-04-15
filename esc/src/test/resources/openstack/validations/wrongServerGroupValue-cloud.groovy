cloud {
	name = "hp-grizzly"

	configuration {  managementMachineTemplate "MANAGER" }

	provider {
		provider "openstack-nova"
		managementGroup "cloudify-manager-"
		machineNamePrefix "cloudify-agent-"
		numberOfManagementMachines 1
	}
	user {
		user "tenant:user"
		apiKey "apiKey"
	}
	cloudNetwork {
		management {
			networkConfiguration {
				name "Cloudify-Management-Network"
				subnets ([
					subnet {
						name "Cloudify-Management-Subnet"
						range "24.0.0.0/24"
					}
				])
				custom ([ "associateFloatingIpOnBootstrap" : "true" ])
			}
		}
		templates ([
			"APPLICATION_NET" : networkConfiguration {
				name "Cloudify-Application-Network"
				subnets {
					subnet {
						name "Cloudify-Application-Subnet"
						range "10.0.0.0/24"
					}
				}
			}
		])
	}

	cloudCompute {
		templates ([
			MANAGER : computeTemplate{
				imageId "region/imageId"
				remoteDirectory "remoteDirectory"
				machineMemoryMB 1600
				hardwareId "region/hardwareId"
				localDirectory "upload"
				overrides ([
					"openstack.endpoint": "openstackUrl"
				])
			},
			APPLI : computeTemplate{
				imageId "region/imageId"
				remoteDirectory "remoteDirectory"
				machineMemoryMB 1600
				hardwareId "region/hardwareId"
				localDirectory "upload"
				overrides ([
					"openstack.endpoint": "openstackUrl"
				])
        options ([
          "serverGroups":"ko"
        ])
			}
		])
	}
}