/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easyorm.utils;
 
 
@FunctionalInterface
public interface Func3<TArg0, TArg1, TArg2, TResult> {
    TResult invoke(TArg0 arg0, TArg1 arg1, TArg2 arg2) throws Exception;
}