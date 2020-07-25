package easydao.dbinfo;

import easydao.DBField;
import easydao.DBUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Date;

public class MySQLInfo implements IDBInfo {

    Driver driver;
    DBUtils db;

    @Override
    public Driver getDriver() {
        return driver;
    }

    @Override
    public boolean init(DBUtils db) throws Exception {
        if (!db.connStr.startsWith("jdbc:mysql:")) {
            return false;
        }
        this.db = db;
        driver = (Driver) Class.forName("com.mysql.jdbc.Driver").getConstructor().newInstance();
        return true;
    }

    @Override
    public String wrapTableName(String tablename)  {
        return "`" + tablename + "`";
    }

    @Override
    public String wrapFieldName(String fieldname) {
        return "`" + fieldname + "`";
    }

    @Override
    public String[] getDBTables() throws Exception {
        var dbname = new URI(db.connStr.substring(db.connStr.indexOf(":") + 1)).getPath().substring(1);
        return db.executeColumn(String.class, "SELECT table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema = ?v0", dbname);

    }

    @Override
    public DBField[] getDBFields(String table) throws Exception {
        final int cachems = 1000 * 60 * 10;
        var result = new ArrayList<>();
        var dbname = new URI(db.connStr.substring(db.connStr.indexOf(":") + 1)).getPath().substring(1);
        var data = db.executeTable(cachems, "select COLUMN_NAME, DATA_TYPE from information_schema.COLUMNS where table_name = ?v0 and table_schema = ?v1", table, dbname);
        for(var row : data.rows())
        {
            Class type;
            String stype =(String)row.get("DATA_TYPE");
            String col = (String)row.get("COLUMN_NAME");
            int p = stype.indexOf("(") ;
            if(p != -1)
            {
                stype = stype.substring(0, p);
            }
            switch(stype)
            {
                case "varchar":
                case "json":
                case "longtext":
                    type = String.class;
                    break;
                case "date":
                case "datetime":
                    type = Date.class;
                    break;
                case "bigint":
                    type = BigInteger.class;
                    break;
                case "int":
                    type = Integer.class;
                    break;
                case "decimal":
                    type = BigDecimal.class;
                    break;
                case "float":
                    type = Float.class;
                    break;
                case "bit":
                    type = Boolean.class;
                    break;
                default:
                    throw new Exception("未处理的类型" + stype + "@" + table + "." + col);
            }
            result.add(new DBField(col,type));
        }
        return result.toArray(new DBField[0]);
    }

    @Override
    public String wrapPage(String sql, int page, int rows)  {
        return String.format("select * from (%s) t limit %d, %d", sql, page * rows, rows);
    }

    @Override
    public String wrapCount(String sql)  {
        return String.format("select count(*) from (%s) t ", sql);
    }

    @Override
    public String getLastId() {
        return "SELECT LAST_INSERT_ID()";
    }

    @Override
    public String appendSql(String sql, String sql2) {
        return sql + ";" + sql2;
    }
}
