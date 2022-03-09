package zefaker

import net.datafaker.Faker
import com.opencsv.CSVWriter

import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

class CsvGenerator implements Generator {
    final CsvOptions opts

    CsvGenerator(CsvOptions opts) {
        this.opts = opts
    }

    @Override
    String name() {
        return "CSV"
    }

    @Override
    String fileExtension() {
        return ".csv"
    }

    @Override
    void generate(Faker faker, Map<ColumnDef, Closure> columnDefs, int maxRows, Flushable flushable) {
        def buf = flushable as Writer

        CSVWriter writer = new CSVWriter(buf, opts.separator, opts.quoteChar, opts.escapeChar, opts.lineSeparator)

        AtomicLong generated = new AtomicLong(0)
        try {
            List<String> headers = columnDefs.keySet()
                .stream()
                .map({ it ->
                    return it.name
                })
                .collect(Collectors.toList())
            String[] columnHeaders = new String[columnDefs.size()]
            headers.toArray(columnHeaders)
            writer.writeNext(columnHeaders)

            def rowValues = new String[columnDefs.size()]
            while(generated.get() < maxRows) {
                columnDefs.each {
                    def col = it.getKey()
                    def fakerFunc = it.getValue()
                    def generatedValue = fakerFunc(faker)
                    rowValues[col.index] = String.valueOf(generatedValue)
                }

                writer.writeNext(rowValues)
                generated.incrementAndGet()
            }

        } catch(IOException e) {
            throw e
        }
    }
}
