/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easyorm.utils;
 

@FunctionalInterface
public interface Func<TResult> {
    TResult invoke() throws Exception;
}