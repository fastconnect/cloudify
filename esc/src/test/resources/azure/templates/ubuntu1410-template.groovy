[
	ubuntu1410 : computeTemplate {
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
		javaUrl "https://s3-eu-west-1.amazonaws.com/cloudify-eu/jdk-6u32-linux-x64.bin"
		custom ([
			"azure.deployment.slot": "Staging",
			"azure.pfx.file": pfxFile,
			"azure.pfx.password" : pfxPassword
		])
  }
]