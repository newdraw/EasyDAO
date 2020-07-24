/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easyorm.utils;


@FunctionalInterface
public interface Func1<TArg, TResult>{
    TResult invoke(TArg arg) throws Exception;
}