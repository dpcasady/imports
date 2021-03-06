package grails.plugins.imports.test

class TestImportItem1 {
	String emailValue
	Long longValue
	Double doubleValue
	Integer integerValue
	Integer integerValueMarshalled
	String stringValue
	String stringValueDefault
	String stringValueBeforeBind
	String stringValueAfterBind
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
