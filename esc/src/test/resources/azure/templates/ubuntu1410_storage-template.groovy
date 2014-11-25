[
	ubuntu1410_storage : computeTemplate {
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
			"azure.cloud.service" : "cfytestitstorageos",
			"azure.deployment.slot": "Production",
			"azure.pfx.file": pfxFile,
			"azure.pfx.password" : pfxPassword,
			"azure.storage.account" : [	"cfytestitos1",	"cfytestitos2"] as List,
			"azure.storage.accounts.data" : ["cfytestitdata1", "cfytestitdata2"] as List,
			"azure.storage.datadisk.size" : 10,
		])
	}
]