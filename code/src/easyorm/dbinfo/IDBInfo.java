package easyorm.dbinfo;

import easyorm.DBField;
import easyorm.DBUtils;

import java.sql.Driver;

public interface IDBInfo {

    Driver getDriver();
    boolean init(DBUtils db) throws Exception ;
    String wrapTableName(String tablename) ;
    String wrapFieldName(String fieldname) ;
    String[] getDBTables() throws Exception;
    DBField[] getDBFields(String table) throws Exception;
    String wrapPage(String sql, int page, int rows) ;
    String wrapCount(String sql);
    String getLastId();
    String appendSql(String sql, String sql2);
}