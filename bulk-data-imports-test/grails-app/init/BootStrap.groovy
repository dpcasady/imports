import grails.config.Config
import grails.core.GrailsApplication

class BootStrap {

    GrailsApplication grailsApplication

    def init = { servletContext ->
        Config config = grailsApplication.config

        log.info """Configured DataSource settings:
        |   url: ${config.dataSource.url}
        |   username: ${config.dataSource.username}
        |   password: ${config.dataSource.password}
        |   dbCreate: ${config.dataSource.dbCreate}
        |   logSql: ${config.dataSource.logSql}""".stripMargin()
    }

    def destroy = {
    }
}
