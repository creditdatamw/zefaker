// Uncomment one of the quoteIdenfiersAs lines below to add column quoting for SQL exports
// quoteIdentifiersAs("mysql")
// quoteIdentifiersAs("postgres")
// quoteIdentifiersAs("mssql")
locale("en-GB")

// Uncomment the line below to instruct zefaker to output data using COPY
// NB: Only postgresql COPY is supported at the moment. 
//
// useSQLCOPY() 

// Specify options that affect CSV output via csvOptions reference
// you can specify the following: separator, quoteChar, lineSeparator, escapeChar
csvOptions.separator = '|'

firstName = column(index= 0, name= "first_name")
lastName  = column(index= 1, name= "last_name")
age       = column(index= 2, name= "age")

accountStatus = column(index=3, name="account_status")

columns = [
    "first_name": { faker -> faker.name().firstName()  },
    "last_name": { faker -> faker.name().lastName() },
    "age": { faker -> faker.number().numberBetween(18, 70) },
    "account_status": { faker -> faker.options().option("Open", "Closed") }
]
// NOTE: This last line is necessary for zefaker to work.
generateFrom columns