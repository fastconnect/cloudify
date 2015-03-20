import org.cloudifysource.domain.context.storage.AzureStorageFacade;

service {
  name "storage"

  elastic true
  numInstances 1
  maxAllowedInstances 2

  compute {
      template "ubuntu"
  }

  lifecycle {
      startDetectionTimeoutSecs 900
      startDetection {
        true
      }
      start "start.groovy"
      stopDetection {
        false
      }
      locator {
          NO_PROCESS_LOCATORS
      }
  }

  customCommands([
    "createStorageAccount": { storageAccountName ->
        AzureStorageFacade storage = context.getAzureStorage()
        storage.createStorageAccount(storageAccountName, 60000L*30)
    },
    "createDataDisk": { storageAccountName, ipAddress, size, lun, hostCaching ->
        return context.azureStorage.createDataDisk(storageAccountName, ipAddress, size as Integer, lun as Integer, hostCaching, 60000L*30)
    },
    "attachDataDisk": { dataDiskName, ipAddress, lun ->
        context.azureStorage.attachDataDisk(dataDiskName, ipAddress, lun as Integer, 60000L*30)
    },
    "deleteDataDisk": { dataDiskName ->
        context.azureStorage.deleteDataDisk(dataDiskName, 60000L*30)
    }
  ])
}

