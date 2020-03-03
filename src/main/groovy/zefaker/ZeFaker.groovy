package zefaker

import com.github.javafaker.Faker
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Paths
import java.util.function.Function

abstract class ZeFaker extends groovy.lang.Script {
    Faker faker

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

        try {
            def fos = new FileOutputStream(Paths.get(outputFile).toFile())
            def wb  = new XSSFWorkbook()

            def sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(sheetName))
            def dateCellStyle = wb.createCellStyle()
            
            dateCellStyle.setDataFormat(
                wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd")
            );

            // Create file headers
            def row = sheet.createRow(0)
            int i = 0

            columnDefs.keySet().each {
                def cell = row.createCell(it.index)
                cell.setCellValue(it.name)
                //if(s.contains("DATE")) cell.setCellStyle(dateCellStyle);
                ++i;
            }

            populateSheet(sheet, columnDefs)
    
            wb.write(fos)

            sheet = null
            wb.close()
            fos.close()
        } catch (IOException e) {
            throw e
        }
    }

    /**
     * Populate the Sheet using the given column definitions 
     * @param sheet The sheet to write to
     * @param columnDefs map of column definitions 
     */
    void populateSheet(sheet, columnDefs) {
        int nextRow = 1
        int generated = 0
        while(generated < maxRows) {
            Row row = sheet.createRow(nextRow);

            columnDefs.each {
                def col = it.getKey()
                def fakerFunc = it.getValue()
                generatedValue = fakerFunc(faker)
                row.createCell(col.index).setCellValue(generatedValue);
            }

            nextRow++;
            generated++;
        }
    }
}