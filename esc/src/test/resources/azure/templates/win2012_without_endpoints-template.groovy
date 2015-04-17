[

	win2012_without_endpoints : computeTemplate {

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
			"azure.generate.endpoints" : "false",			
	
			"azure.firewall.ports" : [
				[name:"SOMEPORT", protocol:"TCP", port:"8999"],
				[name:"NOTVALIDPORT", protocol:"TCP"],
			]
		])
	}
]
