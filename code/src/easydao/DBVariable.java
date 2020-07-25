package easydao;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import easydao.utils.*;

/**
 * 数据库变量
 * @author ryu
 */
public class DBVariable
{
    /**
     * 变量名
     */
    public String name;

    /**
     * 变量类型
     */
    private DBVariableType type;
    private Object value;
    private Func valueGetter;
    private String sqlExp;
//    boolean valueCached = false; //valueCache 造成currentUserId等公共变量或valueGetter变化不能正确反映


    public Object getValue(DBUtils db) throws Exception {
//        if (valueCached) {
//            return value;
//        }

        switch (type) {
            case TYPE_VALUE_GETTER:
                return valueGetter.invoke();
//                break;
            case TYPE_VALUE_SQLEXP:
                if (db == null) {
                    throw new IllegalArgumentException("请传入DB访问对象");
                }
                return db.executeValue(sqlExp);
//                break;
            case TYPE_FIXED_VALUE:
                return value;
            default:
                throw new Exception("暂不支持的DB参数");
        }

//        valueCached = true;
//        return value;
    }
    
    public DBVariable(String name, Object value)
    {
        this.name = name;
        this.value = value;
        this.type = DBVariableType.TYPE_FIXED_VALUE;
//        valueCached = true;
    }

    public DBVariable(String name, Func valueGetter)
    {
        this.name = name;
        this.valueGetter = valueGetter;
        this.type = DBVariableType.TYPE_VALUE_GETTER;
    }

    @Override 
    public String toString()
    {
        try { 
            return this.name + ": " + this.getValue(null);
        } catch (Exception ex) {
            return "";
        }
    }
}
