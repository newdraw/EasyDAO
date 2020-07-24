package easyorm;

/**
 * 数据列
 */
public class DBColumn
{
    /**
     * 列名
     */
    public String name;
    /**
     * 列索引号
     */
    public int index;
    /**
     * 数据表
     */
    public DBTable table;
    public DBColumn(DBTable table, String name, int index)
    {
        this.table = table;
        this.name = name;
        this.index = index;
    }
}
