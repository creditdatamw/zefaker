package zefaker

import com.github.javafaker.Faker
import groovy.cli.picocli.CliBuilder
import org.codehaus.groovy.control.CompilerConfiguration

def cli = new CliBuilder(name: 'zefaker')
cli.f(type: File, required: false, 'Groovy file with column definitions')
cli.x(type: Boolean, defaultValue: 'false', 'Overwrite existing file')
cli.output(type: String, required: false, 'File to write to, e.g. generated.xlsx')
cli.sheet(type: String, defaultValue: 'Data', 'Sheet name in the generated Excel file')
cli.rows(type: Integer, defaultValue: '10', 'Number of rows to generate')
cli.table(type: String, defaultValue: 'Data', 'Table name in the generated SQL file')
cli.excel(type: Boolean, defaultValue: 'false', 'Export as Excel Workbook')
cli.sql(type: Boolean, defaultValue: 'false', 'Export as SQL INSERTS')
cli.json(type: Boolean, defaultValue: 'false', 'Export generated data as JSON file')
cli.jsonl(type: Boolean, defaultValue: 'false', 'Export generated data as JSON Lines file')
cli.csv(type: Boolean, defaultValue: 'false', 'Export generated data as a CSV file')
cli.vvv(type: Boolean, defaultValue: 'false', 'Show verbose output')
cli.web(type: Boolean, defaultValue: 'false', 'Start webserver')

def options = cli.parse(args)

if (options == null) {
    return
}

if (options.web) {
    new ZefakerWebserver().run()
    return
}

def binding = new Binding()
def config = new CompilerConfiguration()
config.scriptBaseClass = "zefaker.ZeFaker"
def groovyShell = new GroovyShell(this.class.classLoader, binding, config)

binding.setProperty("faker", new Faker())
binding.setProperty("verbose", options.vvv)
binding.setProperty("maxRows", options.rows)
binding.setProperty("outputFile", options.output)
binding.setProperty("overwriteExisting", options.x)
// Options for the Excel output
binding.setProperty("sheetName", options.sheet)
// Options for the SQL output
binding.setProperty("tableName", options.table)
binding.setProperty("exportAsSql", options.sql)
binding.setProperty("exportAsJson", options.json)
binding.setProperty("exportAsJsonLines", options.jsonl)
binding.setProperty("exportAsCsv", options.csv)
binding.setProperty("exportAsExcel", !options.sql && !options.json && !options.csv && !options.jsonl)

groovyShell.evaluate(options.f)
