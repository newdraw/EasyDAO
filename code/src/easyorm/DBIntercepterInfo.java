package easyorm;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.sql.Connection;

/**
 *
 * @author Newdraw
 */
public class DBIntercepterInfo {
    
    public DBUtils dbUtils;
    public Connection connection;
    public String sql;
    public DBVariable[] sqlArgs;
    
    public DBIntercepterInfo()
    {
        
    }
    
    public DBIntercepterInfo(DBUtils dbhelper, Connection conn, String sql, DBVariable... args)
    {
        this.dbUtils = dbhelper;
        this.connection = conn;
        this.sql = sql;
        this.sqlArgs = args;
    }
}
