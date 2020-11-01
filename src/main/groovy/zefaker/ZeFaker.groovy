package zefaker

import com.github.javafaker.Faker

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch

/**
 * ZeFaker main Script 
 * 
 * Passes the parameters from the groovy script to the file generators.
 * Accepts the following properties, which may be specified in the script.
 *
 * <ul>
 *   <li>{@code String outputFile}</li>
 *   <li>{@code Faker faker}</li>
 *   <li>{@code ColumnQuotes sqlQuoteMode}</li>
 *   <li>{@code int maxRows}</li>
 *   <li>{@code int streamingBatchSize}</li>
 *   <li>{@code boolean exportAsSql}</li>
 * </ul>
 */
abstract class ZeFaker extends groovy.lang.Script {
    Faker faker
    int streamingBatchSize = 100
    private ColumnQuotes sqlQuoteMode = ColumnQuotes.NONE
    private boolean sqlCopyMode = false 
    CsvOptions csvOptions = new CsvOptions()

    protected CountDownLatch latch = new CountDownLatch(1)

    static ColumnDef column(int index, String name) {
        return new ColumnDef(index, name, { faker -> "" })
    }

    // User should call this to indicate they want SQL COPY format created
    // instead of regular INSERTS
    //
    // Support for Postgresql COPY format only, currently
    void useSQLCOPY() {
        sqlCopyMode = true && sqlQuoteMode == ColumnQuotes.POSTGRESQL
        if (!sqlCopyMode) System.err.println("Only Postgresql is supported for useSQLCOPY, please use `quoteIdentifiersAs(\"postgres\")` in your Zefaker script")
    }

    void quoteIdentifiersAs(stringVal) {
        sqlQuoteMode = ColumnQuotes.NONE
        
        if (stringVal == null) return

        if ("mysql".equalsIgnoreCase(stringVal) ||
            "mariadb".equalsIgnoreCase(stringVal) ||
            "maria".equalsIgnoreCase(stringVal)) {
            sqlQuoteMode = ColumnQuotes.MYSQL
        }

        if ("postgres".equalsIgnoreCase(stringVal) ||
            "postgresql".equalsIgnoreCase(stringVal) ||
            "pg".equalsIgnoreCase(stringVal)) {
            sqlQuoteMode = ColumnQuotes.POSTGRESQL
        }

        if ("sqlserver".equalsIgnoreCase(stringVal) ||
            "mssql".equalsIgnoreCase(stringVal)) {
            sqlQuoteMode = ColumnQuotes.MSSQL
        }
    }

    void generateFrom(columnDefs) {
        if (faker == null)
            faker = new Faker()

        assert sheetName != null
        assert outputFile != null
        assert maxRows >= 1 && maxRows <= Integer.MAX_VALUE

        def filePath = Paths.get(outputFile)

        if (!overwriteExisting && Files.exists(filePath)) {
            System.err.println("Cannot overwrite existing file: " + filePath)
            return
        }

        if (verbose) {
            System.out.println("Generating File: " + filePath)
            if (exportAsSql) {
                System.out.println("Table: " + tableName)
            } else {   
                System.out.println("Sheet: " + sheetName)
            }
            System.out.println("Rows: " + maxRows)
        }

        def fileGenerator

        if (exportAsJson) {
            fileGenerator = new JsonFileGenerator()
        } else if (exportAsCsv) {
            fileGenerator = new CsvGenerator(csvOptions)
        } else if (exportAsSql) {
            if (sqlCopyMode) {
                fileGenerator = new SqlCopyFileGenerator(tableName)
            } else {
                fileGenerator = new SqlFileGenerator(tableName, sqlQuoteMode)
            }
        } else {
            fileGenerator = new ExcelFileGenerator(sheetName, streamingBatchSize)
        }

        final Flushable fw
        if (exportAsExcel) {
            fw = Files.newOutputStream(filePath)
        } else {
            fw = Files.newBufferedWriter(filePath)
        }
        
        try {
            fileGenerator.generate(faker, columnDefs, maxRows, fw)
        } catch(Exception e) {
            throw new RuntimeException("An error occurred: " + e.getMessage(), e)
        } finally {
            fw.close()
        }   
    }
}