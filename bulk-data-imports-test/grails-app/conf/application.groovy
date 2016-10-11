dataSource {
    pooled = true
    url = System.getProperty('db.url', "jdbc:mysql://localhost/imports")
    driverClassName = System.getProperty('db.driverClassName', "com.mysql.jdbc.Driver")
    username = System.getProperty('db.username', "root")
    password = System.getProperty('db.password', "")
    dbCreate = "update"
    logSql = System.getProperty('db.logSql') == 'true'
    dialect = org.hibernate.dialect.MySQL5InnoDBDialect
    properties {
        jmxEnabled = true
        initialSize = 5
        maxActive = 50
        minIdle = 5
        maxIdle = 25
        maxWait = 10000
        maxAge = 600000
        timeBetweenEvictionRunsMillis = 5000
        minEvictableIdleTimeMillis = 60000
        validationQuery = 'SELECT 1'
        validationQueryTimeout = 3
        validationInterval = 15000
        testOnBorrow = true
        testWhileIdle = true
        testOnReturn = false
        jdbcInterceptors = ConnectionState
        defaultTransactionIsolation = 2 // TRANSACTION_READ_COMMITTED
    }
}

grails {
    mongo {
        host = '127.0.0.1'
        port = 27017
        databaseName = "Import"
        options {
            autoConnectRetry = true
            connectTimeout = 300
        }
    }
}

grails.plugins.imports.loggingProvider = 'mem'

rabbitmq {
    connectionfactory {
        username = 'guest'
        password = 'guest'
        hostname = 'localhost'
        consumers = 5
    }
    queues = {
        exchange name: 'imports', type: direct, durable: true, {
            "${grails.util.Holders.grailsApplication.metadata['app.name']}ImportRows" durable: true, binding: "${grails.util.Holders.grailsApplication.metadata['app.name']}"
        }
    }
}

