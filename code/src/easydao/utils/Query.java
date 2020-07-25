package easydao.utils;


import easydao.*;

import java.lang.reflect.Array;
import java.util.*;

public class Query<TSrc> extends ArrayList<TSrc>{

    public Query<TSrc> distinct()
    {
        var result = new HashSet<TSrc>();
        for(var item : this)
        {
            if(result.contains(item))
            {
                continue;
            }
            result.add(item);
        }
        return Query.from(result);
    }

    public void each(Action1<TSrc> act) throws Exception {
        for(var item : this)
        {
            act.invoke(item);
        }
    }


    public Query<TSrc> orderBy(Comparator<? super TSrc> c) throws Exception {
        var result = Query.from(this);
        result.sort(c);
        return result;
    }

    interface DescWrapper<TSrc, Comparable> extends Func1<TSrc, Comparable>
    {

    }

    public <TNew> Query<TNew> join(DBTable table, Func2<TSrc, DBRow, Boolean> on, Func2<TSrc, DBRow, TNew> selector) throws Exception {
        var result = new Query<TNew>();
        for (var src : this) {
            for (var item : table.rows()) {
                if (on.invoke(src, item)) {
                    result.add(selector.invoke(src, item));
                }
            }
        }
        return result;
    }


    public <TNew, TJoin> Query<TNew> join(TJoin[] items, Func2<TSrc, TJoin, Boolean> on, Func2<TSrc, TJoin, TNew> selector) throws Exception {
        var result = new Query<TNew>();
        for (var src : this) {
            for (var item : items) {
                if (on.invoke(src, item)) {
                    result.add(selector.invoke(src, item));
                }
            }
        }
        return result;
    }

    public <TNew, TJoin> Query<TNew> join(Collection<TJoin> items, Func2<TSrc, TJoin, Boolean> on, Func2<TSrc, TJoin, TNew> selector) throws Exception {
        var result = new Query<TNew>();
        for (var src : this) {
            for (var item : items) {
                if (on.invoke(src, item)) {
                    result.add(selector.invoke(src, item));
                }
            }
        }
        return result;
    }

    /**
     * 将排序条件设为倒序
     * @param func
     * @param <TSrc>
     * @return
     */
    public static final<TSrc> Func1<TSrc, Comparable> desc(Func1<TSrc, Comparable> func)
    {
        DescWrapper<TSrc, Comparable> result = i->func.invoke(i);
        return result;
    }

    /**
     * 按字段排序
     * @param orderByFields 可以依次指定多个排序字段，如果需要倒序可以用Query.desc(...)包装字段
     * @return
     */
    public Query<TSrc> orderBy(Func1<TSrc, Comparable>... orderByFields) {
        var result = Query.from(this);
        Comparator<TSrc> comparator = (a, b) -> {
            for (var f : orderByFields) {
                int r;

                try {
                    r = f.invoke(a).compareTo(f.invoke(b));
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0;
                }

                if (r != 0) {
                    if (f instanceof DescWrapper) {
                        return -r;
                    }
                    return r;
                }
            }
            return 0;
        };
        result.sort(comparator);
        return result;
    }

    public <TResult> Query<TResult> select(Func1<TSrc, TResult> selector) throws Exception {
        var result = new Query<TResult>();
        for(var i : this)
        {
            result.add(selector.invoke(i));
        }
        return result;
    }

    /**
     * 查询数组所有元素
     * @param selector
     * @param <TResult>
     * @return
     * @throws Exception
     */
    public <TResult> Query<TResult> selectElements(Func1<TSrc, TResult[]> selector) throws Exception {
        var result = new Query<TResult>();
        for (var i : this) {
            result.addAll(selector.invoke(i));
        }
        return result;
    }

    public void addAll(TSrc[] items) {
        this.addAll(Arrays.asList(items));
    }

    public Query<TSrc> where(Func1<TSrc, Boolean> selector) throws Exception {
        var result = new Query<TSrc>();
        for (var i : this) {
            if (selector.invoke(i)) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * 找到首个符合条件的对象，如果没有找到则返回null。
     * @param selector
     * @return
     * @throws Exception
     */
    public TSrc first(Func1<TSrc, Boolean> selector)throws Exception {
        for (var i : this) {
            if (selector.invoke(i)) {
                return i;
            }
        }
        return null;
    }

    public Query<TSrc> clone()
    {
        return (Query<TSrc>)super.clone();
    }

    public Query<TSrc> unionAll(TSrc... items)  {
        var result = this.clone();
        result.addAll(items);
        return result;
    }

    /**
     * 将元素转为指定类型
     * @param t
     * @param <T>
     * @return
     */
    public <T> Query<T> cast(Class<T> t)
    {
        var result = new Query<T>();
        for(var i : this)
        {
            result.add((T)i);
        }
        return result;
    }


    public static <TSrc> Query<TSrc> from(Collection<TSrc> src) {
        var result = new Query<TSrc>();
        result.addAll(src);
        return result;
    }

    public static <TSrc> Query<TSrc> from(Enumeration<TSrc> src) {
        var result = new Query<TSrc>();
        while (src.hasMoreElements()) {
            result.add(src.nextElement());
        }
        return result;
    }

    public static Query<DBRow> from(DBTable src) throws Exception {
        return from(src.rows());
    }

    @SafeVarargs
    public static <TSrc> Query<TSrc> from(TSrc... src)  {
        var result = new Query<TSrc>();
        result.addAll(src);
        return result;
    }

    public <T> T[] toArray(Class<T> type) {
        var result = Array.newInstance(type, this.size());
        for (var i = 0; i < this.size(); i++) {
            Array.set(result, i, this.get(i));
        }
        return (T[]) result;
    }

    public static <TSrc> Query<TSrc> from(Object src) throws Exception {

        if(src == null)
        {
            return new Query<TSrc>();
        }

        var type = src.getClass();

        if(src instanceof Enumeration)
        {
            return Query.from((Enumeration)src);
        }

        if(src instanceof Collection) {
            return Query.from((Collection) src);
        }

        if(type.isArray())
        {
            return Query.from((TSrc[])src);
        }

        throw new IllegalArgumentException("不支持的类型");

    }



}
