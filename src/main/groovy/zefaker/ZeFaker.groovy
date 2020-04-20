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

    class ColumnDef {
        int index
        String name
        Closure faker

        public ColumnDef(int index, String name, Closure faker) {
            this.index = index
            this.name = name
            this.faker = faker
        }
    }

    ColumnDef column(int index, String name) {
        return new ColumnDef(index, name, { faker -> "" })
    }

    void generateFrom(columnDefs) throws IOException {
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

        def wb = new SXSSFWorkbook(streamingBatchSize)
        
        if (verbose) {
            System.out.println("Generating File: " + filePath)
            System.out.println("Sheet: " + sheetName)
            System.out.println("Rows: " + maxRows)
        }

        def fileGenerator = new FileGenerator(filePath,  columnDefs, wb)

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

    String repeat(String v, int n) {
        return Collections.nCopies(n, v).stream().collect(Collectors.joining())
    }

    private class FileGenerator extends Runnable {
        Workbook wb
        def columnDefs
        def filePath
        final AtomicLong generated = new AtomicLong(0)

        FileGenerator(filePath, columnDefs, workbook) {
            this.filePath = filePath
            this.wb = workbook
            this.columnDefs = columnDefs
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
}