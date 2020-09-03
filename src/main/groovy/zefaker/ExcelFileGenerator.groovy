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

class ExcelFileGenerator implements Runnable {
    Workbook wb
    def columnDefs
    def filePath
    def maxRows
    final CountDownLatch latch
    final Faker faker
    final AtomicLong generated = new AtomicLong(0)

    ExcelFileGenerator(faker, filePath, columnDefs, streamingBatchSize, maxRows, latch) {
        this.faker = faker
        this.filePath = filePath
        this.wb = new SXSSFWorkbook(streamingBatchSize)
        this.columnDefs = columnDefs
        this.latch = latch
        this.maxRows = maxRows
    }

    void run() {

        try {
            def fos = Files.newOutputStream(filePath)
            def sheet = this.wb.createSheet(WorkbookUtil.createSafeSheetName(sheetName))
            def dateCellStyle = this.wb.createCellStyle()
            
            dateCellStyle.setDataFormat(
                // TODO: Enable user to specify a date format in the script
                this.wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd")
            );

            // Create file headers
            def row = sheet.createRow(0)
            int i = 0

            columnDefs.keySet().each {
                def cell = row.createCell(it.index)
                cell.setCellValue(it.name)
                // TODO: if(s.contains("DATE")) cell.setCellStyle(dateCellStyle);
                ++i;
            }

            try {
                populateSheet(sheet, columnDefs)
            } catch(Exception e) {
                System.err.println("ERROR: Exception during file processing: " + e.getMessage())
            } finally {
                wb.write(fos)
                fos.close()

                wb.dispose() // remove temporary files
                wb.close()
                sheet = null
            }
            
            latch.countDown()

        } catch (IOException e) {
            latch.countDown()
            throw new RuntimeException("Failed to generate file", e)
        }
    }

    /**
    * Populate the Sheet using the given column definitions 
    * @param sheet The sheet to write to
    * @param columnDefs map of column definitions 
    */
    void populateSheet(sheet, columnDefs) {
        int nextRow = 1
        Row row = null

        while(generated.get() < maxRows) {
            row = sheet.createRow(nextRow)

            columnDefs.each {
                def col = it.getKey()
                def fakerFunc = it.getValue()
                generatedValue = fakerFunc(faker)
                row.createCell(col.index).setCellValue(generatedValue)
            }

            nextRow++;
            generated.incrementAndGet();
        }
    }
} 