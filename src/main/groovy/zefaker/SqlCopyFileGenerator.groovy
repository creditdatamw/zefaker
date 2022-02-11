package zefaker

import java.util.concurrent.atomic.AtomicLong

import net.datafaker.Faker

import java.util.stream.Collectors


class SqlCopyFileGenerator implements Generator {
    def tableName = "data"
    def quoteMode = ColumnQuotes.NONE
    final AtomicLong generated = new AtomicLong(0)

    SqlCopyFileGenerator(tableName, quoteMode) {
        this.tableName = tableName
        this.quoteMode = ColumnQuotes.POSTGRESQL
    }

    String name() {
        return "SQL COPY"
    }

    String fileExtension() {
        return ".sql"
    }

    void generate(Faker faker, Map<ColumnDef, Closure> columnDefs, int maxRows, Flushable output) {
        assert output instanceof  BufferedWriter
        def buf = output as BufferedWriter
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

        try {
            buf.write(sb.toString())
            buf.newLine()

            def rowValues = new Object[columnDefs.size()]

            while(generated.get() < maxRows) {
                columnDefs.each {
                    def col = it.getKey()
                    def fakerFunc = it.getValue()
                    def generatedValue = fakerFunc(faker)
                    rowValues[col.index] = generatedValue
                }

                def copyLineString = createCopyLine(rowValues)
                buf.write(copyLineString)
                buf.newLine()

                generated.incrementAndGet();
            }
            buf.write("\\.")
            buf.newLine()
            buf.flush();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate file", e)
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