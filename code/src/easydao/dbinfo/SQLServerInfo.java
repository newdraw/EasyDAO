package easydao.dbinfo;

import easydao.DBField;
import easydao.DBTable;
import easydao.DBUtils;

import java.math.BigDecimal;
import java.sql.Driver;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;

public class SQLServerInfo implements IDBInfo {

    Driver driver;
    DBUtils db;

    @Override
    public Driver getDriver() {
        return driver;
    }

    @Override
    public boolean init(DBUtils db) throws Exception {
        if (!db.connStr.startsWith("jdbc:sqlserver:")) {
            return false;
        }
        this.db = db;
        driver = (Driver)Class.forName("com.mysql.jdbc.Driver").getConstructor().newInstance();
        return true;
    }

    @Override
    public String wrapTableName(String tablename)  {
        return "[" + tablename + "]";
    }

    @Override
    public String wrapFieldName(String fieldname) {
        return "[" + fieldname + "]";
    }

    @Override
    public String[] getDBTables() throws Exception {
        return db.executeColumn(String.class, "SELECT name FROM Sys.Tables");
    }

    @Override
    public DBField[] getDBFields(String table) throws Exception {
        final int cachems = 1000 * 60 * 10;
        var result = new ArrayList<>();
        DBTable data = db.executeTable(cachems, "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?v0", table);
        for(var row : data.rows()) {
            Class type;
            var stype = (String) row.get("DATA_TYPE");
            var col = (String) row.get("column_name");
            switch (stype) {
                case "varchar":
                case "nvarchar":
                    type = String.class;
                    break;
                case "date":
                case "datetime":
                    type = Date.class;
                    break;
                case "time":
                    type = Time.class;
                    break;
                case "bit":
                    type = Boolean.class;
                    break;
                case "int":
                    type = Integer.class;
                    break;
                case "bigint":
                    type = Long.class;
                    break;
                case "float":
                    type = Float.class;
                    break;
                case "double":
                    type = Double.class;
                    break;
                case "decimal":
                    type = BigDecimal.class;
                    break;
                case "image":
                    type = byte[].class;
                    break;
                default:
                    throw new Exception("未处理的类型" + stype + "@" + table + "." + col);
            }
            result.add(new DBField(col, type));
        }
        return result.toArray(new DBField[0]);
    }

    @Override
    public String wrapPage(String sql, int page, int rows)  {
        if (rows == 0) {
            var orderby = sql.toLowerCase().lastIndexOf("order by ");
            if(orderby >= 0) {
                sql = sql.substring(0, orderby - 1);
            }
            return String.format("select top 0 * from (%s) t ", sql);
        }
        return String.format("%s offset %d rows fetch next %d rows only", sql, page * rows, rows);

    }

    @Override
    public String wrapCount(String sql)  {
        var orderby = sql.toLowerCase().lastIndexOf("order by ");
        if(orderby >= 0) {
            sql = sql.substring(0, orderby - 1);
        }
        return String.format("select count(*) from (%s) t ", sql);
    }

    @Override
    public String getLastId() {
        return "SELECT SCOPE_IDENTITY()";
    }

    @Override
    public String appendSql(String sql, String sql2) {
        return sql + ";" + sql2;
    }
}
