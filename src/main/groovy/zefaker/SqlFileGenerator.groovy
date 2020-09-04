package zefaker

import com.github.javafaker.Faker

import java.nio.file.Paths
import java.nio.file.Files
import java.io.BufferedWriter
import java.util.stream.Collectors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

class SqlFileGenerator implements Runnable {
    def columnDefs
    def filePath
    def tableName = "data"
    def maxRows = 10
    def quoteMode = ColumnQuotes.NONE
    final CountDownLatch latch
    final Faker faker
    final AtomicLong generated = new AtomicLong(0)

    final VALUES_PLACEHOLDER = "__values__"

    SqlFileGenerator(faker, filePath, columnDefs, tableName, maxRows, latch) {
        this.faker = faker
        this.filePath = filePath
        this.columnDefs = columnDefs
        this.tableName = tableName
        this.latch = latch
        this.maxRows = maxRows
    }

    void setQuoteMode(quoteMode) {
        this.quoteMode = quoteMode
    }

    void run() {
        StringBuilder sb = new StringBuilder()
        sb.append("INSERT INTO ")
            .append(tableName)
            .append(" (")
            // TODO: consider order of the columns?
            .append(columnDefs.keySet()
                .stream()
                .map({ it ->
                    switch(quoteMode) {
                        case ColumnQuotes.MSSQL:
                            return String.format("[%s]", it.name)
                        case ColumnQuotes.MYSQL:
                            return String.format("`%s`", it.name)
                        case ColumnQuotes.POSTGRESQL:
                            return String.format("\"%s\"", it.name)
                        case ColumnQuotes.NONE:
                        default:
                            return it.name
                    }
                })
                .collect(Collectors.joining(",")))
            .append(") ")
            .append("VALUES (")
            .append(VALUES_PLACEHOLDER)
            .append(");");

        def sqlInsertTemplate = sb.toString()

        def bufferedWriter = Files.newBufferedWriter(filePath)

        try {
            def rowValues = new Object[columnDefs.size()]

            while(generated.get() < maxRows) {
                columnDefs.each {
                    def col = it.getKey()
                    def fakerFunc = it.getValue()
                    def generatedValue = fakerFunc(faker)
                    rowValues[col.index] = generatedValue
                }

                def sqlStatement = createInsertStatement(sqlInsertTemplate, rowValues)
                bufferedWriter.write(sqlStatement)
                bufferedWriter.newLine()

                generated.incrementAndGet();
            }
            
            bufferedWriter.flush();

        } catch (Exception e) {
            bufferedWriter.close()

            throw new RuntimeException("Failed to generate file", e)
        } finally {
            latch.countDown()
        }
    }

    String createInsertStatement(sqlTemplate, rowValues) {
        //def rowValuesQuotesReplaced = rowValues.map {
        //    
        //    return it
        //}
        // use rowValuesQuotesReplaced
        def valuesString = Arrays.stream(rowValues)
            .map({ it -> 
                if (it == null) return null
                if (it instanceof String) {
                    return String.format("'%s'", it.replace("'", "''"))
                }
                return String.format("%s", it)
            })
            .collect(Collectors.joining(","))

        return sqlTemplate.replace(VALUES_PLACEHOLDER, valuesString)
    }
}   