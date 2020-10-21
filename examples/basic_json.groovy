/**
 * Basic Example Usage for zefaker for Exporting to JSON 
 * 
 * Run it with a command similar to the one below :
 * 
 * ```sh
 * $ java -jar zefaker.jar -f=basic_json.groovy -output=people.json -rows=100 
 * ```
 */
import com.google.gson.JsonObject

firstName = column(index= 0, name= "firstName")
lastName  = column(index= 1, name= "lastName")
age       = column(index= 2, name= "age")

accountStatus = column(index=3, name="accountStatus")
accountMeta   = column(index=4, name="accountMeta")

columns = [
    (firstName): { faker -> faker.name().firstName() },
    (lastName): { faker -> faker.name().lastName() },
    (age): { faker -> faker.number().numberBetween(18, 70) },
    (accountStatus): { faker -> faker.options().option("Open", "Closed") },
    // You can nest objects like this
    (accountMeta): { faker -> 
        def meta = new JsonObject()
        meta.addProperty("totalTokens", faker.number().numberBetween(5000, 10000))
        meta.addProperty("activityStatus", faker.options().option("Active", "Dormant"))
        return meta
    }
]
// NOTE: This last line is necessary for zefaker to work.
generateFrom columns