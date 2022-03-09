package zefaker

import net.datafaker.Faker
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import groovy.cli.picocli.CliBuilder
import org.codehaus.groovy.control.CompilerConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.*

import java.nio.file.Files
import javax.servlet.http.Part
import javax.servlet.MultipartConfigElement;

import static spark.Spark.*

class GenerateRequestDto {
    String fileName
    String code
    String format
    String sqlquote
    String table
    String sheet
    int rows
    boolean exportAsSql
    boolean exportAsJson
    boolean exportAsJsonLines
    boolean exportAsCsv
    boolean exportAsExcel
}

class ZefakerWebserver {
    final Logger logger = LoggerFactory.getLogger(ZefakerWebserver.class)
    final Gson gson = new Gson()
    
    def run() {
        staticFileLocation("/public")
        
        redirect.get("/", "/index.html")

        post("/generate", this::generateHandler)
    } 
    
    void generateHandler(request, response) {
        def requestDto = gson.fromJson(request.body(), GenerateRequestDto.class)
        if (requestDto.fileName == null) {
            requestDto.fileName = "data"
        }
        def extension = ""
        def outputContentType = "application/octet-stream; charset=utf-8"
        
        // Options for the Excel output
        switch(requestDto.format) {
            case "sql":
            case "sqlcopy":
                requestDto.exportAsSql = true
                extension = ".sql"
                break
            case "json":
                requestDto.exportAsJson = true
                extension = ".json"
                break
            case "jsonl":
                requestDto.exportAsJsonLines = true
                extension = ".jsonl"
                break
            case "csv":
                requestDto.exportAsCsv = true
                extension = ".csv"
                break
            case "xlsx":
                requestDto.exportAsExcel = true
                extension = ".xlsx"
                break
            default:
                response.body("Invalid file format type")
                response.type("application/json")
                response.status(400)
                return
        }
        
        if (requestDto.exportAsSql && (requestDto.table == null || requestDto.table.isEmpty())) {
            response.body("Table name must be specified for SQL output")
            response.type("application/json")
            response.status(400)
            return
        }
        
        if (requestDto.exportAsExcel && (requestDto.sheet == null || requestDto.sheet.isEmpty())) {
            response.body("Sheet Name must be specified for Excel output")
            response.type("application/json")
            response.status(400)
            return
        }

        try {
            def tmpInputFilename = Files.createTempFile("zefaker", ".groovy")
            def tmpOutputFilename = Files.createTempFile("zout-", extension)
            Files.writeString(tmpInputFilename, requestDto.code)

            def binding = new Binding()
            def config = new CompilerConfiguration()
            config.scriptBaseClass = "zefaker.ZeFaker"

            binding.setProperty("sheetName", requestDto.sheet)
            binding.setProperty("maxRows", requestDto.rows)
            binding.setProperty("tableName", requestDto.table)
            binding.setProperty("exportAsSql", requestDto.exportAsSql)
            binding.setProperty("exportAsJson", requestDto.exportAsJson)
            binding.setProperty("exportAsJsonLines", requestDto.exportAsJsonLines)
            binding.setProperty("exportAsCsv", requestDto.exportAsCsv)
            binding.setProperty("exportAsExcel", requestDto.exportAsExcel)
            binding.setProperty("overwriteExisting", true)
            binding.setProperty("verbose", false)
            binding.setProperty("outputFile", tmpOutputFilename.toString())

            logger.info("Trying to generate file from: {} to {}", tmpInputFilename, tmpOutputFilename)
            def groovyShell = new GroovyShell(this.class.classLoader, binding, config)
            groovyShell.evaluate(tmpInputFilename.toFile())

            response.type(outputContentType)
        
            def attachmentName = new StringBuilder()
                .append("attachment; filename=\"")
                .append(requestDto.fileName)
                .append(requestDto.fileName.endsWith(extension) ? "" : extension)
                .append("\"")
                .toString();
            
            response.header("Content-Disposition", attachmentName);
            response.raw().getOutputStream().write(Files.readAllBytes(tmpOutputFilename))
            response.status(200)
            return

        } catch(Exception e) {
            logger.error("Failed to generate file", e)
            def errorResponse = [
                "message": "Failed to generate file",
                "exception": e.getMessage(),
            ]

            response.type("application/json")
            response.body(gson.toJson(errorResponse))
            response.status(400)
        }
    }
}

