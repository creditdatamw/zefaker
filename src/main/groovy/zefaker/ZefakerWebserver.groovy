import spark.*
import com.github.javafaker.Faker
import groovy.cli.picocli.CliBuilder
import org.codehaus.groovy.control.CompilerConfiguration

import static spark.Spark.*

class ZefakerWebserver {

    def run() {
        staticFileLocation("/public")
        
        redirect.get("/", "/index.html")

        post("/generate",  { request, response ->
            def binding = new Binding()

            def bodyAsText = request.body()

            def outFileName = request.getQueryParams("fileName")
            def exportFormat = request.getQueryParams("format")
            binding.setProperty("sheetName", request.getQueryParams("sheet"))
            binding.setProperty("maxRows", request.getQueryParams("rows"))
            binding.setProperty("tableName", request.getQueryParams("table"))

            def config = new CompilerConfiguration()
            config.scriptBaseClass = "zefaker.ZeFaker"
            def groovyShell = new GroovyShell(this.class.classLoader, binding, config)

            binding.setProperty("overwriteExisting", true)
            binding.setProperty("faker", new Faker())
            binding.setProperty("outputFile", tmpFileName)
            
            extension = ""
            outputContentType = "application/json"
            // Options for the Excel output
            switch(exportFormat) {
                case "sql":
                case "sqlcopy":
                    binding.setProperty("exportAsSql", true)
                    outputContentType = "text/plain"
                    extension = ".sql"
                    break
                case "json":
                    binding.setProperty("exportAsJson", true)
                    outputContentType = "application/json"
                    extension = ".json"
                    break
                case "jsonl":
                    binding.setProperty("exportAsJsonLines", true)
                    outputContentType = "application/json"
                    extension = ".jsonl"
                    break
                case "csv":
                    binding.setProperty("exportAsCsv", true)
                    outputContentType = "text/csv"
                    extension = ".csv"
                    break
                default:
                    binding.setProperty("exportAsExcel", true)
                    outputContentType = "application/vnd+msexcel"
                    extension = ".xlsx"
            }
            // Options for the SQL output
            try {
                tmpFileName = Files.newTempFile("zefaker")
                Files.writeLines(bodyAsText, tmpFileName)
                groovyShell.evaluate(tmpFileName)
            } catch(Exception e) {
                errorResponse = "Failed to generate file"
                response.body(errorResponse)
                response.type("application/json")
                return response
            }

            response.type(outputContentType)
            response.headers().add("Content-Disposition", "filename=$outFileName.$extension")
            response.body(Files.readAllBytes(outputFile))
            return response
        })
    }
}

