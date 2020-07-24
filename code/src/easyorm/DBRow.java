package easyorm;

import easyorm.utils.*;

import java.util.HashMap;
import java.util.Map;

public class DBRow
{
    public Object[] values;
    public DBTable table;
    public DBRow(DBTable table, Object[] values)
    {
        this.table = table;
        this.values = values;
    }

    public Object get(int index)
    {
        return values[index];
    }

    public Object get(String columnName)
    {
        return values[table.getColumn(columnName).index];
    }

    public Object get(DBColumn col)
    {
        return values[col.index];
    }

    public <T> T get(Class<T> type, String columnName) throws Exception {
        return FXUtils.changeType(get(columnName), type);
    }

    public <T> T get(Class<T> type, int index) throws Exception {
        return FXUtils.changeType(get(index), type);
    }

    public <T> T get(Class<T> type, DBColumn col) throws Exception {
        return FXUtils.changeType(get(col), type);
    }

    public Map<String, Object> toMap() {
        var result = new HashMap<String, Object>();
        for(var c : table.columns())
        {
            result.put(c.name, get(c.index));
        }
        return result;
    }
}
