Zé Faker
========

`zefaker` is a command-line tool that allows you to generate Excel files 
using a simple Groovy DSL and [java-faker](https://github.com/DiUS/java-faker)

## Why would I use this?

Well, if you ever need to generate an Excel file with (random*) data for whatever
reason you can use `zefaker` to automate the process while leveraging the power of
[Groovy](https://www.groovy-lang.org)!

We created it because we deal with a lot of Excel files (with lots of columns!) 
and often have to generate files to test the code that processes those files.

_* the generated data need not necessarily be random_

## Example Usage

Let's go straight to an example:

Create a file named `person.groovy` and place the following content:

```groovy
// person.groovy

firstName = column(index= 0, name="Firstname")
lastName  = column(index= 1, name="Last Name")
age       = column(index= 2, name="Age")

accountStatus = column(index=3, name="Account Status")

columns = [
    (firstName): { faker -> faker.name().firstName() },
    (lastName): { faker -> faker.name().lastName() },
    (age): { faker -> faker.number().numberBetween(18, 70) },
    (accountStatus): { faker -> faker.options().option("Open", "Closed") }
]
// NOTE: This last line is necessary for zefaker to work.
generateFrom columns
```

Once you have this, you can pass it to the `zefaker` command to generate an Excel file:

```sh
$ java -jar zefaker.jar -f=person.groovy -sheet="Persons" -rows=100 -output=people.xlsx
```

The example command, above, generates a file named **people.xlsx** with a **100 rows** populated
with data generated using the faker methods specified in the groovy script.

_Bonus / Shameless plug_: If you're using Java, you can process the generated files _quickly_ and 
_efficiently_ using [zerocell](https://github.com/creditdatamw/zerocell).

## Installation

Download a copy of `zefaker` from the [Releases](https://github.com/creditdatamw/zefaker/releases) page.

> NOTE: Requires at least Java 8 to be installed

## Usage

### Command Line

```sh
Usage: zefaker -f=PARAM -output=PARAM [-rows=PARAM] [-sheet=PARAM]
    -f=PARAM        Groovy file with column definitions
    -output=PARAM   File to write to, e.g. generated.xlsx
    -rows=PARAM     Number of rows to generate
    -sheet=PARAM    Sheet name in the generated Excel file
```

### In the Groovy Script

The following are the only methods that are required in the groovy script for 
Zefaker to run. 

**ColumnDef column(int index, String name)**

This method defines a new Column that has a name and an index (position)

**void generateFrom(ColumnDef[] columns)**

This method actually initiates the generation of the Excel file. If you don't
call this method you won't actually get any result. 

## Building

We are using `Gradle` for this, so follow the instructions below to build it.

```sh
$ git clone https://github.com/creditdatamw/zefaker.git
$ cd zefaker
$ gradlew clean jar shadowJar
```

After this, the build file will be in `build/libs/zefaker-all.jar` - it is an executable Jar file.

## IDEAS / TODO

This is simple CLI and so far it does what we need, but it can always be improved.
Here are some ideas:

- Decrease the size of the JAR using Java 9+ modules to strip out stuff we don't need
- Build a native binary using GraalVM
- Handle exceptions raised by/in the input script better
- Explore using multi-threading to generate the rows
- Export to CSV?

## CONTRIBUTING

Pull Requests are welcome. If you run into a problem, create an issue and we will try to resolve it.

---

Copyright (c) Credit Data CRB Ltd