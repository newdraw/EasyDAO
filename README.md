# EasyORM
简单好用的数据访问类。 easy orm dao

## 基本用法
```
var db = new DBUtils("jdbc:mysql://localhost:3306/mall?User=root&Password=root");
var rows = db.executeValue("select count(*) from table");
```

## 访问数据表
