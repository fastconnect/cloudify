
cloud {

	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "Azure"
	configuration {

		// Mandatory - Azure IaaS cloud driver.
		className "org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureCloudDriver"
		storageClassName "org.cloudifysource.esc.driver.provisioning.storage.azure.MicrosoftAzureStorageDriver"
		azureStorageClassName "org.cloudifysource.esc.driver.provisioning.storage.azure.AzureStorageProvisioningDriverImpl"

		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "MEDIUM_WIN2012"

		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true

		// Optional. Path to folder where management state will be written. Null indicates state will not be written.
		// persistentStoragePath persistencePath
	}

	provider {

		// Optional
		provider "azure"

		/*************************************************************************************************************************
		 * Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the *
		 * cloudify version matching that of the client from the cloudify CDN.													 *
		 * Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a			 *
		 * different HTTP server instead.																						 *
		 ************************************************ *************************************************************************/

		// IMPORTANT: the default linux bootstrap script appends '.tar.gz' to the url whereas the default windows script appends '.zip'.
		// Therefore, if setting a custom URL, make sure to leave out the suffix.
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.7.1-6300-RELEASE/gigaspaces-cloudify-2.7.1-ga-b6300.zip"

		managementGroup "${codeCountry}${codeEnvironment}"
		machineNamePrefix "${codeCountry}${codeEnvironment}"

		managementOnlyFiles ([])

		numberOfManagementMachines 1

		reservedMemoryCapacityPerMachineInMB 1024

		sshLoggingLevel "WARNING"
	}

	user { // Azure subscription id
		user subscriptionId }


	/********************
	 * Cloud storage configuration.
	 */
	cloudStorage {
		templates ([
			SMALL_BLOCK : storageTemplate{
				namePrefix "cloudify-storage-volume"
				size 5
				deviceName "2" // LUN
				deleteOnExit false
				custom ([
					"azure.storage.account" : [
						"storagesmallblock1",
						"storagesmallblock2"] as List,
					"azure.storage.hostcaching" : "None" // or ReadOnly or ReadWrite
				])
				partitioningRequired true // If true, the driver will partition the disk (relevant with static storage)
				path "/storage"           // Used to mount (relevant with static storage)
				fileSystemType "ext4"     // Used to format (relevant with static storage)
			}
		])
	}

	cloudNetwork {
		custom ([
			/*******************************************************************************************
			 * A Virtaul Network Site name.                                                            *
			 * All VM's will belong to this network site.                                              *
			 * If the specified network site does not exist, it will be created automatically for you. *
			 * in this case, you must specify the 'azure.address.space' property                       *
			 *******************************************************************************************/
			"azure.networksite.name" : netWorksite,

			/***************************************************************************************
			 * CIDR notation specifying the Address Space for your Virtaul Network.                *
			 * All VM's will be assigned a private ip from this address space.                     *
			 ***************************************************************************************/
			"azure.address.space"    : netAddress,

			/***************************************************************************************
			 * Set DNS servers.                                                                    *
			 ***************************************************************************************/
			//"azure.dns.servers" : "dnsName1:ipAdress1,dnsName2:ipAdress2"

			/***************************************************************************************
			 * VPN CONFIGURATION                                                                   *
			 ***************************************************************************************/

			/*
			 "azure.vpn.localsite.name" : vpnLocalSiteName,
			 "azure.vpn.gateway.address" : vpnGatewayAddress,
			 "azure.vpn.gateway.type" : vpnGatewayType, // StaticRouting or DynamicRouting
			 "azure.vpn.gateway.key" : vpnGatwayKey,
			 "azure.vpn.address.space" : vpnAddressSpace,
			 "azure.vpn.subnet.address.prefix" : vpnSubnetAddressPrefix,
			 */

		])

		management {
			networkConfiguration {
				subnets ([
					subnet {
						// The name of the subnet.
						name  managementSubnetName
						// CIDR notation specifying the address range for the subnet.
						range adminNetAddress
					}
				])
			}
		}
		templates ([
			"ADM_NET" : networkConfiguration {
				name null // this property is unused with azure
				subnets {
					subnet {
						name "admin_subnet"
						range "10.0.1.0/24"
					}
				}
			},
			"DATA_NET" :  networkConfiguration {
				name null // this property is unused with azure
				subnets {
					subnet {
						name "data_subnet"
						range "10.0.2.0/23"
					}
				}
			},
			"DATA2_NET" :  networkConfiguration {
				name null // this property is unused with azure
				subnets {
					subnet {
						name "data_subnet2"
						range "10.1.0.0/16"
					}
				}
			}
		])
	}

	cloudCompute {

		templates ([

			UBUNTU1410 : computeTemplate{

				imageId "b39f27a8b8c64d52b05eac6a62ebad85__Ubuntu-14_04_1-LTS-amd64-server-20140909-en-us-30GB"
				machineMemoryMB 3500
				hardwareId "Medium"

				username username
				password password

				remoteDirectory "/home/${username}/gs-files"

				localDirectory "upload"

				fileTransfer "SCP"
				remoteExecution "SSH"
				scriptLanguage "LINUX_SHELL"

				custom ([

					// Choose whether do deploy this instance in Staging or Production environment. defaults to Staging
					"azure.deployment.slot": "Production",

					/**************************************************************
					 * Mandatory only for templates used for management machines. *
					 * Put this file under the path specified in 'localDirectory' *
					 ***************************************************************/
					"azure.pfx.file": pfxFile,

					// Password that was used to create the certificate
					"azure.pfx.password" : pfxPassword,

					// Endpoints definition
					"azure.endpoints" : [
						[
							"basic" : [name:"SSH", protocol:"TCP", localPort: "22", port:"22"]
						]

					]

					// Optional. each availability set represents a different fault domain.
					// "azure.availability.set" : "ENTER_AVAILABILITY_SET",

					// Optional. If the property "azure.availability.set" is configured for this compute template.
					// The driver will create availability sets with a maximum of "azure.availability.set.max.members" members in it.
					// When the maximum members is reached in the availability set, the driver will create a new one.
					// "azure.availability.set.max.members" : 2,

					// Optional. Cloud service. If not exist, it will create one.
					//"azure.cloud.service" : "CLOUD_SERVICE_NAME",

					// Optional. Specify a storage account. If not exist, it will create one.
					//"azure.storage.account" : ["STORAGE_ACCOUNT_NAME","STORAGE_ACCOUNT_NAME2"],

					// Optional. If set, the driver will create and attack a data disk with the defined size to the new VM
					//"azure.storage.datadisk.size" : 15, // Will create and attach a data disk of 15Gb

					// Optional. Define a fixed IP address
					// You can define a list of IPs. In this case, the machine will take the first free IP of the list
					// "azure.network.ipAddresses" : "10.0.0.12,10.0.0.13",

					// Optional. Custom Data
					// "azure.deployment.customdata" : "#!/bin/sh\r\necho 'hello world' > /home/administrateur/hello.txt",
				])
			},

			MEDIUM_WIN2012 : computeTemplate {

				imageId "bd507d3a70934695bc2128e3e5a255ba__RightImage-Windows-2012-x64-v13.5"

				machineMemoryMB 3500
				hardwareId "Medium"

				username username
				password password

				remoteDirectory "/C\$/Users/${username}/gs-files"

				localDirectory "upload-windows"

				// File transfer mode. Optional, defaults to SCP.
				fileTransfer "CIFS"
				// Remote execution mode. Options, defaults to SSH.
				remoteExecution "WINRM"
				// Script language for remote execution. Defaults to Linux Shell.
				scriptLanguage "WINDOWS_BATCH"

				custom ([
					// Choose whether do deploy this instance in Staging or Production environment. defaults to Staging
					"azure.deployment.slot": "Production",

					/**************************************************************
					 * Mandatory only for templates used for management machines. *
					 * Put this file under the path specified in 'localDirectory' *
					 ***************************************************************/
					"azure.pfx.file": pfxFile,

					// Password that was used to create the certificate
					"azure.pfx.password" : pfxPassword,

					// Endpoints definition
					"azure.endpoints" : [
						[
							"basic": [name:"CIFS_SMB", protocol:"TCP", localPort : "445", port:"445"]
						],
						[
							"basic": [name:"WINRM", protocol:"TCP", localPort : "5985", port:"5985"],
						],
						[
							"basic": [name:"WINRM_SSL", protocol:"TCP", localPort : "5986", port:"5986"],
						],
						[
							"basic": [name:"HTTP", protocol:"TCP", localPort:"80", port:"80"],
						],
						[
							"basic":[name:"REMOTE_DESKTOP", protocol:"TCP", localPort:"3389", port:"3389"],
						]
					],

					// Optional. Firewall port to open
					"azure.firewall.ports" : [
						[name:"EVENTS", protocol:"TCP", port:"8081"]
					]


					// Optional. each availability set represents a different fault domain.
					// "azure.availability.set" : "ENTER_AVAILABILITY_SET",

					// Optional. Cloud service. If not exist, it will create one.
					//"azure.cloud.service" : "CLOUD_SERVICE_NAME",

					// Optional. Specify a storage account. If not exist, it will create one.
					//"azure.storage.account" : ["STORAGE_ACCOUNT_NAME","STORAGE_ACCOUNT_NAME2"],

					// Optional. If set, the driver will create and attack a data disk with the defined size to the new VM
					//"azure.storage.datadisk.size" : 15, // Will create and attach a data disk of 15Gb

					// Optional. Define a fixed IP address
					// You can define a list of IPs. In this case, the machine will take the first free IP of the list
					// "azure.network.ipAddresses" : "10.0.0.12,10.0.0.13",

					// Optional. Join a domain
					/*
					 "azure.domain.join" : [
					 domain:"DOMAIN_NAME",  // ex: victor
					 userName:"DOMAIN_USER",  // ex: administrateur
					 password: "DOMAIN_USER_PASSWORD",  // ex: Azerty@01
					 joinDomain: "DOMAIN", // ex: victor.local
					 machineObjectOU: "MEMBER_OU"
					 ]
					 */

					// Optional. Specify extensions to install ('puppet', 'symantec')
					/*
					 "azure.extensions" : [
					 // puppet
					 [name:"puppet", masterServer:"puppetMasterIpAddress"],
					 // custom script that creates a folder
					 [name:"customScript", storageAccount:"storageAccountName", container :"mycontainer", files:"myscript.ps1", arguments:"arg1 arg2"],
					 // endpoints seem blocked after installing symantec extension
					 [name:"symantec"]
					 ]
					 */
				])
			}
		])
	}

	custom ([

		/****************************************************************************************
		 * An Affinity Group name.																*
		 * if the specified group does not exist, one will be created automatically for you.	*
		 * in this case, you must specify the 'azure.affinity.location' property				*
		 *****************************************************************************************/
		"azure.affinity.group" : affinityGroup,

		/********************************************************************************************************************************
		 * The MS Data Center location. 																								*
		 * All VM's will be launched onto this Data Center. see http://matthew.sorvaag.net/2011/06/windows-azure-data-centre-locations/	*
		 * Mandatory only if the affinity group specifed above is not a pre-existing one.												*
		 *********************************************************************************************************************************/
		"azure.affinity.location" : affinityLocation,

		/************************************************************************************************************
		 * A (defaut) Storage Account prefix name.																    *
		 * All OS/Data Disk Images will be stored in this account if thery weren't defined in the compute template. *
		 * If the specified account does not exist, it will be created automatically for you.	 				    *
		 ************************************************************************************************************/

		"azure.storage.account.prefix" : storageAccountPrefix,

		/*****************************************************************************************
		 * A Storage Account name.																 *
		 * This storage account will be used for the file share service							 *
		 * If the specified account does not exist, it will be created automatically        	 *
		 ******************************************************************************************/

		"azure.storage.account.file.service" : fileServiceStorageAccount,

		/******************************************************************************************
		 * A global availability set															  *
		 * if a compute template doesn't set an availability set, this one will be used     	  *
		 ******************************************************************************************/

		"azure.availability.set" : availabilitySet,

		/*************************************************************************************************************************
		 * Specify whether or not to delete the network (if found) when you execute a teardown command.							 *
		 * If set to 'true', the storage account, affinity group, and network specified above will be deleted upon teardown.	 *
		 * NOTE : if you are using pre exsisting services and you dont want them to be deleted, please set this value to 'false' *
		 **************************************************************************************************************************/
		"azure.cleanup.on.teardown" : "true",

		/********************************************************************
		 * Cloud service code
		 * The code will be append to the machineNamePrefix/managementGroup.
		 * The cloud service name will resulting to be something like :
		 *   ${machineNamePrefix}${cloudServiceCode}XXX
		 * where XXX is an incremental index
		 *********************************************************************/
		"azure.cloud.service.code" : cloudServiceCode,

		// Enable/Disable Cloud Requests Logging.
		"azure.wireLog": wireLog,
	])
}

