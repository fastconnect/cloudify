[

	win2012_joindomain : computeTemplate {

		imageId "a699494373c04fc0bc8f2bb1389d6106__Windows-Server-2012-R2-201408.01-en.us-127GB.vhd"
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
			"azure.cloud.service" : "cfytestitdomainjoin",

			// Endpoints definition
			"azure.endpoints" : [
				[name:"CIFS_SMB", protocol:"TCP", localPort : "445", port:"445"],
				[name:"WINRM", protocol:"TCP", localPort : "5985", port:"5985"],
				[name:"WINRM_SSL", protocol:"TCP", localPort : "5986", port:"5986"],
				[name:"HTTP", protocol:"TCP", localPort:"80", port:"80"],
				[name:"RemoteDesktop", protocol:"TCP", localPort:"3389", port:"3389"]
			],

			// Firewall port to open (winrm port 5985 should be opened by default on the image)
			"azure.firewall.ports" : [
				[name:"CLOUDIFY_GUI", protocol:"TCP", port:"8099"],
				[name:"CLOUDIFY_REST", protocol:"TCP", port:"8100"],
				[name:"RemoteDesktop", protocol:"TCP", port:"3389"]
			],
		
			// join a domain
			"azure.domain.join" : 	[domain:"victor", userName:"administrateur", password: "Azerty@01", joinDomain: "victor.local" ]		
		])
	}
]