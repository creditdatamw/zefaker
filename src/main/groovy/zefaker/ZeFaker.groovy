package zefaker

import com.github.javafaker.Faker
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.nio.file.Paths
import java.nio.file.Files
import java.util.stream.Collectors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

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
 
    protected CountDownLatch latch = new CountDownLatch(1)

    ColumnDef column(int index, String name) {
        return new ColumnDef(index, name, { faker -> "" })
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

    void generateFrom(columnDefs) throws IOException {
        try {
            actualGenerateFrom(columnDefs)
        } catch(Exception e) {
            e.printStackTrace(System.err)
            latch.countDown();
        }
    }

    private void actualGenerateFrom(columnDefs) throws IOException {
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

        def fileGenerator = new ExcelFileGenerator(faker, filePath, columnDefs, sheetName, streamingBatchSize, maxRows, latch)
        
        if (exportAsSql) {
            fileGenerator = new SqlFileGenerator(faker, filePath,  columnDefs, tableName, maxRows, latch)
            fileGenerator.setQuoteMode(sqlQuoteMode)
        }

        new Thread(fileGenerator).start()

        String display = "Generated rows: 0 / " + maxRows
        
        if (verbose) {
            System.out.print(display)
        }

        int gen = 0
        while(true) {
            gen = fileGenerator.generated.get()
            
            if (gen >= maxRows) break
            if (verbose) {
                System.out.print(repeat("\b", display.length()))
                display = "Generated rows: " + gen + " / " + maxRows
                System.out.print(display)
            }
        }

        latch.await()

        if (verbose) {
            System.out.print(repeat("\b", display.length()))
            display = "Generated rows: " + gen + " / " + maxRows
            System.out.print(display)
        }
    }

    private String repeat(String v, int n) {
        return Collections.nCopies(n, v).stream().collect(Collectors.joining())
    }  
}