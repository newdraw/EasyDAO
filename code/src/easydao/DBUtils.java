package easydao;

import easydao.dbinfo.*;
import easydao.utils.*;
import easydao.utils.Ref;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * db访问工具类
 *
 * @author ryu
 */
public class DBUtils {

    //region DBUtils
    /**
     * 默认数据缓存器
     */
    public static Cache defaultCache = new Cache("DBUtils缓存");

    private Connection transactionConnection = null;

    /**
     * sql配置
     */
    public static SQLConfigReader sqlConfig = null;

    /**
     * 公共sql参数
     * 这些参数将在所有sql中生效
     */
    public final static ArrayList<DBVariable> commonVariables = new ArrayList<>();

    public final static ArrayList<Class> registeredInfos =new ArrayList<>(){
        {
            add(MySQLInfo.class);
            add(SQLServerInfo.class);
        }
    };

    public static Action1<DBIntercepterInfo> defaultSqlIntercepter = null;
    public Action1<DBIntercepterInfo> sqlIntercepter = defaultSqlIntercepter;

    public final IDBInfo info;


    public boolean inTransaction() {
        return transactionConnection != null;
    }

    /**
     * 开始事务
     *
     * @throws SQLException
     */
    public void beginTransaction() throws Exception {
        if (transactionConnection != null) {
            throw new SQLException("事务已经开始");
        }

        transactionConnection = createConnection();
        transactionConnection.setAutoCommit(false);
    }

    /**
     * 提交事务
     *
     * @throws SQLException
     */
    public void commitTransaction() throws Exception {
        transactionConnection.commit();
        transactionConnection.setAutoCommit(true);
        transactionConnection.close();
        transactionConnection = null;
    }

    /**
     * 回滚事务
     *
     * @throws SQLException
     */
    public void rollbackTransaction() throws SQLException {
        transactionConnection.rollback();
        transactionConnection.setAutoCommit(true);
        transactionConnection.close();
        transactionConnection = null;
    }

    /*
    连接字符串
     */
    public final String connStr;


    /**
     * @param connStr 连接字符串
     */
    public DBUtils(String connStr) throws Exception {
        connStr = connStr.trim();
        this.connStr = connStr;
        for(var infoType : registeredInfos) {
            var info = (IDBInfo)infoType.getConstructor().newInstance();
            if(info.init(this))
            {
                this.info = info;
                return;
            }
        }

        throw new IllegalArgumentException("不支持的数据库类型");
    }

    static class PoolledConnection
    {
        public Date time;
        public Connection connection;
    }


    static final int poolTimeoutMs = 30000; //池中连接超时时间，必须大于15000
    final static int poolmin = 5;//池最小连接数量，不小于5.
    final static int poolmax = 50;//池最大连接数，不大于db自身的限制
    static class ConnectionPool extends ArrayList<PoolledConnection>
    {
        public int max = poolmin*2;//default size
    }

