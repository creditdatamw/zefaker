package zefaker

import com.github.javafaker.Faker

/**
 * Generator interface for supported generator classes.
 */
interface Generator {
    String name()

    String fileExtension()

    void generate(Faker faker, Map<ColumnDef, Closure> columnDefs, int maxRows, Flushable output)
}
