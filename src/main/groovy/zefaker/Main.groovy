package zefaker

import com.github.javafaker.Faker
import groovy.cli.picocli.CliBuilder
import org.codehaus.groovy.control.CompilerConfiguration

def cli = new CliBuilder(name: 'zefaker')
cli.f(type: File, required: true, 'Groovy file with column definitions')
cli.x(type: Boolean, defaultValue: 'false', 'Overwrite existing file')
cli.output(type: String, required: true, 'File to write to, e.g. generated.xlsx')
cli.vvv(type: Boolean, defaultValue: 'false', 'Show verbose output')
// format specific arguments
cli.sheet(type: String, defaultValue: 'Data', 'Sheet name in the generated Excel file')
cli.rows(type: Integer, defaultValue: '10', 'Number of rows to generate')
cli.table(type: String, defaultValue: 'Data', 'Table name in the generated SQL file')
// Export format arguments
cli.format(type: String, defaultValue: 'excel', required: false, 'Export format, one of csv, sql, sqlcopy, excel, json, jsonl')
cli.excel(type: Boolean, defaultValue: 'false', 'Export as Excel Workbook')
cli.sql(type: Boolean, defaultValue: 'false', 'Export as SQL INSERTS')
cli.json(type: Boolean, defaultValue: 'false', 'Export generated data as JSON file')
cli.jsonl(type: Boolean, defaultValue: 'false', 'Export generated data as JSON Lines file')
cli.csv(type: Boolean, defaultValue: 'false', 'Export generated data as a CSV file')

def options = cli.parse(args)

if (options == null) {
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

def format = "excel"

if (options.sql) format = "sql"
if (options.csv) format = "csv"
if (options.json) format = "json"
if (options.jsonl) format = "jsonl"

binding.setProperty("format", format)
// Options for the Excel output
binding.setProperty("sheetName", options.sheet)
// Options for the SQL output
binding.setProperty("tableName", options.table)

groovyShell.evaluate(options.f)
