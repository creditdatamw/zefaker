/**
 * This example shows how to use the same configuration file to generate two
 * files with different columns!
 * This allows you to generate a set of related files with one configuration.
 * 
 * java -jar zefaker.jar -f=multi_configuration.groovy -output=dummy
 */

// number of rows to create in the first file
maxRows = 10
// the name of the first file to output
outputFile = "companies.xlsx"
// column configuration for the first file
generateFrom([
    (column(index= 0, name="Account No.")): { faker -> faker.number().numberBetween(1, 200) },
    (column(index= 1, name="Company Name")): { faker -> faker.name.fullName()  + faker.options().option("Plc", "Pvt Ltd", "") },
    (column(index= 2, name="TPIN")): { faker -> "TPIN" + faker.number().numberBetween(1, 200) },
    (column(index= 3, name="Registration Date")): { faker -> "197001-01" },
    (column(index= 4, name="Postal Address")): { faker -> "P.O. Box 123" },
    (column(index= 5, name="Telephone")): { faker -> "265999" + faker.number().numberBetween(111111, 999999) }
])

// number of rows to create in the second file
maxRows = 10
// the name of the second file to output
outputFile = "directors.xlsx"
// column configuration for the second file
generateFrom([
    (column(index= 3, name="Company Account No.")): { faker -> faker.number().numberBetween(1, 200) },
    (column(index= 0, name="Full name")): { faker -> faker.name.fullName() },
    (column(index= 1, name="Phone Number")): { faker -> "265999" + faker.number().numberBetween(111111, 999999) },
    (column(index= 2, name="E-mail")): { faker -> "user@example.com" },
    (column(index= 4, name="Address")): { faker -> "P.O. Box 123" }
])
