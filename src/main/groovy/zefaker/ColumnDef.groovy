package zefaker

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