[
	ubuntu1410_acl_endpoints : computeTemplate {
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
			"azure.deployment.slot": "Production",
			"azure.pfx.file": pfxFile,
			"azure.pfx.password" : pfxPassword,
			"azure.cloud.service" : "cfytestitendpointsacl",
			"azure.endpoints" : [
				
				[
					"basic" : [name:"HTTP", protocol:"TCP", localPort: "80", port:"8080"],
					"lb"    : [loadBalancedSet: "lbSetTest", probePort : "80", probeProtocol:"TCP"],
					"acl"   :
					 [				
						["order" : 1, "action":"permit", "subnet":"10.0.0.0/24", "description": "rule for something" ],
						["order" : 2, "action":"deny", "subnet":"10.0.1.0/24", "description": "rule for something else" ]
					 ]		
				],
			]
		])
	}
]