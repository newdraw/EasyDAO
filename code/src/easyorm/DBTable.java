package easyorm;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;


/**
 * 数据表
 */
public class DBTable {

    private Map<String, DBColumn> columnMap = null;
    /**
     * 表格包含的所有列
     */
    private final DBColumn[] columns;
    /**
     * 表格包含的所有行
     */
    private final DBRow[] rows;

    public DBRow rows(int index)
    {
        return rows[index];
    }

    public DBRow[] rows()
    {
        return rows;
    }

    public DBColumn columns(int index)
    {
        return columns[index];
    }

    public DBColumn[] columns()
    {
        return columns;
    }

    /**
     * 用列名获得列
     * @param columnName
     * @return
     */
    public DBColumn getColumn(String columnName)
    {
        return columnMap.get(columnName);
    }

    /**
     * 将表转为Map List形式。
     * @return
     */
    public List<Map<String, Object>> toList() {
        var result = new ArrayList<Map<String, Object>>();
        for (var row : rows) {
            var item = new HashMap<String, Object>();
            for (var col : columns) {
                item.put(col.name, row.get(col));
            }
            result.add(item);
        }
        return result;
    }

//    /**
//     * 将表转为JSONArray
//     * @return
//     */
//    public JSONArray toJson() throws Exception {
//        var result = new JSONArray();
//        for (var row : rows) {
//            var item = new JSONObject(true);
//            for (var col : columns) { 
//                item.put(col.name, easyorm.utils.FXUtils.jsonValueConvert(row.get(col)));
//            }
//            result.add(item);
//        }
//        return result;
//    }


    public DBTable(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();

        int cols = md.getColumnCount();

        //init metadata
        columns = new DBColumn[cols];
        columnMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < cols; i++) {
            String name = md.getColumnLabel(i + 1);
            columns[i] = new DBColumn(this, name, i);
            columnMap.put(name, columns[i]);
        }

        //init table
        var rows = new ArrayList<DBRow>();
        while (rs.next()) {
            var row = new Object[cols];
            for (int i = 0; i < cols; i++) {
                row[i] = rs.getObject(i + 1);
            }
            rows.add(new DBRow(this, row));
        }

        this.rows = rows.toArray(new DBRow[0]);
    }

}