    static Hashtable<String, ConnectionPool> connPools = new Hashtable<>();
    static Thread poolThread = null;
    static void poolThreadProc() {
        while (true) {
            try {
                //为连接池准备连接
                var now = new Date().getTime();
                for (var connStr : connPools.keySet()) {
                    var pool = connPools.get(connStr);

                    //移除超时连接
                    var count = 0;
                    for (var pc : pool.toArray(new PoolledConnection[0])) {
                        if ((now - pc.time.getTime()) > poolTimeoutMs) {
                            FXUtils.tryInvoke(() -> pc.connection.close());
                            synchronized (pool) {
                                pool.remove(pc);
                            }
                            count++;
                        }
                    }
                    if(count>0 && pool.max > poolmin) {
                        //存在超时的连接说明连接池过大，自动收缩连接池
                        pool.max = Math.max(poolmin, pool.max - count / 2);
                        //easyorm.utils.FXUtils.debug(easyorm.DBUtils.class, "%s的连接池上限收缩到%d。", connStr.split("\\?")[0], pool.max);
                    }

                    //补充池中连接
                    while (pool.size() < pool.max) {

                        var index = new ArrayList<Integer>();
                        for(var i = 0;i<pool.max - pool.size();i++) {
                            index.add(i);
                        }
                        index.parallelStream().forEach(i->{
                            try {
                                var conn = DriverManager.getConnection(connStr);
                                var pc = new PoolledConnection();
                                pc.connection = conn;
                                pc.time = new Date();
                                synchronized (pool) {
                                    pool.add(pc);
                                }
                            }
                            catch(Exception ex)
                            {
                                ex.printStackTrace();
                                //ignore
                            }
                        });

                    }
                }

                //
                Thread.sleep(Math.max(1, poolTimeoutMs - 10000));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 从连接池中获取连接，连接池中没有可用连接则返回null
     * @param connStr
     * @return
     * @throws Exception
     */
    static Connection getPoolConn(String connStr) throws Exception {

        if (poolThread == null) {
            poolThread = new Thread(() -> poolThreadProc());
            poolThread.start();
        }

        var pool = connPools.get(connStr);
        if (pool == null) {
            synchronized (connPools) {
                connPools.put(connStr, new ConnectionPool());
            }
            return null;
        }

        if (pool.size() == 0 && pool.max < poolmax) {
            pool.max = (int) Math.min(poolmax, pool.max * 1.1 + 1);//池中连接用完了， 自动扩大池上限。
            //easyorm.utils.FXUtils.debug(easyorm.DBUtils.class, "%s的连接池上限提升到%d。", connStr.split("\\?")[0], pool.max);
            return null;
        }

        PoolledConnection result;

        synchronized (pool) {
            if (pool.size() > 0) {
                result = pool.remove(0);
            } else {
                return null;
            }
        }

        return result.connection;
    }



    private Connection createConnection() throws Exception {
//        switch(info.type)
//        {
//            case MYSQL:
//                InitialContext ctx = new InitialContext();
//                DataSource ds = (DataSource)ctx.lookup("java:comp/env/jdbc/MySQLDB");
//                return ds.getConnection();
//        }
        var result = getPoolConn(connStr);
        if(result != null) {
            return result;
        }
        return DriverManager.getConnection(connStr);
        //return info.driver.connect(connStr, null);
    }

    private Connection getConnection() throws Exception {
        return transactionConnection != null ? transactionConnection : createConnection();
    }


//    static ScriptEngine script = null;

    private void getRealSql(DBIntercepterInfo info) throws Exception {

        var sql = info.sql.trim();

        //如果是sql配置则加载配置的sql
        if (!sql.contains(" ")) {
            String cfgsql = sqlConfig.get(sql);
            if (!FXUtils.isNullOrEmpty(cfgsql)) {
                sql =  cfgsql;
            }
        }

        //解析sql中的js脚本
        if(sql.contains("{")) {
            sql = sql.replace("`", "\\`");
            sql = String.join("{", Query.from(sql.split("\\{\\{")).select(s -> s.replace("{", "`+(")));
            sql = String.join("}", Query.from(sql.split("\\}\\}")).select(s -> s.replace("}", ")+`")));
            sql = "`" + sql + "`";
            var scriptArgs = new HashMap<String, Object>();
            for(var i : info.sqlArgs)
            {
                scriptArgs.put(i.name, i.getValue(this));
            }
            sql = FXUtils.eval(sql, scriptArgs).toString();
        }

        //
        info.sql = sql;
        if (this.sqlIntercepter != null) {
            try {
                sqlIntercepter.invoke(info);
            } catch (Exception ex) {
                FXUtils.debug(DBUtils.class, "SQLIntercepter出错:" + ex);
                ex.printStackTrace();
                //ignore;
            }
        }

    }

    private class DBVariableInfo {

        public DBVariable variable;
        public ArrayList<Integer> sqlPositions = new ArrayList<>();
        public DBVariableInfo(DBVariable variable) {
            this.variable = variable;
        }
    }

    private Object processArg(Object arg) {
        if (arg instanceof Date) {
            return new Timestamp(((Date) arg).getTime());
        }
        return arg;
    }

//    class Sql
//    {
//        public String sql;
//        public easyorm.DBVariable variables;
//        public easyorm.DBTable table;
//    }
//
//    ArrayList<Sql> SqlQueue = new ArrayList<>();

    private PreparedStatement makeStatement(Connection conn, String sql, Func1<String, String> sqlPreprocessor, Ref<String> realSql, Object... args) throws Exception {

        //准备sql参数
        var vars = new ArrayList<DBVariableInfo>();
        for (DBVariable cv : commonVariables) {
            vars.add(new DBVariableInfo(cv));
        }

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof DBVariable) {
                vars.add(new DBVariableInfo((DBVariable) arg));
            } else {
                vars.add(new DBVariableInfo(new DBVariable("v" + i, arg)));
            }
        }

        //准备sql脚本
        var info = new DBIntercepterInfo(this, conn, sql, Query.from(vars).select(i->i.variable).toArray(DBVariable.class));
        getRealSql(info);

        //预处理sql
        if(sqlPreprocessor!=null)
        {
            info.sql = sqlPreprocessor.invoke(info.sql);
        }

        //将sql转换为jdbc格式 （todo：需要按语义翻译或为?提供转义符，防止错误地替换变量名）
        vars.sort((a, b) -> b.variable.name.length() - a.variable.name.length());

        String sqlFind = info.sql;
        ArrayList<Integer> allVarPos = new ArrayList<>();
        for (DBVariableInfo var : vars) {
            int sqlPos = -1;
            String varName = "?" + var.variable.name;
            while ((sqlPos = sqlFind.indexOf(varName, sqlPos + 1)) != -1) {
                allVarPos.add(sqlPos);
                var.sqlPositions.add(sqlPos);
            }
            sqlFind = sqlFind.replace(varName, padRight("?", ' ', varName.length()));
        }

        allVarPos.sort((a, b) -> a - b);
        realSql.set(sqlFind);

        //创建jdbc对象
        PreparedStatement result = conn.prepareStatement(sqlFind, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);//每个变量只取一次值，防止有些getter和sql exp变量多次取值，值不相同，违反调用者直觉。
        for (DBVariableInfo var : vars) {
            if(var.sqlPositions.isEmpty())
            {
                continue;
            }
            var value = processArg(var.variable.getValue(this));
            for (int pos : var.sqlPositions) {
                result.setObject(allVarPos.indexOf(pos) + 1, value);
            }
        }

        return result;

    }

    private static String padRight(String str, char c, int length) {
        return str + makeString(String.valueOf(c), "", length - str.length());
    }

    private static String makeString(String item, String spliter, int count) {
        if (count == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(item);
            result.append(spliter);
        }
        return result.substring(0, result.length() - spliter.length());
    }

    private static boolean isNumeric(String str) {

        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public DBTable page(int page, int rows, String sql, Object... args) throws Exception {
        return executeTable(0, s->info.wrapPage(s, page, rows), sql, args);
    }

    /**
     * 执行Sql
     *
     * @param sql  sql语句
     * @param args sql参数
     * @throws Exception
     */
    public void execute(String sql, Object... args) throws Exception {
        execute(0, pstmt->pstmt.execute(), null, sql, args);
    }

    public int count(String sql, Object... args) throws Exception {
        return FXUtils.changeType(executeTable(0, s->info.wrapCount(s), sql, args).rows(0).get(0), int.class);
    }

    public DBTable executeTable(String sql, Object... args) throws Exception {
        return executeTable(getDefaultCacheMs(sql), sql, args);
    }

    public <T> T[] executeColumn(Class<T> TColumn, String sql, Object... args) throws Exception {
        ArrayList<T> result = new ArrayList<>();
        DBTable data = executeTable(sql, args);
        for (var row : data.rows()) {
            result.add((T) row.values[0]);
        }

        return result.toArray((T[]) java.lang.reflect.Array.newInstance(TColumn, 0));
    }



    public DBTable executeTable(int cacheTimeoutMs, Func1<String, String> sqlPreprocess, String sql, Object... args) throws Exception {
        return execute(cacheTimeoutMs,
                pstmt -> {
                    try (var rs = pstmt.executeQuery()) {
                        return new DBTable(rs);
                    }
                },
                sqlPreprocess,
                sql,
                args
        );
    }

    /**
     * 执行Sql并返回数据表
     *
     * @param cacheTimeoutMs 允许缓存超时毫秒数
     * @param sql            sql语句
     * @param args           sql参数
     * @return
     * @throws Exception
     */
    public DBTable executeTable(int cacheTimeoutMs, String sql, Object... args) throws Exception {
        return executeTable(cacheTimeoutMs, null, sql, args);
    }

    /**
     *
     * @param cacheTimeoutMs sql结果缓存超时毫秒数，0表示不缓存
     * @param executor 执行器：利用jdbc PreparedStatement执行需要的操作
     * @param sqlPreprocess sql预处理器：在把需要执行的sql语句取出后和翻译成jdbc sql前，预处理sql字符串。
     * @param sql 需要执行的sql语句或sql key（配置在xml中sql的key）
     * @param args sql参数
     * @param <T>
     * @return
     * @throws Exception
     */
    private <T> T execute(int cacheTimeoutMs, Func1<PreparedStatement, T> executor, Func1<String, String> sqlPreprocess, String sql, Object... args) throws Exception {
        if (args.length == 1 && args[0] instanceof ArrayList) {
            args = ((ArrayList) args[0]).toArray();
        }

        //try get from cache
        CompositeKey key = null;

        if (cacheTimeoutMs > 0) {
            ArrayList keyitems = new ArrayList<>();
            keyitems.add(this.connStr);
            keyitems.add(sql);
            for (Object arg : args) {
                keyitems.add(arg);
            }
            key = new CompositeKey(keyitems.toArray());
            var data = new Ref<T>();
            if (defaultCache.tryGet(key, cacheTimeoutMs, data)) {
                FXUtils.debug(DBUtils.class, sql + " found in cache");
                return data.get();
            }
        }

        //
        Connection conn = null;
        T result = null;
        var realSql = new Ref<String>();
        try {
            conn = getConnection();
            long start = new Date().getTime();
            try(var pstmt = makeStatement(conn, sql, sqlPreprocess, realSql, args)) {
                result = executor.invoke(pstmt);
            }
            long end = new Date().getTime();
            FXUtils.debug(DBUtils.class, realSql.get()  + " sql time:" + (end - start) + "ms");
        } catch (Exception ex) {
            System.out.println(realSql.get());
            throw ex;
        } finally {
            if (conn != null && !this.inTransaction() && !conn.isClosed()) {
                conn.close();
            }
        }

        //do cache
        if (key != null) {
            defaultCache.put(key, result);
            FXUtils.debug(DBUtils.class, "SQL请求缓存%dms：%s", cacheTimeoutMs, sql);
        }

        return result;

    }

    public int getDefaultCacheMs(String sqlKey) throws Exception {

        if (sqlKey.contains(" ")) {
            return 0;
        }

        String cache = sqlConfig.getAttr(sqlKey, "cache");
        if (FXUtils.isNullOrEmpty(cache)) {
            return 0;
        }

        if (isNumeric(cache)) {
            return Integer.valueOf(cache);
        }

        cache = sqlConfig.get(cache);
        if (cache == null) {
            throw new SQLException("没有找到缓存时间配置：" + cache);
        }
        return Integer.valueOf(cache);

    }

    public <T> T executeValue(String sql, Object... args) throws Exception {

        return executeValue(getDefaultCacheMs(sql), sql, args);
    }

    /**
     * 执行Sql并返回首行首列
     *
     * @param sql  sql语句
     * @param args sql参数
     * @return
     * @throws Exception
     */
    public <T> T executeValue(int cacheTimeoutMs, String sql, Object... args) throws Exception {
        DBTable data = executeTable(cacheTimeoutMs, sql, args);
        if (data.rows().length > 0) {
            return (T) data.rows(0).get(0);
        }
        return null;
    }

    public boolean exists(String sql, Object... args) throws Exception {
        return count(sql, args) > 0;
    }

    //endregion

    //region EntityUtils

    /**
     * 默认的主键字段
     */
    public static String[] defaultKeyFields = null;
    /**
     * 默认的自动生成值（自增）的主键字段
     */
    public static String defaultAutoKeyField = null;


    public static <T> T makeEntity(Class<T> tEntity, DBRow row) throws Exception {
        T result = tEntity.getConstructor().newInstance();
        for (Field f : tEntity.getFields()) {
            var col = row.table.getColumn(f.getName());
            if (col != null) {
                FXUtils.setMember(result, f, row.get(col), true);
                //f.set(result, easyorm.utils.FXUtils.changeType(row.get(col), f.g));
            }
        }
        return result;
    }

    public static <T> T[] makeEntities(Class<T> tEntity, DBTable table) throws Exception
    {
        var result = (T[])java.lang.reflect.Array.newInstance(tEntity, table.rows().length);
        for(var i = 0; i < table.rows().length; i++)
        {
            result[i] = makeEntity(tEntity, table.rows(i));
        }
        return result;
    }

    public <T> void delete(T entity) throws Exception {
        delete(entity, defaultKeyFields);
    }

    public <T> void delete(T entity, String... keyFields) throws Exception {
        if (keyFields == null || keyFields.length == 0) {
            throw new Exception("至少需要一个key field");
        }
        var fields = getEntityFields(entity);
        Class type = entity.getClass();

        StringBuilder sql = new StringBuilder();
        String table = type.getSimpleName();
        sql.append(String.format("delete from %s where ", info.wrapTableName(table)));
        ArrayList<Object> args = new ArrayList<>();

        for (String k : keyFields) {
            Field tf = fields.get(k);
            if (tf == null) {
                throw new Exception("没有找到主键字段" + k);
            }
            sql.append(String.format("%s=?v%s and", info.wrapFieldName(k), args.size()));
            args.add(tf.get(entity));
        }
        sql = sql.delete(sql.length() - 3, sql.length());


        execute(sql.toString(), args.toArray());
    }

    public <T> T first(Class<T> TEntity, String sql, Object... args) throws Exception {
        return first(TEntity, 0, sql, args);
    }

    public <T> T first(Class<T> TEntity, int cacheTimeoutMs, String sql, Object... args) throws Exception {
        T[] items = get(TEntity, cacheTimeoutMs, sql, args);
        if (items.length == 0) {
            return null;
        }
        return items[0];
    }

    public <T> T[] get(Class<T> TEntity, String sql, Object... args) throws Exception {
        return get(TEntity, 0, sql, args);
    }

    public <T> T[] get(Class<T> TEntity, int cacheTimeoutMs, String sql, Object... args) throws Exception {
        return makeEntities(TEntity, executeTable(cacheTimeoutMs, sql, args));
    }


    private static <T> TreeMap<String, Field> getEntityFields(T entity) throws Exception {
        //todo:cache
        if (entity == null) {
            throw new Exception("参数entity不能为null");
        }
        Class type = entity.getClass();
        TreeMap<String, Field> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Field f : type.getFields()) {
            fields.put(f.getName(), f);
        }
        return fields;
    }

    /**
     * 插入实体对象
     *
     * @param entity       实体对象
     * @param keyFields    对象唯一键（主键）字段。如果没有autoFields，该参数也可以不指定。
     * @param autoKeyField 自动键字段。相应java字段值将被忽略，框架会从db获取值覆盖java字段值。
     * @param autoFields   数据库自动填值的字段。相应java字段值将被忽略，框架会从db获取值覆盖java字段值。
     * @param <T>
     * @throws Exception
     */
    public <T> void insert(T entity, String[] keyFields, String autoKeyField, String[] autoFields) throws Exception {

        var fields = getEntityFields(entity);
        var type = entity.getClass();

        var table = type.getSimpleName();
        var sqlCols = new StringBuilder();
        var sqlValues = new StringBuilder();
        var args = new ArrayList<>();

        var cols = info.getDBFields(table);
        //ArrayList<easyorm.DBField> dbkeys = new ArrayList<>();
        for (DBField dbf : cols) {
            if (FXUtils.contains(autoFields, dbf.name, true) || dbf.name.equalsIgnoreCase(autoKeyField)) {
                continue;
            }
            Field field = fields.get(dbf.name);// type.getField(dbf.name);
            if (field == null) {
                continue;
            }
            sqlCols.append(String.format("%s,", info.wrapFieldName(dbf.name)));
            sqlValues.append(String.format("?v%s,", args.size()));
            args.add(field.get(entity));
        }
        if (args.isEmpty()) {
            throw new Exception("没有找到需要插入的字段。");
        }
        sqlCols = sqlCols.deleteCharAt(sqlCols.length() - 1);
        sqlValues = sqlValues.deleteCharAt(sqlValues.length() - 1);

        String sql = String.format("insert into %s (%s) values(%s)", info.wrapTableName(table), sqlCols, sqlValues);
        if (FXUtils.isNullOrEmpty(autoKeyField) && FXUtils.isNullOrEmpty(autoFields)) {
            execute(sql, args.toArray());
            return;
        }

        //回填自动键
        if (!FXUtils.isNullOrEmpty(autoKeyField)) {
            info.appendSql(sql, info.getLastId());
            FXUtils.setMember(entity, autoKeyField, executeValue(sql, args.toArray()), true);
        }

        //回填自动字段
        if (!FXUtils.isNullOrEmpty(autoFields)) {
            if (FXUtils.isNullOrEmpty(keyFields)) {
                throw new Exception("没有key字段找回忽略字段的值。");
            }
            StringBuilder sqlWhere = new StringBuilder();
            args.clear();
            for (String key : keyFields) {
                sqlWhere.append(String.format("%s=?v%s and", info.wrapFieldName(key), args.size()));
                args.add(fields.get(key).get(entity));
            }
            sqlWhere = sqlWhere.delete(sqlWhere.length() - 3, sqlWhere.length());

            T newEntity = (T) first(type, String.format("select * from %s where %s", info.wrapTableName(table), sqlWhere), args.toArray());
            for (String sf : autoFields) {
                FXUtils.setMember(entity, sf, FXUtils.getMember(newEntity, sf), false);
            }
        }
    }

    public <T> void insert(T entity) throws Exception {
        insert(entity, defaultKeyFields, defaultAutoKeyField, null);
    }

    public <T> void update(T entity) throws Exception {
        update(entity, defaultKeyFields);
    }

    public <T> void update(T entity, String... keyFields) throws Exception {
        var fields = getEntityFields(entity);
        Class type = entity.getClass();

        StringBuilder sql = new StringBuilder();
        String table = type.getSimpleName();
        sql.append(String.format("update %s set ", info.wrapTableName(table)));
        ArrayList<Object> args = new ArrayList<>();

        //set token
        DBField[] cols = info.getDBFields(table);
        ArrayList<DBField> dbkeys = new ArrayList<>();
        for (DBField dbf : cols) {
            if (FXUtils.contains(keyFields, dbf.name, true)) {
                dbkeys.add(dbf);
                continue;
            }
            Field field = fields.get(dbf.name);// type.getField(dbf.name);
            if (field == null) {
                continue;
            }
            sql.append(String.format("%s=?v%s,", info.wrapFieldName(dbf.name), args.size()));
            args.add(field.get(entity));
        }
        if (args.isEmpty()) {
            throw new Exception("没有找到需要更新的字段。");
        }
        sql = sql.deleteCharAt(sql.length() - 1);

        //where token
        if (dbkeys.size() != keyFields.length) {
            throw new Exception("db中没有对应的key");
        }
        if (keyFields.length > 0) {
            sql.append(" where");
            for (DBField dbk : dbkeys) {
                Field tf = fields.get(dbk.name);
                if (tf == null) {
                    throw new Exception("没有找到主键字段" + dbk.name);
                }
                sql.append(String.format(" %s=?v%s and", info.wrapFieldName(dbk.name), args.size()));
                args.add(tf.get(entity));
            }
            sql = sql.delete(sql.length() - 3, sql.length());
        }

        execute(sql.toString(), args.toArray());

    }

    /**
     * 生成Entity类代码文件
     *
     * @param path      entity代码目录
     * @param package_  代码包名
     * @param clearPath 生成前是否清空目录中所有文件
     * @throws Exception
     */
    public void makeEntityCodeFiles(String path, String package_, boolean clearPath) throws Exception {
        File p = new File(path);
        if (clearPath) {
            p.delete();
            p.mkdir();
        }

        String[] tables = info.getDBTables();

        for (String table : tables) {
            StringBuilder result = new StringBuilder();
            result.append(String.format("package %s;\n\n", package_));
            result.append(String.format("public class %s{\n", table));
            for (DBField field : info.getDBFields(table)) {
                result.append(String.format("\tpublic %s %s;\n", field.type.getTypeName(), field.name));
            }
            result.append(String.format("}\n", table));
            File f = Paths.get(path, table + ".java").toFile();
            f.createNewFile();
            try (FileOutputStream fs = new FileOutputStream(f)) {
                fs.write(result.toString().getBytes());
            }
        }

    }
    //endregion
}
