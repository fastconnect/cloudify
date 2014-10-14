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
    "createStorageAccount": {
        AzureStorageFacade storage = context.getAzureStorage()
        storage.createStorageAccount("storageAccountName")
    },
    "createContainer": {
        context.azureStorage.createContainer("storageAccountName", "containerName")
    },
    "createFileService": {
        context.azureStorage.createFileService("storageAccountName")
    },
    "createDataDisk": {
        context.azureStorage.createDataDisk("containerName", "path_to_vhd")
    },
    "attachDataDisk": {
        context.azureStorage.attachDataDisk("attachDataDisk")
    }
  ])
}

