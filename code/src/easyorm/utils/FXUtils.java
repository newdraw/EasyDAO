package easyorm.utils;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 框架工具类
 * @author ryu
 */
public class FXUtils
{


    private static HashMap<Class, Class> boxedTypes = new HashMap<>(){
        {put(Integer.class, int.class);}
        {put(Byte.class, byte.class);}
        {put(Short.class, short.class);}
        {put(Long.class, long.class);}
        {put(Float.class, float.class);}
        {put(Double.class, double.class);}
        {put(Character.class, char.class);}
        {put(Boolean.class, boolean.class);}
    };

    private static HashMap<Class, Class> primitiveTypes = new HashMap<Class, Class>(){
        {put(int.class, Integer.class);}
        {put(byte.class,Byte.class  );}
        {put(short.class, Short.class );}
        {put(long.class, Long.class);}
        {put(float.class, Float.class );}
        {put(double.class, Double.class );}
        {put(char.class, Character.class);}
        {put( boolean.class, Boolean.class);}
    };

    /**
     * 自动类型转换。
     * 尝试各种方式，尽可能将对象转换为需要的类型。
     * @param src
     * @param type
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> T changeType(Object src, Class<T> type) throws Exception {

        if (src == null) {
            return null;
        }

        if(type == Object.class)
        {
            return (T)src;
        }

        Class srcType = src.getClass();
        if(srcType == type)
        {
            return (T)src;
        }

        Class srcPrimitiveType = null;

        //自动拆箱
        if(!srcType.isPrimitive())
        {
            srcPrimitiveType = boxedTypes.get(srcType);
            if(srcPrimitiveType == type)
            {
                return (T)src;
            }
        }

        //自动装箱
        if(srcType.isPrimitive() && !type.isPrimitive())
        {
            if(primitiveTypes.get(srcType) == type)
            {
                return (T)src;
            }
        }

        //子类可以强转
        if (srcType.isAssignableFrom(type)) {
            return (T) src;
        }

        //尝试将JSONArray等集合转换成需要的数组
//        if (type.isArray()) {
//            List list = null;
//            if (srcType.isAssignableFrom(ArrayList.class)) {
//                list = (List) src;
//            } else if (src instanceof JSONArray) {
//                list = ((JSONArray) src).toJavaList(Object.class);
//            }
//
//            if (list != null) {
//                Class<?> ctype = type.getComponentType();
//                Object result = Array.newInstance(ctype, list.size());
//                for (int i = 0; i < list.size(); i++) {
//                    Array.set(result, i, changeType(list.get(i), ctype));
//                }
//                return (T) result;
//            }
//        }

        //尝试util.Date、sql.Date、Timestamp之间的转换
        if (type == Date.class) {
            if (src instanceof Timestamp) {
                return (T) new Date(((Timestamp) src).getTime());
            }

            if (src instanceof java.sql.Date) {
                return (T) new Date(((java.sql.Date) src).getTime());
            }
        }

        //尝试将JSONObject转换成java对象
//        if (src instanceof JSONObject) {
//            JSONObject jo = (JSONObject) src;
//            Object result = type.getDeclaredConstructor().newInstance();
//            for (String key : jo.keySet()) {
//                try
//                {
//                    setMember(result, key, jo.get(key), true);
//                }
//                catch(Exception ex)
//                {
//                    System.out.println(String.format("由于Java对象缺少与JSON匹配的成员，数据发生丢失：%s.%s" , type.getSimpleName(), key));
//                }
//            }
//            return (T) result;
//        }

        //尝试使用valueOf函数
        try
        {
            var boxedType = type;
            if(boxedType.isPrimitive()) {
                boxedType = primitiveTypes.get(boxedType);
            }
            Method m = boxedType.getMethod("valueOf", srcPrimitiveType==null? srcType:srcPrimitiveType);
            return (T)m.invoke(null, src);
        }
        catch(NoSuchMethodException ex)
        {
            //ignore
        }

        //使用toString转为String
        if (type == String.class) {
            return (T) src.toString();
        }

        //尝试构造函数
        try {
            return type.getConstructor(srcType).newInstance(src);
        }
        catch(Exception ex)
        {
            //ignore
        }


        //处理特定的数据类型
        if(srcPrimitiveType == int.class && type == BigInteger.class)
        {
            return (T)BigInteger.valueOf((long)(int)src);
        }

        if(srcPrimitiveType == int.class && (type == byte.class || type == Byte.class))
        {
            return  (T)(Byte)(byte)(int)src;
        }

        if(srcPrimitiveType == long.class && (type == int.class || type == Integer.class))
        {
            return (T)(Integer)(int)(long)src;
        }


        throw new Exception("尚未支持的类型转换" + srcType.getSimpleName() + "到" + type.getSimpleName());
    }


    static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static void debug(Class host, Object... msg) {
        var sb = new StringBuilder();

        if (!FXUtils.isNullOrEmpty(msg)) {
            if (msg.length == 1) {
                sb.append(msg[0]);
            } else if (msg.length > 1) {
                sb.append(String.format(msg[0].toString(), Arrays.copyOfRange(msg, 1, msg.length)));
            }
        }

        var now = new Date();
        var text = TIME_FORMAT.format(now)
                + "[" + host.getSimpleName() + "]"
                + sb;
        System.out.println(text);

    }



    /**
     * 获取成员的值
     * @param object
     * @param member
     * @return
     * @throws Exception
     */
    public static Object getMember(Object object, Member member) throws Exception {

        if(member instanceof Method)
        {
            return ((Method) member).invoke(object);
        }

        if(member instanceof Field)
        {
            return ((Field) member).get(object);
        }

        throw new IllegalArgumentException("不支持的成员类型");
    }

