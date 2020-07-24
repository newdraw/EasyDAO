# EasyORM
好上手的数据访问类。 

提供可泛用的接口，减少编码量。

# 上手
### Hello World
```Java
var db = new DBUtils("jdbc:mysql://localhost:3306/mall?User=root&Password=root");
var value = db.executeTable("select 'Hello World'");
```

### 访问表中数据
```Java
var table = db.executeTable("select * from student");
var firstRow = table.rows(0);
var name = firstRow.get("name");
```

### 增删改 & SQL参数
```Java
db.execute("delete from student where name = ?v0", "张三");
db.execute("insert into student(id, name) values(?v0, ?v1)", 1, "张三");
db.execute("update student set name = ?v1 where id = ?v0", 1, "李四");
```

### 查询实体类
```Java
class student {
    public int id;
    public String name;
}
var students = db.get(student.class, "select id, value from student");
```

### 通过实体类增删改
```Java
var entity = new table();
entity.id = 1;
entity.value = "defaultValue";
db.insert(entity);  //插入

entity.value = "newValue";
db.update(entity);  //更新

db.delete(entity);  //删除
```

### 事务
```Java
db.beginTransaction();
try {
    db.insert(new table());
    db.insert(new table());
    db.commitTransaction();
}
catch(Exception ex) {
    db.rollbackTransaction();
}
```
事务支持所有执行SQL的方法

### SQL配置
```XML
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <item key="getStudents"> 
        select * from student 
    </item>
</root>    
```

```Java
DBUtils.sqlConfig = new SQLConfigReader("/home/sqlconfig.xml");
var students = db.executeTable("getStudents");
```
SQL配置支持所有执行SQL的方法

### Join数据实体
```XML
<item key="getStudents">
	select
	    student.name studentName,
	    class.name className
	from student
	join class on class.id = student.classId 
</item>
```

```Java
class joinData {
    public String studentName;
    public String className;
}
var data = db.get(joinData.class, "getStudents");
```


# 进阶
### 缓存
```Java
var cache = db.executeTable(1000*60, "select * from table"); //数据将被缓存6000ms，在超时前再次执行SQL，将返回内存中的缓存数据。
```
缓存支持所有执行SQL的方法

### 拦截器
```Java
db.sqlIntercepter = info->{
    System.out.println("执行了SQL：" + info.sql);
};
```

### 在内存中查询
```Java
var students = db.get(student.class, "select * from student");
var names = Query.from(students)
            .where(student->student.name.startsWith("张"))
            .orderBy(student->student.name)
            .select(student->student.name);
```

### 命名SQL参数 & 全局SQL参数
```Java
var students = db.get(
    student.class,
    "select * from student where name like concat(?lastname, '%')", 
    new DBVariable("lastname", "张")
);

DBUtils.commonVariables.add(new DBVariable("zero", 0));
var students = db.get(student.class, "select * from student where id in (?zero, ?v0)", 1);
```
SQL参数不一定必须被使用，因此多传了不要紧。

### 动态SQL参数
```Java
var students = db.get(
    student.class, 
    "select * from student where id = ?userid",
    new DBVariable("userid", ()->session.get("currentUserId"))
);
```
一般用于参数值会变化的情况，可以结合全局SQL参数，传入常用SQL参数，减少代码量。

### SQL拼接
```Java
var students = db.get(
        student.class,
        "select * from student where id in ({v0})",
        String.join(",", ids)
);
```
SQL字符串的大括号里支持javascript语法，因此可以做很多事...

### 自动生成实体类
```Java
db.makeEntityCodeFiles("/home/entities/", "myproject.entity", false);
```
执行后，程序自动根据DB结构生成所有实体类文件，可以配置到项目的实体类目录。生成的实体类如下：
```Java
package myproject.entity;

public class student{
	public java.lang.Integer id;
	public java.lang.String name;
}
```

### SQL配置缓存时间
```XML
<item key="CacheTime" remark="Demo缓存毫秒数">60000</item>
<item key="getStudents" cache="CacheTime">
    select * from student
</item>
```

### SQL配置适用计算机
```XML
<item key="getStudents" forComputer="serverComputerName">
    select * from student
</item>

<item key="getStudents" forComputer="debugComputerName">
    select * from student limit 100
</item>
```
SQL只在名称匹配forComputer的计算机上有效

### 更多工具方法
```Java
db.execute("delete from student"); //不关心返回结果
db.executeTable("select * from student");//返回表
db.executeValue("select count(*) from student"); //返回第一个单元格
db.executeColumn(String.class, "select name from students"); //返回第一列

db.get(student.class, "select * from student");//返回所有实体对象
db.first(student.class, "select * from student where id = 1");//返回第一个实体对象
```

### 添加数据库支持
```Java
public class OracleInfo implements IDBInfo {

    @Override
    public boolean init(DBUtils dbUtils) throws Exception {
        //...
    } 

    //...
}
```

```Java
DBUtils.registeredInfos.add(OracleInfo.class);
var db = new DBUtils("jdbc:oracle:...");
```
目前已支持MySQL和SQLServer
