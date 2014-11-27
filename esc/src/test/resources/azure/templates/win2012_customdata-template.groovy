[
	win2012_customdata : computeTemplate {
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
			"azure.deployment.slot": "Staging",
			"azure.pfx.file": pfxFile,
			"azure.pfx.password" : pfxPassword,
			"azure.endpoints" : [
				[name:"CIFS_SMB", protocol:"TCP", port:"445", localPort:"445"],
				[name:"WINRM", protocol:"TCP", port:"5985", localPort:"5985"],
				[name:"WINRM_SSL", protocol:"TCP", port:"5986", localPort:"5986"],
				[name:"MSTSC", protocol:"TCP", port:"3389", localPort:"3389"]
			],
			"azure.deployment.customdata" : "C:\\Windows\\System32\\netsh.exe advfirewall set allprofiles state off"
		])
	}
]
