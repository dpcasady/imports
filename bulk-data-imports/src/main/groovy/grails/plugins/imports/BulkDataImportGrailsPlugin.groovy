package grails.plugins.imports

import grails.plugins.Plugin
import grails.plugins.imports.DefaultImporter as DF
import grails.plugins.imports.ImportsService as IS
import grails.plugins.imports.logging.DefaultLogger
import grails.plugins.imports.logging.InMemoryLogger
import grails.plugins.imports.logging.MongoLogger
import grails.util.GrailsNameUtils as NU
import groovy.util.logging.Slf4j

@Slf4j
class BulkDataImportGrailsPlugin extends Plugin {

    def grailsVersion = "3.0 > *"
    def title = "Bulk Data Imports Plugin"
    def author = "Jeremy Leng"
    def authorEmail = "jleng@bcap.com"
    def description = '''\
Bulk Data Imports Plugin simplifies importing of bulk data via file uploads
Default support for CSV and domain classes
'''

    def loadBefore = ['rabbitmq','rabbit-amqp']
    def documentation = "http://github.com/bertramdev/imports"
    def watchedResources = "file:./grails-app/services/*Service.groovy"
    def organization = [ name: "BertramLabs", url: "http://www.bertramlabs.com/" ]
    def license         = "APACHE"
    def issueManagement = [ system: "GITHUB", url: "http://github.com/bertramdev/imports/issues" ]
    def scm             = [ url: "http://github.com/bertramdev/imports" ]

    Closure doWithSpring() { {->
            def loggingProvider = config.grails.plugins.imports.containsKey('loggingProvider') ? config.grails.plugins.imports.loggingProvider : 'default'
            if (loggingProvider == 'mongo') {
                importsLogger(MongoLogger)
            } else if (loggingProvider == 'mem') {
                importsLogger(InMemoryLogger)
            } else if (loggingProvider == 'default') {
                importsLogger(DefaultLogger)
            } else {
                Class clazz = Class.forName(loggingProvider, true, Thread.currentThread().contextClassLoader)
                importsLogger(clazz)
            }
        }
    }

    void doWithDynamicMethods() {
        for(service in grailsApplication.serviceClasses) {
            if (service.hasProperty('imports')) {
                def entityName,
                    imports = service.getPropertyValue('imports')
                if (imports instanceof Class || imports instanceof String) {
                    entityName = NU.getPropertyName(imports)
                }
                if (entityName) {
                    def found = grailsApplication.domainClasses?.find { NU.getPropertyName(it.name) == entityName} != null
                    if (!found  && !service.hasMetaMethod('processRow', getArgs(5)) ) {
                        log.warn('\n    BulkDataImports: could not configure importer '+service.shortName+'... no domain class found and missing processRow method')
                    } else {
                        DF.SERVICE_METHODS.each {k,v->
                            if (!service.hasMetaMethod(k, getArgs(v))) service.metaClass."${k}" = DF."${k}"
                        }
                        def props = DF.SERVICE_PROPERTIES.clone()
                        props.entityName = entityName
                        props.each {k, v->
                            if (!service.hasMetaMethod(k)) service.metaClass."${k}" = { ->  v }
                        }
                        IS.IMPORT_CONFIGURATIONS[entityName] = NU.getPropertyNameRepresentation(service.shortName)
                    }
                } else {
                    log.warn('\n    BulkDataImports: invalid imports configuration for '+service.shortName+' :'+imports)
                }
            }

        }
        IS.IMPORT_CONFIGURATIONS.each {k,v-> log.info('\n    BulkDataImports:'+ k + ' imported by '+v) }
    }

    void onChange(Map<String, Object> event) {
        if (event.source) doWithSpring()
    }

    private getArgs(ct) {
        (1..ct).collect { Object.class }.toArray()
    }
}
