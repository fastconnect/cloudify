[

	win2012 : computeTemplate {

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

			// Endpoints definition
			"azure.endpoints" : [
				[name:"CIFS_SMB", protocol:"TCP", localPort:"445", port:"445" ],
				[name:"WINRM", protocol:"TCP", localPort : "5985", port:"5985"],
				[name:"WINRM_SSL", protocol:"TCP", localPort :"5986", port:"5986"],
				[name:"HTTP", protocol:"TCP", localPort: "80", port:"80"]
			],

			"azure.firewall.ports" : [
				[name:"SOMEPORT", protocol:"TCP", port:"8999"],
				[name:"NOTVALIDPORT", protocol:"TCP"],
			]
		])
	}
]
