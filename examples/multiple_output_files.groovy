/**
 * This example shows how to output to multiple files using the same 
 * configuration in one go.
 * 
 * This technique is useful if you want to create to model different scenarios
 * or records with the same record structure.
 * 
 * This particular example creates 10 customer files each with 700 transactions
 * from a fictional Malawian mobile-money system.
 * 
 * Run it using
 * 
 * java -jar zefaker.jar -f=multiple_output_files.groovy -output=foo -rows=700
 */
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE


class IncrementingDays {
    AtomicLong prev

    IncrementingDays(start) {
        this.prev = new AtomicLong(start.toEpochDay())
    }

    def currentDate() {
        return LocalDate.ofEpochDay(this.prev.get())
    }

    def nextDate(faker) {
        this.prev.addAndGet(faker.number().numberBetween(1, 5))
        return this.currentDate()
    }
}

malawianCityFaker = { faker ->
    faker.options().option(
        "Blantyre", "Lilongwe", "Mzuzu", "Zomba", "Kasungu"
    )
}

productFaker = { faker ->
    faker.options().option(
        "ESCOM - Prepaid Electricty Units", 
        "BWB - Prepaid Water Bill",
        "Airtel - Airtime",
        "Airtel - Internet Bundle",
        "TNM - Airtime",
        "TNM - Internet Bundle",
    )
}

nationalIDFaker = { faker -> 
    "AB" + 
    faker.number().numberBetween(10, 19) + 
    faker.options().option("J", "K", "L", "M") + 
    faker.options().option("V", "X", "Y", "Z") +
    "DE"
}

txnTimestamp = new AtomicLong()

dateIncrementer = new IncrementingDays(LocalDate.of(2016, 1, 1))
customerID = ""
customerName = ""
customerNationalID = ""
customerGender = ""
customerCity = ""
customerRegistrationDate = ""

// Maximum number of rows for each file
maxRows = 700

// This loop will result in multiple calls to the generate function which
// enables us to output multiple files 
for (int i = 0; i < 10; i++) {
    randomYear = faker.number().numberBetween(2015, 2017)

    dateIncrementer = new IncrementingDays(LocalDate.of(randomYear, 1, 1))

    customerID = "CUST-22000" + i
    customerName = faker.name.fullName()
    customerNationalID = nationalIDFaker(faker)
    customerGender = faker.options().option("M", "F")
    customerCity = malawianCityFaker(faker)
    customerRegistrationDate = dateIncrementer.nextDate(faker).format(BASIC_ISO_DATE)

    // This sets the outputFile and overrides the one set on the command-line
    outputFile = customerName.replace("/\\s?/","_").concat(".xlsx")

    generateFrom([
        (column(index=0, name="Customer ID")): { faker -> customerID },
        (column(index=1, name="Customer Name")): { faker -> customerName },
        (column(index=2, name="Registered At")): { faker ->  customerRegistrationDate },
        (column(index=3, name="Amount")): { faker -> faker.number().randomDouble(3, 1000, 30000) },
        (column(index=4, name="TXN Date")): { faker -> 
            dateIncrementer.nextDate(faker).format(BASIC_ISO_DATE) 
        },
        (column(index=5, name="Product Name")): productFaker,
        (column(index=6, name="Reference No")): { faker -> 
            "TXN-" + customerID + "-" + dateIncrementer.currentDate() 
        },
        (column(index=7,  name="Customer National ID")): { faker -> customerNationalID },
        (column(index=8,  name="Customer City")): { faker -> customerCity },
        (column(index=9,  name="Customer Gender")): { faker -> customerGender },
        (column(index=10, name="Customer Age")): { faker -> "Unknown" },
        (column(index=11, name="Preferred Language")): { faker -> "Chichewa" },
        (column(index=12, name="TXN Language")): { faker -> 
            faker.options().option("Chichewa", "English")
        },
        (column(index=13, name="Tax Inclusive")): { faker -> faker.bool().bool() },
        (column(index=14, name="Tax Percentage")): { faker -> 0.16 },
        (column(index=15, name="TXN Currency")): { faker -> "MWK" },
        (column(index=16, name="TXN Duration (Seconds)")): { faker ->  Math.random() },
        (column(index=17, name="TXN Result")): { faker -> 
            faker.options().option("SUCCESS") // "FAILED", "INCOMPLETE", "QUEUED"
        },
        (column(index=18, name="TXN Source")): { faker -> 
            faker.options().option("Android", "USSD", "Web App", "iOS", "Other")
        },
        (column(index=19, name="Platform Version")): { faker -> "v1.0.20100101.100-build:1337" }
    ])
}
