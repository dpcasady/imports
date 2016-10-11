**Bulk Data Imports Grails Plugin**
=====================

This plugin provides support for importing bulk data, typically via CSV file upload.

**Included features:**

 - Declarative "imports" support for domain classes or other structures
 - Controller for processing file uploads
 - Optional pre-processing validation
 - Error row tracking
 - Built-in support for CSV processing
 - Support for asynchronous processing (via Executor plugin)
 - Optional support for distributed processing (via optional RabbitMQ plugin)
 - Optional support for confirmation and summary email notification  (via optional Mail Plugin)
 - All aspects of import processing can be overridden
 - Pluggable import logging framework (includes in-memory and Mongo logging implementations)
 - Support for canceling asynchronous and queue-based import processing
 - Support in customized import instructions
 - Support for archiving imported file

Data import functionality is exposed using Grails services.

Import file upload POST URL is:

    /imports/process

This endpoint expect multi-part mime encoded and accepts 4 parameters:

- entityName [string]
- import [file] \(parameter can be customized in config\)
- format (of response) [html | json | xml]
- email [string] (email address to send confirmation and summary emails [only if mail plugin is configured])

GET to same URL will return configurable "info" response.

Refer to grails.plugins.imports.ImportsService to see how things work.

Plugin project has sample imports in test/imports and entry point is:

    http://localhost:8080/importsTest

Basic Example
-------------

Domain class:

```groovy
class MyDomainClass {
    String emailValue
    Long longValue
    Double doubleValue
    Integer integerValue
    Integer integerValueMarshalled
    String stringValue
    String stringValueNullable
    String listValue
    String aliasValue
    Date dateCreated
    Date lastUpdated
    static constraints = {
        stringValueNullable(nullable:true)
        integerValueMarshalled(nullable:true)
        emailValue(email:true)
        listValue(inList:['listValue1','listValue2', 'listValue3'])
    }
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass
}
```

Import CSV file:

    emailValue,longValue,doubleValue,stringValue,listValue,integerValueMarshalled,integerValue,aliasValue
    test@test.com,11,11.11,poop,listValue1,0,1,aliasValue
    test@test.com,22,22.22,poop2,listValue2,0,2,aliasValue
    test@test.com,33,33.33,poop3,listValue3,0,,aliasValue

Property Alias Example
----------------------

Domain class:

```groovy
class MyDomainClass {
    String stringValue
    Date dateCreated
    Date lastUpdated
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass

    def columns(params, importLogId) {
        ['FriendlyColumnName':'stringValue']
    }
}
```

Import CSV file:

    FriendlyColumnName
    value1
    value2
    value3

Asynchronous Example
----------------------

Domain class:

```groovy
class MyDomainClass {
    String stringValue
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass
    def async = true
}
```

Import CSV file:

    stringValue
    value1
    value2
    value3

Queue Example
----------------------

Domain class:

```groovy
class MyDomainClass {
    String stringValue
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass
    def useQueue = true
}
```

Import CSV file:

    stringValue
    value1
    value2
    value3
Non-Domain Class Example
----------------------

Import service:

```groovy
class MyDomainClassImportService {
    static imports = 'ArbitraryName'
    def importsLogger
    def processRow(row, index, columns, params, importLogId) {
        def success = false
        // do something interesting
        if (!success) {
            importsLogger.logErrorRow(importLogId, row, index, 'something bad happened')
        } else {
            importsLogger.logSuccessRow(importLogId, row, index)
        }
        if (isImportComplete(importLogId)) // should do this if using queue
            importsLogger.processComplete(params, importLogId)
    }
}
```

Import CSV file:

    anyColumnValue
    value1
    value2
    value3

Custom Update Example
----------------------

Domain class:

```groovy
class MyDomainClass {
    String stringValue
    String code
    Date dateCreated
    Date lastUpdated
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass
    def matchProperties = ['code']
}
```

Import CSV file:

    stringValue,code
    value1,ABC123
    value2,XYZ789
    value3,LMN456

Association Example
----------------------

Domain classes:

```groovy
class MyDomainClass {
    String stringValue
    MyAssociatedDomainClass domainClassValue
    Date dateCreated
    Date lastUpdated
}

class MyAssociatedDomainClass {
    String name
    Date dateCreated
    Date lastUpdated
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass
}
```
Import CSV file:

    stringValue,domainClassValue.id
    value1,1
    value2,1
    value3,2

Custom Association Example
----------------------
Domain classes:

```groovy
class MyDomainClass {
    String stringValue
    MyAssociatedDomainClass domainClassValue
    Date dateCreated
    Date lastUpdated
}

class MyAssociatedDomainClass {
    String name
    Date dateCreated
    Date lastUpdated
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass
    def afterBindRow(obj, row, index, columns, params, importLogId) {
        if (row['associatedItemName']) {
        obj.testImportItem5= MyAssociatedDomainClass.findByName(row['associatedItemName'])
        }
    }
}
```

