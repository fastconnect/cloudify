[

	win2012_custom_script : computeTemplate {

		imageId "bd507d3a70934695bc2128e3e5a255ba__RightImage-Windows-2012-x64-v13.5"
		machineMemoryMB 3500
		hardwareId "Medium"
		username username
		password password
		remoteDirectory "/C\$/Users/${username}/gs-files"
		localDirectory "upload-windows"
		fileTransfer "CIFS"
		remoteExecution "WINRM"
		scriptLanguage "WINDOWS_BATCH"

		custom ([

			"azure.deployment.slot": "Production",
			"azure.pfx.file": pfxFile,
			"azure.pfx.password" : pfxPassword,
			"azure.cloud.service" : "cfytestitpuppetsymantec",

			"azure.extensions" : [
				// custom script that creates a folder
				[name:"customScript", storageAccount:"nouhstorage", container :"mycontainer", files:"myscript.ps1", arguments:"c:\\nouh"],
			],

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
		])
	}
]