    /**
     * 获取成员的值。
     * 方法会尝试get方法和字段。
     * @param object
     * @param member
     * @return
     * @throws Exception
     */
    public static Object getMember(Object object, String member) throws Exception {
        return getMember(object, findMember(object.getClass(), member));
    }

    public static Member findMember(Class type, String member) throws Exception {
        Ref<Boolean> match = new Ref<>();
        return findMember(
                type,
                m->(instanceOf(m, Method.class, meth->meth.getName().equalsIgnoreCase("get" + member) || meth.getName().equalsIgnoreCase(member) , match) && match.get())
                        || (instanceOf(m, Field.class, field->field.getName().equalsIgnoreCase(member), match) && match.get())
        );
    }

    public static Class getMemberType(Object object, String member) throws Exception {
        Ref<Boolean> match = new Ref<>();
        Member mem = findMember(object.getClass(), member);

        if(mem instanceof Method)
        {
            return ((Method) mem).getReturnType();
        }

        if(mem instanceof Field)
        {
            return ((Field) mem).getType();
        }

        throw new IllegalArgumentException("不支持的成员类型");
    }

    /**
     * 设置成员的值
     * @param object
     * @param member
     * @param value
     * @param changeType
     * @throws Exception
     */
    public static void setMember(Object object, Member member, Object value, boolean changeType) throws Exception {

        if(member instanceof Method)
        {
            Method m = ((Method) member);
            m.invoke(object, changeType ? changeType(value, m.getParameterTypes()[0]) : value);
            return;
        }

        if(member instanceof Field)
        {
            Field f = ((Field) member);
            f.setAccessible(true);
            f.set(object, changeType ? changeType(value, f.getType()) : value);
            return;
        }

        throw new IllegalArgumentException("不支持的成员类型");
    }

    public static <T>int indexOf(T[] array, T find) {
        for (var i = 0; i < array.length; i++) {
            if (array[i].equals(find)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 返回集合中首个匹配的元素
     * @param array
     * @param finder
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> T first(T[] array, Func1<T, Boolean> finder) throws Exception {
        for (T i : array) {
            if (finder.invoke(i)) {
                return i;
            }
        }
        return null;
    }

    /**
     * 查找成员
     * @param type
     * @param finder
     * @return
     * @throws Exception
     */
    public static Member findMember(Class<?> type, Func1<Member, Boolean> finder) throws Exception {

        Member result;
        if((result = first(type.getMethods(), finder)) != null)
        {
            return result;
        }

        if((result = first(type.getFields(), finder)) != null)
        {
            return result;
        }

        return null;

    }

    /**
     * 设置成员的值。
     * 方法会尝试set方法和字段。
     * @param object
     * @param member
     * @param value
     * @param changeType
     * @return
     * @throws Exception
     */
    public static void setMember(Object object, String member, Object value, boolean changeType) throws Exception {
        Ref<Boolean> match = new Ref<>();
        Member mem = findMember(
                object.getClass(),
                m->(instanceOf(m, Method.class, meth->meth.getName().equalsIgnoreCase("set" + member), match) && match.get())
                        || (instanceOf(m, Field.class, field->field.getName().equalsIgnoreCase(member), match) && match.get())
        );

        setMember(object, mem, value, changeType);
    }


    public static boolean contains(String[] array, String value, boolean ignoreCase)
    {
        if(array == null || array.length == 0)
        {
            return false;
        }

        for(var v : array)
        {
            if(value == null)
            {
                if(v==null)
                {
                    return true;
                }
                continue;
            }

            if(ignoreCase?value.equalsIgnoreCase(v):value.equals(v))
            {
                return true;
            }
        }

        return false;

    }

    /**
     * 对象是否匹配类型，若匹配则执行func。
     * @param obj
     * @param type
     * @param func
     * @param result
     * @param <T>
     * @param <R>
     * @return
     * @throws Exception
     */
    public static <T,R> boolean instanceOf(Object obj, Class<T> type, Func1<T, R> func, Ref<R> result) throws Exception {
        if(type.isAssignableFrom(obj.getClass()))
        {
            result.set(func.invoke((T)obj));
            return true;
        }

        return false;
    }

    /**
     * 尝试执行代码，如果发生异常则返回异常
     * @param act
     * @return
     */
    public static Exception tryInvoke(Action act)
    {
        try
        {
            act.invoke();
            return null;
        }
        catch (Exception ex)
        {
            return ex;
        }
    }

    /**
     * 尝试执行代码，如果发生异常则返回异常
     * @param arg
     * @param act
     * @param <T>
     * @return
     */
    public static <T> Exception tryInvoke(T arg, Action1<T> act)
    {
        try
        {
            act.invoke(arg);
            return null;
        }
        catch (Exception ex)
        {
            return ex;
        }
    }

    /**
     * 字符串为null或空
     * @param val
     * @return
     */
    public static boolean isNullOrEmpty(String val)
    {
        return val == null || val.length() == 0;
    }


    public static boolean isNullOrWhiteSpace(String val)
    {
        return val == null || val.trim().length() == 0;
    }

    public static boolean isNullOrZero(BigInteger value)
    {
        return value == null || value.equals(BigInteger.ZERO);
    }

    /**
     * 数组为null或空
     * @param array
     * @param <T>
     * @return
     */
    public static <T> boolean isNullOrEmpty(T[] array)
    {
        return array == null || array.length==0;
    }


    /**
     * 计算表达式结果
     * @param expression
     * @param args
     * @return
     */
    public static Object eval(String expression, Map<String, Object> args) throws ScriptException {
        System.setProperty("nashorn.args", "--language=es6");
        var script = new ScriptEngineManager().getEngineByName("Nashorn");
        for(var member : args.keySet())
        {
            script.put(member, args.get(member));
        }
        return script.eval(expression);
    }
}
