// Run using:
// 
// $ java -jar zefaker-all.jar -f=db.groovy -output=files -sql -x
// 

/**
 * db.groovy
 * 
 * An example of how to use Zefaker to generate a whole test database
 *
 * Creates  "migration" files that can be used to create tables and populate
 * a database about "Businesses".
 *
 * Creates the following files in the current directory:
 * 
 * * V000_schema.sql                     - Base migration that creates the tables
 * * V001_create_businesses.sql          - Inserts businesses data
 * * V002_create_users.sql               - Inserts Users data
 * * V003_create_businesses_services.sql - Inserts Businesses' Services data
 * 
 */
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat

dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

timestampFaker = { faker -> 
    return dateFormatter.format(faker.date().past(365, TimeUnit.DAYS))
}

class MaxFakedInteger {
    def maxFakedValue = 0
    def fakerFunc
    MaxFakedInteger(fn) {
        this.fakerFunc = fn
    }

    def getMaxID() {
        return this.maxFakedValue
    }

    def newValue(faker) {
        def genVal = fakerFunc(faker)
        if (genVal > this.maxFakedValue) {
            this.maxFakedValue = genVal
        }
        return genVal
    }
}

def addTimestampColumns(theData) {
    modifiedData = theData
    def lastID = 0
    theData.each {
        if (it.getKey().index > lastID) {
            lastID = it.getKey().index
        }
    }

    modifiedData.put(column(index=++lastID,name="created_at"), timestampFaker)
    modifiedData.put(column(index=++lastID,name="modified_at"), timestampFaker)
    modifiedData.put(column(index=++lastID,name="deleted_at"), { faker -> null })

    return modifiedData
} 

businessIDGenerator = new AtomicLong(0)
idGen = new AtomicLong(0)

businessIDFaker = new MaxFakedInteger({ faker -> businessIDGenerator.incrementAndGet() })

businessData = [
 (column(index=0,name="id")): { faker -> businessIDFaker.newValue(faker) },
 (column(index=1,name="business_name")): { faker -> faker.name().fullName() },
 (column(index=2,name="address")): { faker -> faker.name().fullName() },
 (column(index=3,name="registration_date")): { faker -> faker.date().birthday() },
 (column(index=4,name="email")): { faker -> faker.internet().emailAddress() },
]

usersData = [
    (column(index=0,name="id")): { faker -> idGen.incrementAndGet() },
    (column(index=1,name="business_id")): { faker -> 
        faker.number().numberBetween(1, businessIDFaker.getMaxID())
    },
    (column(index=2,name="username")): { faker -> 
        faker.name().fullName().replace("\\s","").toLowerCase()
    },
    (column(index=3,name="subscription_type")): { faker -> 
        faker.options().option('premium', 'team', 'free')
    },
    (column(index=4,name="last_login_at")): timestampFaker
]

businessServicesData = [
    (column(index=0,name="id")): { faker -> idGen.incrementAndGet() },
    (column(index=1,name="business_id")): { faker -> 
        faker.number().numberBetween(1, businessIDFaker.getMaxID())
    },
    (column(index=2,name="service_name")): { faker -> 
        faker.options().option('Electronics', 'Retail', 'Transportation')
    }
]


def sqlSchema = """
CREATE TABLE businesses (
    id int not null primary key,
    business_name varchar(255) not null,
    address varchar(255),
    registration_date date,
    email text,
    created_at timestamp,
    modified_at timestamp,
    deleted_at timestamp
);
CREATE TABLE users (
    id int not null primary key,
    business_id int,
    username varchar(255),
    subscription_type varchar(50),
    last_login_at timestamp,
    created_at timestamp,
    modified_at timestamp,
    deleted_at timestamp

);
CREATE TABLE businesses (
    id int not null primary key,
    business_id int,
    service_name varchar(255),
    created_at timestamp,
    modified_at timestamp,
    deleted_at timestamp
);
"""

Files.write(Paths.get("V000__schema.sql"), sqlSchema.getBytes())

businessData = addTimestampColumns(businessData)
usersData = addTimestampColumns(usersData)
businessServicesData = addTimestampColumns(businessServicesData)

// Businesses
maxRows = 400
tableName = "businesses"
outputFile = "V001__create_businesses.sql"
generateFrom(businessData)  // Generates the file

maxRows = 50
tableName = "users"
outputFile = "V002__create_users.sql"
generateFrom(usersData) // Generates the file

maxRows = 10
tableName = "businesses_services"
outputFile = "V003__create_businesses_services.sql"

idGen.getAndSet(0)

generateFrom(businessServicesData) // Generates the file