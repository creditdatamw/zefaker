package zefaker

import com.github.javafaker.Faker
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.streaming.SXSSFWorkbook

import java.util.concurrent.atomic.AtomicLong

class ExcelFileGenerator implements Generator {
    SXSSFWorkbook wb
    def sheetName = "Sheet 1"
    final AtomicLong generated = new AtomicLong(0)

    ExcelFileGenerator(sheetName, streamingBatchSize) {
        this.sheetName = sheetName
        this.wb = new SXSSFWorkbook(streamingBatchSize)
    }

    String name() {
        return "Excel"
    }

    String fileExtension() {
        return ".xlsx"
    }

    void generate(Faker faker, Map<ColumnDef, Closure> columnDefs, int maxRows, Flushable output) {
        try {
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

            populateSheet(faker, sheet, columnDefs, maxRows)
            
            wb.write(output as OutputStream)

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate file", e)
        } finally {
            wb.dispose() // remove temporary files
            wb.close()
        }
    }

    /**
    * Populate the Sheet using the given column definitions 
    * @param sheet The sheet to write to
    * @param columnDefs map of column definitions 
    */
    void populateSheet(faker, sheet, columnDefs, maxRows) {
        int nextRow = 1
        Row row = null

        while(generated.get() < maxRows) {
            row = sheet.createRow(nextRow)

            columnDefs.each {
                def col = it.getKey()
                def fakerFunc = it.getValue()
                def generatedValue = fakerFunc(faker)
                row.createCell(col.index).setCellValue(generatedValue)
            }

            nextRow++;
            generated.incrementAndGet();
        }
    }
} 