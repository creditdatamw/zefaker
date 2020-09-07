package zefaker

import com.github.javafaker.Faker

import java.nio.file.Paths
import java.nio.file.Files
import java.io.BufferedWriter
import java.util.stream.Collectors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

class SqlCopyFileGenerator implements Runnable {
    def columnDefs
    def filePath
    def tableName = "data"
    def maxRows = 10
    def quoteMode = ColumnQuotes.NONE
    final CountDownLatch latch
    final Faker faker
    final AtomicLong generated = new AtomicLong(0)

    SqlCopyFileGenerator(faker, filePath, columnDefs, tableName, maxRows, latch) {
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
        sb.append("SET DATESTYLE TO ISO;\n")
        sb.append("COPY ")
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
            .append(")")
            .append(" FROM stdin;");

        def bufferedWriter = Files.newBufferedWriter(filePath)

        try {
            bufferedWriter.write(sb.toString())
            bufferedWriter.newLine()

            def rowValues = new Object[columnDefs.size()]

            while(generated.get() < maxRows) {
                columnDefs.each {
                    def col = it.getKey()
                    def fakerFunc = it.getValue()
                    def generatedValue = fakerFunc(faker)
                    rowValues[col.index] = generatedValue
                }

                def copyLineString = createCopyLine(rowValues)
                bufferedWriter.write(copyLineString)
                bufferedWriter.newLine()

                generated.incrementAndGet();
            }
            bufferedWriter.write("\\.")
            bufferedWriter.newLine()
            bufferedWriter.flush();

        } catch (Exception e) {
            bufferedWriter.close()

            throw new RuntimeException("Failed to generate file", e)
        } finally {
            latch.countDown()
        }
    }

    String createCopyLine(rowValues) {
        def valuesString = Arrays.stream(rowValues)
            .map({ it -> 
                if (it == null) return "\\N"
                if (it instanceof String) {
                    if (it.contains("'") || it.contains("\"")) {
                        return String.format("\"%s\"", it.replace("'", "''"))
                    }
                }
                return String.format("%s", it)
            })
            .collect(Collectors.joining("\t"))

        return valuesString;
    }
}   