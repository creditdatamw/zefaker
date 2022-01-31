package zefaker

class ColumnDef implements Comparable<ColumnDef> {
    int index
    String name
    Closure faker

    public ColumnDef(int index, String name, Closure faker) {
        this.index = index
        this.name = name
        this.faker = faker
    }

    @Override
    int compareTo(ColumnDef o) {
        if (o == null) return 1
        return Integer.compare(this.index, o.getIndex())
    }

    static Map<ColumnDef, Closure> fromStringColumns(Map stringColumnDefs) {
        def lastIdx = -1
        def columnDefs = new LinkedHashMap<ColumnDef, Closure>()

        stringColumnDefs.each {
            if (! (it.key instanceof String)) {
                throw new Exception("Column definition must be a String")
            }
            columnDefs.put(new ColumnDef(++lastIdx, it.key, it.value), it.value)
        }
        return columnDefs
    }
}