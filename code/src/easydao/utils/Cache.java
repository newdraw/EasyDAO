/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easydao.utils;

import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author ryu
 */
public class Cache {

    public String name;

    public Cache(String name) {
        this.name = name;
    }

    private class CacheItem {
        public Object data;
        public Date Time;
        public Object Key;
    }


    /**
     * 缓存配额
     */
    public long quota = 1024 * 1024 * 500;
    /**
     * 缓存配额模式
     */
    public CacheQuotaMode quotaMode = CacheQuotaMode.FreeMemoryBytes;
//    public long totalMemoryBytes = 1024 * 1024 * 100;
    private final HashMap<Object, CacheItem> cache = new HashMap<Object, CacheItem>();
    Runtime runtime = Runtime.getRuntime();

    public <T> boolean tryGet(Object key, long timeLimit, Ref<T> cache) {
        CacheItem info = this.cache.getOrDefault(key, null);

        //no cache
        if (info == null) {
            return false;
        }

        //cache timeout
        if (new Date().getTime() - info.Time.getTime() > timeLimit) {
            return false;
        }

        cache.set((T)info.data);
        return true;//!cache.isEmpty();
    }
 
    public <T> void put(Object key, T value) {
        //检查配额
        var exceeded = false;
        switch(quotaMode)
        {
            case MaxItems:
                exceeded = cache.size() > quota;
                break;
            case FreeMemoryBytes:
                exceeded = runtime.freeMemory() < quota;
                break;
        }
        if (exceeded) {
            synchronized (cache) {
                FXUtils.debug(Cache.class, "Cache[%s]由于缓存配额不足，释放缓存。", name);
                cache.clear();
                //todo:
                //ArrayList<DBCacheInfo> items = new ArrayList<>(cache.values());
                //items.sort((a,b)->-a.Time.compareTo(b.Time)); 
            }
        }
        CacheItem info = new CacheItem();
        info.data = value;
        info.Time = new Date();
        //info.Key = key;
        synchronized (cache) {
            cache.put(key, info);
            FXUtils.debug(Cache.class, "Cache[%s]的项目数：%d", name, cache.size());
        }

    }
}

/**
 * 缓存配额模式
 */
enum CacheQuotaMode
{
    /**
     * 限制项目数
     */
    MaxItems,
    /**
     * 限制系统剩余内存
     */
    FreeMemoryBytes
}