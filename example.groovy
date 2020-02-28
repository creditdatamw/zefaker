firstName = column(index= 0, name= "Firstname")
lastName  = column(index= 1, name= "Last Name")
age       = column(index= 2, name= "Age")

accountStatus = column(index=3, name="Account Status")

columns = [
    (firstName): { faker -> faker.name().firstName() },
    (lastName): { faker -> faker.name().lastName() },
    (age): { faker -> faker.number().numberBetween(18, 70) },
    (accountStatus): { faker -> faker.options().option("Open", "Closed") }
]
// NOTE: This last line is necessary for zefaker to work.
generateFrom columns