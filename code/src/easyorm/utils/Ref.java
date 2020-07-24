package easyorm.utils;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Newdraw
 */
public class Ref<T> {
    
    private boolean isEmpty = true;
    private T value;

    public Ref()
    {

    }

    public Ref(T defaultValue)
    {
        this.value = defaultValue;
    }

    public Ref(T defaultValue, boolean isEmpty)
    {
        this.value = defaultValue;
        this.isEmpty = isEmpty;
    }

    public void setIfNotNull(T value)
    {
        if(value != null)
        {
            set(value);
        }
    }

    public void set(T value)
    {
        isEmpty = false;
        this.value = value;
    }
    
    public T get()
    {
        return this.value;
    }
    
    public boolean isEmpty()
    {
        return isEmpty;
    }
    
}
