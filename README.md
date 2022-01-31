Zé Faker
========

`zefaker` is a command-line tool that helps you to generate and export data into CSV, Excel and SQL files.

## Features

* Random data generation via [java-faker](https://github.com/DiUS/java-faker)
* Export to CSV
* Export to SQL INSERTS
* Export to Excel files
* Export to JSON

## Why would I use this?

Short answer **to generate (test) data**.

If you ever need to have a CSV, Excel file with (random*) data or need to 
populate a database with development/dummy data you can use `zefaker` to 
generate the data, leveraging the power of [Groovy](https://www.groovy-lang.org)

We created it because we deal with a lot of Excel files (with lots of columns!) and SQL data and often have to generate files to test the code that processes those files or data.

_* the generated data does not have to be random_

## Example Usage

> NOTE: Checkout the [examples](./examples/) for even more examples, and "advanced" usage

Let's go straight to an example:

Create a file named `person.groovy` and place the following content:

```groovy
// person.groovy

locale("en-GB") // tells Java Faker to use the given tag for the Locale.

// You can also use a custom faker
// import com.github.javafaker.Faker
// useFaker(new Faker(Locale.getLanguageTag("en-GB")))

generateFrom([
    "FirstName": { faker -> faker.name().firstName() },
    "LastName": { faker -> faker.name().lastName() },
    "Age": { faker -> faker.number().numberBetween(18, 70) },
    "AccountStatus": { faker -> faker.options().option("Open", "Closed") },
    "Plan": { faker -> "FREE" } // doesn't necessarily have to be a faker value
])
```

Once you have this, you can pass it to the `zefaker` command to generate a file:

### Exporting as a CSV File

```sh
$ zefaker -f person.groovy -rows 1000 -output people.csv -csv 
```

The example command, above, generates a file named **people.csv** with **1000 rows** 
populated with data generated using the Faker functions specified in the Groovy script.

### Exporting as an Excel file

```sh
$ zefaker -f person.groovy -rows 1000 -output people.xlsx -sheet People
```

The example command, above, generates a file named **people.xlsx** with **1000 records**.

> _Bonus / Shameless plug_: If you're using Java, you can process the generated files _quickly_ and 
_efficiently_ using [zerocell](https://github.com/creditdatamw/zerocell).

### Exporting as an SQL file

```sh
$ zefaker -f person.groovy -rows 1000 -output people.sql -sql -table people 
```

The example command, above, generates a file named **people.sql** with 
**1000 INSERT statements** which have random data in the _VALUES_ clause.

### Exporting as a JSON file

```sh
$ zefaker -f person.groovy -rows 1000 -output people.json -json 
```

The example command, above, generates a file named **people.json** with **1000 JSON objects**.


## Installation

Download a copy of `zefaker` from the [Releases](https://github.com/creditdatamw/zefaker/releases) page.

> NOTE: Requires at least Java 8 to be installed

## Usage

The build archive contains start scripts, `zefaker` and `zefaker.bat` for UN*X and Windows environments, respectively.

### Command Line

```sh
Usage: zefaker [options]
  -f=FILE         Groovy file with column definitions
  -rows=ROWS      Number of rows to generate
  -output=FILE    File to write to, e.g. generated.xlsx
  -sheet=NAME     Sheet name in the generated Excel file
  -table=NAME     The name of the table to use in SQL INSERT mode
  -sql            Use SQL INSERT export mode
  -vvv            Show verbose output
  -x              Overwrite existing file
```

### Web based UI

You can run the following `java -jar zefaker.jar -web` to start a webserver at port `4567`:

![[]](zefakerweb.png)

### In the Groovy Script

Within your Groovy script you are required to use the *generateFrom(<map>)* 
function to generate the Excel file.

#### Methods

The following are the only methods that are required in the groovy script for 
Zefaker to run. 

**ColumnDef column(int index, String name)**


This method defines a new Column that has a name and an index (position)

**void generateFrom(ColumnDef[] columns)**

This method actually initiates the generation of the Excel file. If you don't
call this method you won't actually get any result. 

Example: `generateFrom([ (firstName) : { faker -> faker.name().firstName() } ])`

#### Properties

Inside the Groovy script you can set some variables or properties that affect the 
output of zefaker. These variables, consequently, take precedence over arguments 
specified on the command-line. 

The following special variables are available, and are therefore *reserved names*:

* **sheetName** - Change the name of the target Sheet in Excel. Overrides `-sheet`
* **tableName** - Change the name of the target table in SQL INSERTS. Overrides `-table`
* **outputFile** - The name/path of the file to write output to. Overrides `-f`
* **verbose** - Show verbose output. Overrides `-vvv`
* **maxRows** - Sets the maximum number of rows to generate in the file. Overrides `-rows`
* **overwriteExisting** - Whether to overrite an existing file with the new Workbook. Overrides `-x`

## Building

We are using `Gradle` for this, so follow the instructions below to build it.

```sh
$ git clone https://github.com/creditdatamw/zefaker.git
$ cd zefaker
$ gradlew clean build
```

After this, the build file will be in `build/libs/zefaker-all.jar` - it is an executable Jar file.

## Using Docker

You will have to build the image locally at the moment, :). Then run:

```sh
$ docker run --volume "$(pwd):/tmp:rw" zefaker -f /tmp/example.groovy -output /tmp/first.sql -sql 
```
## IDEAS / TODO

This is simple CLI and so far it does what we need, but it can always be improved.
Here are some ideas:

- Decrease the size of the JAR using either Java 9+ modules or Proguard to strip out stuff we don't need
- Build a native binary using [Graal](https://www.graalvm.org/)
- Handle exceptions raised by/in the input script better

## CONTRIBUTING

Pull Requests are welcome. If you run into a problem, create an issue and we will try to resolve it.

---

Copyright (c) 2022, Credit Data CRB Ltd