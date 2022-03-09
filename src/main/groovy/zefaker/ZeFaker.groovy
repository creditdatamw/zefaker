package zefaker

import net.datafaker.Faker

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
abstract class ZeFaker extends Script {
    Faker faker
    int streamingBatchSize = 100
    private ColumnQuotes sqlQuoteMode = ColumnQuotes.NONE
    private boolean sqlCopyMode = false 
    CsvOptions csvOptions = new CsvOptions()

    protected CountDownLatch latch = new CountDownLatch(1)

    static ColumnDef column(int index, String name) {
        return new ColumnDef(index, name, { faker -> "" })
    }

    void locale(String localeTag) {
        this.faker = new Faker(Locale.forLanguageTag(localeTag))
    }

    void useFaker(fakerInstance) {
        this.faker = fakerInstance
    }

    // User should call this to indicate they want SQL COPY format created
    // instead of regular INSERTS
    //
    // Support for Postgresql COPY format only, currently
    void useSQLCOPY() {
        sqlCopyMode = true
        if (sqlQuoteMode != ColumnQuotes.POSTGRESQL)
            System.err.println("Only Postgresql is supported for useSQLCOPY, please use `quoteIdentifiersAs(\"postgres\")` in your Zefaker script")
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

    private _setAndSort(Map<ColumnDef, Closure> columnDefs) {
        columnDefs.forEach { col, func ->
            col.faker = func
        }
        columnDefs.sort { a, b -> (a.key <=> b.key) }
    }

    void generateFrom(columnDefs) {
        assert columnDefs != null
        assert sheetName != null
        assert outputFile != null
        assert maxRows >= 1 && maxRows <= Integer.MAX_VALUE

        if (faker == null)
            faker = new Faker()

        if (columnDefs.isEmpty()) {
            throw new RuntimeException("No columns specified")
        }

        // Support string columns in the specification
        if (columnDefs.keySet().iterator().next() instanceof String) {
            columnDefs = ColumnDef.fromStringColumns(columnDefs)
        }
        
        this._setAndSort(columnDefs)

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
        } else if (exportAsJsonLines) {
            fileGenerator = new JsonLinesFileGenerator()
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