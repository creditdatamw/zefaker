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
}