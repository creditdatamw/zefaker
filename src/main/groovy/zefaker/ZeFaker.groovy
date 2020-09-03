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

abstract class ZeFaker extends groovy.lang.Script {
    Faker faker
    int streamingBatchSize = 100
    protected CountDownLatch latch = new CountDownLatch(1)

    ColumnDef column(int index, String name) {
        return new ColumnDef(index, name, { faker -> "" })
    }

    void generateFrom(columnDefs) throws IOException {
        try {
            actualGenerateFrom(columnDefs)
        } catch(Exception e) {
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
            System.out.println("Sheet: " + sheetName)
            System.out.println("Rows: " + maxRows)
        }

        def fileGenerator = new ExcelFileGenerator(filePath,  columnDefs, streamingBatchSize, latch)
        
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
            display = "Generated rows: " + maxRows + " / " + maxRows
            System.out.print(display)
        }
    }

    private String repeat(String v, int n) {
        return Collections.nCopies(n, v).stream().collect(Collectors.joining())
    }  
}