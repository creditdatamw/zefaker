package zefaker

import com.github.javafaker.Faker
import groovy.lang.Binding
import groovy.cli.picocli.CliBuilder
import java.nio.file.Paths
import org.codehaus.groovy.control.CompilerConfiguration

def cli = new CliBuilder(name: 'zefaker')
cli.f(type: File, required: true, 'Groovy file with column definitions')
cli.output(type: String, required: true, 'File to write to, e.g. generated.xlsx')
cli.sheet(type: String, defaultValue: 'Data', 'Sheet name in the generated Excel file')
cli.rows(type: Integer, defaultValue: '10', 'Number of rows to generate')

def options = cli.parse(args)

if (options == null) {
    return
}

def sharedData = new Binding()
def config = new CompilerConfiguration()
config.scriptBaseClass = "zefaker.ZeFaker"
def groovyShell = new GroovyShell(this.class.classLoader, binding, config)

binding.setProperty("faker", new Faker())
binding.setProperty("sheetName", options.sheet)
binding.setProperty("maxRows", options.rows)
binding.setProperty("outputFile", options.output)

groovyShell.evaluate(options.f)
