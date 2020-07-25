# EasyDAO
好上手的数据访问类， 无配置，零依赖。

提供可泛用的接口，减少编码量。

# 上手
### Hello World
```Java
var db = new DBUtils("jdbc:mysql://localhost:3306/school?User=root&Password=root");
System.out.println((String)db.executeValue("select 'Hello World'"));
```

### 访问表中数据
```Java
var table = db.executeTable("select * from student");
var firstRow = table.rows(0);
var studentName = firstRow.get("name");
```

### 增删改 & SQL参数
```Java
db.execute("delete from student where name = ?v0", "张三");
db.execute("insert into student(id, name) values(?v0, ?v1)", 1, "张三");
db.execute("update student set name = ?v1 where id = ?v0", 1, "李四");
```
SQL参数通过索引号传入。

### 查询映射到实体
```Java
class student {
    public int id;
    public String name;
}
var students = db.get(student.class, "select id, name from student");
```
字段自动映射到对象同名成员，字段和成员不必须一一对应，根据实际需要，多了少了都没有关系。

### 通过实体类增删改
```Java
var student = new student();
student.id = 1;
student.name = "张三";
db.insert(entity);  //插入

student.name = "李四";
db.update(student);  //更新

db.delete(student);  //删除
```

### 事务
```Java
db.beginTransaction();
try {
    db.insert(new student(){{id=0; name="张三";}});
    db.insert(new student(){{id=1; name="李四";}});
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
SQL配置支持所有执行SQL的方法，支持多个配置文件，支持在运行时修改配置内容。


### Join查询映射到实体
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
var cache = db.executeTable(1000*60, "select * from student"); 
//数据将被缓存6000ms，在超时前再次执行SQL，将返回内存中的缓存数据。
```
缓存支持所有执行SQL的方法，支持配置缓存配额，不满足配额时缓存自动释放。

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
比stream更接近SQL关键字。

```
//将文件夹中所有xml文件加载为SQL配置
var sqlFolder = new File("/home/sqlFolder/");
DBUtils.sqlConfig = new SQLConfigReader(
    Query.from(sqlFolder.listFiles())
        .where(i->i.getName().toLowerCase().endsWith(".xml"))
        .select(i->i.getPath())
        .toArray(String.class)
);
```

### 自增主键 & 计算列
```Java
DBUtils.defaultAutoKeyField = "id"; //id是自增主键
var student = new student(); 
db.insert(student);
System.out.println(student.id); 
```
自增主键的值自动回填到实体对象。

```Java
var student = new student();
student.id = 1; 
student.birthday = birthday;
db.insert(student, new String[]{"id"}, null, new String[]{"age"}); //age是计算列
System.out.println(student.age);
```
计算列的值自动回填到实体对象。


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
当SQL参数非常多时，命名参数比匿名的索引号参数更清晰。

全局参数在所有SQL中有效，一般用于频繁需要传入的参数。

SQL参数不一定必须被SQL语句使用，因此传多了不要紧。

### 动态SQL参数
```Java
var students = db.get(
    student.class, 
    "select * from student where id = ?userid",
    new DBVariable("userid", ()->session.get("currentUserId"))
);
```
动态SQL一般用于参数值会变化的情况，比如时间有关的参数、递增的参数、Session中的参数等，可以结合全局SQL参数使用。

### SQL拼接
```Java
var students = db.get(
    student.class,
    "select * from student where id in ({v0})",
    String.join(",", ids)
);
```
SQL里的大括号支持javascript语法，因此可以做很多事，比如下面的代码...
```Java
var students = db.get(
    student.class,
    "select * from student where {v0!=''?'id in (' + v0 + ')':'1=1'}",
    String.join(",", ids)
);
```

### 生成实体类
```Java
db.makeEntityCodeFiles("/home/entities/", "myproject.entity", false);
```
执行后，程序自动根据DB结构生成所有实体类java文件，可以配置生成目录到项目目录。生成的java文件内容如下：
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
SQL只在名称匹配forComputer的计算机上有效，因此即使同名也不冲突。

### 把数据转成习惯的对象
```Java
var table = db.executeTable("select * from student");
List<Map<String, Object>> data = table.toList(); //转为Map
var entities = DBUtils.makeEntities(student.class, table); //转为实体对象
```

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
默认已支持MySQL和SQLServer
