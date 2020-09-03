// Uncomment one of the quoteIdenfiersAs lines below to add column quoting for SQL exports
// quoteIdentifiersAs("mysql")
// quoteIdentifiersAs("postgres")
// quoteIdentifiersAs("mssql")

firstName = column(index= 0, name= "first_name")
lastName  = column(index= 1, name= "last_name")
age       = column(index= 2, name= "age")

accountStatus = column(index=3, name="account_status")

columns = [
    (firstName): { faker -> faker.name().firstName()  },
    (lastName): { faker -> faker.name().lastName() },
    (age): { faker -> faker.number().numberBetween(18, 70) },
    (accountStatus): { faker -> faker.options().option("Open", "Closed") }
]
// NOTE: This last line is necessary for zefaker to work.
generateFrom columns