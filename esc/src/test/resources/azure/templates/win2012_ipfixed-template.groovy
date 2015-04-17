[

	win2012_ipfixed : computeTemplate {

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

			"azure.deployment.slot": "Staging",
			"azure.pfx.file": pfxFile,
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
					 "basic":[name:"RemoteDesktop", protocol:"TCP", localPort:"3389", port:"3389"],	
				]
			],
			"azure.network.ipAddresses" : "10.0.0.12"
		])
	}
]
