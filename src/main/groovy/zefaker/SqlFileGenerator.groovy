package zefaker

import java.util.concurrent.atomic.AtomicLong

import net.datafaker.Faker

import java.util.stream.Collectors


class SqlFileGenerator implements Generator {
    def tableName = "data"
    def quoteMode = ColumnQuotes.NONE
    final AtomicLong generated = new AtomicLong(0)

    final VALUES_PLACEHOLDER = "__values__"

    SqlFileGenerator(tableName, quoteMode) {
        this.tableName = tableName
        this.quoteMode = quoteMode
    }

    String name() {
        return "SQL"
    }

    String fileExtension() {
        return ".sql"
    }

    void generate(Faker faker, Map<ColumnDef, Closure> columnDefs, int maxRows, Flushable output) {
        assert output instanceof  BufferedWriter
        def buf = output as BufferedWriter

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
                            return it.name
                        default:
                            return it.name
                    }
                })
                .collect(Collectors.joining(",")))
            .append(") ")
            .append("VALUES (")
            .append(VALUES_PLACEHOLDER)
            .append(");")

        def sqlInsertTemplate = sb.toString()

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
                buf.write(sqlStatement)
                buf.newLine()

                generated.incrementAndGet()
            }

            buf.flush()

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate file", e)
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