Import CSV file:

```
stringValue,associatedItemName
value1,joe
value2,fred
value3,carl
```

Custom Column Marshalling Example
----------------------

Domain class:

```groovy
class MyDomainClass {
    String stringValue
    Integer customMarshallField
    Date dateCreated
    Date lastUpdated
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass

    def marshall(columnName,  propertyName, value, importLogId) {
        def rtn = value
        if (propertyName == 'customMarshallField') {
            rtn = new Integer((rtn ?: '0').toString()) + 10
            log.info('marshall:'+columnName+','+value+'->'+rtn)
        }
        return rtn
    }

}
```
Import CSV file:
```
stringValue,customMarshallField
value1,1
value2,1
value3,2
```

Custom Column Default Example
----------------------
You can assign a default value to fields (if the Domain Class does not define a default)

Domain class:

```groovy
class MyDomainClass {
    String stringValue
    String customDefaultField
    Date dateCreated
    Date lastUpdated
}
```

Import service:

```groovy
class MyDomainClassImportService {
    static imports = MyDomainClass

    def defaultValue (columnName, propertyName, importLogId) {
        def rtn = null
        if (propertyName == 'customDefaultField') {
            rtn = new Date().format('HH:mm:ss')
            log.info('defaultValue:'+columnName+'->'+rtn)
        }
        return rtn
    }
}
```

Import CSV file:

```
stringValue,customDefaultField
value1,
value2,
value3,
```

Import service properties [default value]
--------------------------

Add these service properties to modify processing behavior (i.e. "def async = false")

 - infoUrl [null]
 - async [true]
 - parameterName ['import']
 - matchProperties [ ['id'] ]
 - useQueue [false]
 - maxErrors [Integer.MAX_VALUE]
 - doValidation [false]
 - cancelCheckIncrement [50]
 - doConfirmationEmail [false]
 - doSummaryEmail [false]
 - doArchiveFile [false]
 - fromEmailAddress ['imports@myapp.com']
 - doIncludeErrorsInSummary [true]
 - confirmationEmailContentTemplate [ImportsService.DEFAULT_CONFIRMATION_EMAIL_CONTENT]
 - summaryEmailContentTemplate [ImportsService.DEFAULT_SUMMARY_EMAIL_CONTENT]
 - confirmationEmailSubjectTemplate [ImportsService.DEFAULT_CONFIRMATION_EMAIL_SUBJECT]
 - summaryEmailSubjectTemplate [ImportsService.DEFAULT_SUMMARY_EMAIL_SUBJECT]

Mail Information
----------------
If the custom import service includes the properties `def doConfirmationEmail = true` and/or `def doSummaryEmail = true`, emails will be sent out. By default, the recipient,s email address will be to to the 'email' parameter submitted with the import file. You may override `def summaryEmailAddress(params, importLogId)` and/or `def confirmationEmailAddress(params, importLogId)` to lookup the recipient email using another means.

***NOTE:***  To send emails you must install and configure the mail plugin into you Grails application.

**Confirmation Email Template**

> Hi,
>
> Your recent import of [${]entityName] has been received.
>
> Filename : [fileName]
>
> Uploaded at : [created]
>
> Total rows provided: [total]
>
> If you experience issues related to your import process, please
> contact support.
>
> Thank you very much!


**Summary Email Template**

> Hi,
>
> Your recent import of [entityName] has been completed. Please find the
> result statistics below.
>
> Filename : [fileName]
>
> Uploaded at : [created]
>
> Total rows provided: [total]
>
> Total success : [successCount]
>
> Total errors : [errorCount]
>
> Attempted inserts : [insertCount]
>
> Attempted updates : [updateCount]
>
> If you experience issues related to your import process, please
> contact support..
>
> Thank you very much!

