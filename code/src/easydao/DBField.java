package easydao;

/**
 *
 * @author Newdraw
 */
public class DBField
{
    public String name;
    public Class<?> type;
    public DBField(String name, Class type)
    {
        this.name =name;
        this.type =type;
    }
}