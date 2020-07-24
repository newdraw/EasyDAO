/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easyorm.utils;

import java.util.Arrays;

/**
 *
 * @author Newdraw
 */
public class CompositeKey {
      
    public Object[] items;
    public CompositeKey(Object... items)
    {
        this.items = items;
    }
    
    @Override  
    public boolean equals(Object obj) {  
        if(!(obj instanceof CompositeKey)){
            return false;  
        }  
        
        return Arrays.deepEquals(this.items, ((CompositeKey)obj).items);
    }  

    @Override
    public int hashCode() { 
        return (71 * 5) ^ items.length ^ Arrays.deepHashCode(this.items);
    }
    
}
