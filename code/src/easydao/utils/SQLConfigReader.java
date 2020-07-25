/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easydao.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * @author Administrator
 */
public class SQLConfigReader implements AutoCloseable {
    
    private TreeMap<String, Node> items = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private TreeMap<String, String> texts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static String computer = System.getenv().get("COMPUTERNAME");
    private Query<File> files;
    private HashMap<File, Long> lastModified = new HashMap<>();

    private static ArrayList<SQLConfigReader> readers = new ArrayList<>();
    private static Thread autoReloadThread = new Thread(()->{
        while(true) {
            try {
                synchronized (readers) {
                    for (var i : readers) {
                        for (var f : i.lastModified.keySet()) {
                            if (f.lastModified() != i.lastModified.get(f)) {
                                i.reload();
                                break;
                            }
                        }
                    }
                }
                Thread.sleep(2000);
            } catch (Exception ex) {
                //ignore
            }
        }
    });

    static {
        autoReloadThread.start();
    }


    private void reload() throws Exception {
        synchronized (this) {
            items.clear();
            texts.clear();

            for (var file : files) {
                FXUtils.debug(SQLConfigReader.class, "加载配置文件：" + file.getPath());
                lastModified.put(file, file.lastModified());
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(file);
                NodeList nodes = doc.getElementsByTagName("item");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    var attrs = node.getAttributes();
                    var forComputer = attrs.getNamedItem("forComputer");
                    if (forComputer != null) {
                        var value = forComputer.getNodeValue();
                        if (!FXUtils.isNullOrWhiteSpace(value) && !value.equalsIgnoreCase(computer)) {
                            continue;
                        }
                    }
                    String key = attrs.getNamedItem("key").getNodeValue();
                    items.put(key, node);
                    texts.put(key, node.getTextContent());
                }
            }
        }
    }

    public SQLConfigReader(String... files) throws Exception {
        this.files = Query.from(files).select(i->new File(i));
        reload();
        synchronized (readers) {
            readers.add(this);
        }
    }

    public <T> T getOrDefault(Class<T> type, String key, T defaultValue) {
        if (!contains(key)) {
            return defaultValue;
        }

        try {
            return get(type, key);
        } catch (Exception ex) {
            //ignore
        }
        return defaultValue;
    }
    
    public String get(String key)
    {
        synchronized (this) {
            return texts.get(key);
        }
    }

    public <T> T get(Class<T> type, String key) throws Exception {
        return FXUtils.changeType(get(key), type);
    }

    public double getDouble(String key)
    {
        return Double.valueOf(get(key));
    }

    public int getInt(String key)
    {   
        return Integer.valueOf(get(key));
    } 
     
    public long getLong(String key)
    {   
        return Long.valueOf(get(key));
    } 
    
    public String getAttr(String key, String attr)
    {
        synchronized (this) {
            Node item = items.get(key);
            if (item == null) {
                throw new IllegalArgumentException("没有找到配置：" + key);
            }
            Node node = item.getAttributes().getNamedItem(attr);
            if (node == null) {
                return null;
            }
            return node.getNodeValue();
        }
    }
    
    public boolean tryGet(String key, Ref<String> value)
    {
        synchronized (this) {
            if (items.containsKey(key)) {
                value.set(get(key));
                return true;
            }

            return false;
        }
    }
    
    public boolean contains(String key) {
        synchronized (this) {
            return items.containsKey(key);
        }
    }

    public String[] keys()
    {
        synchronized (this) {
            return items.keySet().toArray(new String[0]);
        }
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        synchronized (readers) {
            readers.remove(this);
        }
    }
}
