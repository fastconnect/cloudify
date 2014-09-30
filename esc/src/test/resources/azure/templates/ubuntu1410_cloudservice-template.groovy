[
	ubuntu1410_cloudservice : computeTemplate {
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
      "azure.cloud.service" : "cfytestitcloudservice",
  	  "azure.endpoints" : [
  		  [name:"HTTP", protocol:"TCP", localPort: "80", port:"81"],
  		  [name:"SSH", protocol:"TCP", localPort: "22", port:"5000"]
  	  ],
    ])
  }
]