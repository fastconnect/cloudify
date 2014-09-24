[
  ubuntu1410_ipfixed : computeTemplate {
		imageId "b39f27a8b8c64d52b05eac6a62ebad85__Ubuntu-14_04_1-LTS-amd64-server-20140909-en-us-30GB"
		machineMemoryMB 1600
		hardwareId "Small"
		username username
		password password
		remoteDirectory "/home/${username}/gs-files"
		localDirectory "upload"
		fileTransfer "SCP"
		remoteExecution "SSH"
		scriptLanguage "LINUX_SHELL"
		custom ([
			"azure.availability.set" : "TEST",
			"azure.deployment.slot": "Production",
			"azure.pfx.file": pfxFile,
			"azure.pfx.password" : pfxPassword
		])
		options ([ "ipAddresses" : "10.0.0.12"])
	}
]