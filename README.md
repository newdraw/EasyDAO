# EasyORM
简单好用的数据访问类。 

## 上手
```Java
var db = new DBUtils("jdbc:mysql://localhost:3306/mall?User=root&Password=root");
var rows = db.executeValue("select 'Hello World'");
```

## 访问表中单元格
```Java
var table = db.executeTable("select count(*) from table");
var firstCell = table.rows(0).get(0);
```

## 遍历表
```Java
var table = db.executeTable("select count(*) from table");
for(var row : table.rows()) {
    for (var col : table.columns()) {
        System.out.println(row.get(col));
    }
}
```

## 访问实体类
```Java
class TableEntity{
    public int id;
    public String value;
}
var entities = db.get(TableEntity.class, "select id, value from table");
```

## 