RabbitMQ Information
--------------------
If RabbitMQ is installed the queue used for processing imports will be called `${grailsApplication.metadata['app.name']}ImportRows` (the app name as defined in `application.properties`.

***NOTE:*** To use queues you must have a running instance of RabbitMQ and install and configure the RabbitMQ plugin into your Grails application.

Import service processing methods that can be overridden
--------------------------
```groovy
// File processing methods
def validateFile(uploadedFile, params, importLogId)
def getRowCount(uploadedFile, params, importLogId)
def processCsv(uploadedFile, params, importLogId)
def processXXX(uploadedFile, params, importLogId) //support XXX mime type
def columns(params, importLogId)
def column(params, name, importLogId)
def archiveFile(uploadedFile, params, importLogId)
def retrieveArchivedFile(params, importLogId)
def processRow(row, index, columns, params, importLogId) // override entire process
def beforeBindRow(row, index, columns, params, importLogId)
def fetchObject(row, index, columns, params, importLogId) // called during bind row
def bindRow(row, index, columns, params, importLogId)
def defaultValue(columnName, propertyName, importLogId)
def marshall(columnName,  propertyName, value, importLogId)
def validateHeaders(headers, params, importLogId)
def afterBindRow(obj, row, index, columns, params, importLogId)
def validateRow(obj, row, index, columns, params, importLogId)
def saveRow(obj, row, index, columns, params, importLogId)
def formatErrors(obj, importLogId)
def afterSaveRow(success, obj, row, index, columns, params, importLogId)
def processComplete(params, importLogId)

//Email methods
def summaryEmailContentTemplate(params, importLogId)
def summaryEmailSubjectTemplate(params, importLogId)
def summaryEmailBindVariables(binding, params, importLogId)
def summaryEmailContent(params, importLogId)
def summaryEmailSubject(params, importLogId)
def summaryEmailAddress(params, importLogId)
def sendSummaryEmail(params, importLogId) // override entire summary email
def confirmationEmailContentTemplate(params, importLogId)
def confirmationEmailSubjectTemplate(params, importLogId)
def confirmationEmailBindVariables(binding, params, importLogId)
def confirmationEmailContent(params, importLogId)
def confirmationEmailSubject(params, importLogId)
def confirmationEmailAddress(params, importLogId)
def sendConfirmationEmail(params, importLogId)
```

Import logging
--------------------------

The logger implementation is configurable and pluggable:

```groovy
grails.plugins.imports.loggingProvider = 'mem' \\'mongo','default', 'file', [custom class name]
```

The logger can me autowired into Grails artifacts:

```groovy
def importsLogger
```


```groovy
//Logging methods
def createImportLog(params)
def incrementImportCounter(importLogId)
def setImportTotal(importLogId, total)
def setImportLogValue(importLogId, name, value)
def cancel(importLogId) // canceling occurs via logging
def isCanceled(importLogId)
def isImportComplete(importLogId)
def logMessage(importLogId, valuesMap)
def logSuccessRow(importLogId, row, index)
def logCancelRow(importLogId, row, index)
def logInsertRow(importLogId, row, index)
def logUpdateRow(importLogId, row, index)
def logErrorRow(importLogId, row, index, msg)
def getImportLog(importLogId)
def getImportLogErrorInfo(importLogId)
def findImportLogs(params)
```

**NOTE:** The import log is used to maintain state during import processing. The "cancel import" functionality depends on the import log.

Sample Log structure in JSON:
```json
{
   "headers":[
      "emailValue",
      "longValue",
      "doubleValue",
      "stringValue",
      "listValue",
      "integerValueMarshalled",
      "integerValue",
      "friendlyName"
   ],
   "total":2,
   "canceled":false,
   "scope":"imports",
   "accountId":null,
   "cancelCount":0,
   "insertCount":0,
   "errorCount":0,
   "errorRows":[

   ],
   "messages":[
      {
         "text":"Starting file processing",
         "ts":"2014-03-20T16:50:29Z"
      },
      {
         "text":"Process request complete (async processing may still be occurring)",
         "ts":"2014-03-20T16:50:30Z"
      }
   ],
   "entityName":"testImportItem1",
   "confirmationEmail":{
      "content":" Hi, Your recent import of testImportItem1 has been received. Filename : testImportItem1.csv Uploaded at : Thu Mar 20 09:50:29 PDT 2014 Total rows provided: 2 If you experience issues related to your import process, please contact support. Thank you very much! ",
      "subject":"Your import of testImportItem1 has been received",
      "from":"imports@myapp.com",
      "recipient":"sefiaconsulting@gmail.com"
   },
   "processed":0,
   "importType":"csv",
   "_id":"2c00b45261134adebdf0e16859aa2bff",
   "created":"2014-03-20T16:50:29Z",
   "userId":null,
   "fileName":"testImportItem1.csv",
   "archivedFileLocator":null,
   "processing":true,
   "updateCount":0,
   "successCount":0,
   "fileSizeBytes":215
}
```

Custom logger classes should implement the `com.grails.plugins.imports.logging.ImportLogger` interface that defines the methods above.

Import actions
--------------------------

There are some default URLs that can be used to access import functionality. The views rendered are configurable.

```
// view config: grails.plugins.imports.logView
/imports/showLog

// customized user instructions for a specific import
// view config: infoUrl property in each import service
/imports/showInfo


// Cancel asynchronous import process. Accepts the parameter importLogId or id
// view config: grails.plugins.imports.cancelView
/imports/cancel

// Upload file to be processed. Must be a multi-part file POST. File parameter is configurable in import service and defaults to 'import'
// view config: grails.plugins.imports.confirmationView
/imports/process

//
// view config: grails.plugins.imports.errorRowView
/imports/showErrorRows

```
