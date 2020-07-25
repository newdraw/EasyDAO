/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easydao.utils;
 

@FunctionalInterface
public interface Func2<TArg0, TArg1, TResult> {
    TResult invoke(TArg0 arg0, TArg1 arg1) throws Exception;
}