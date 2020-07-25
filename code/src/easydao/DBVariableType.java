package easydao;

public enum DBVariableType {
    /**
     * 固定值
     */
    TYPE_FIXED_VALUE ,
    /**
     * 通过web层Getter函数动态取值
     */
    TYPE_VALUE_GETTER,
    /**
     * 通过sql表达式动态取值
     */
    TYPE_VALUE_SQLEXP
}
