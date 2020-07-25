/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easydao.utils;

/**
 *
 * @author Administrator
 */
@FunctionalInterface
public
interface Action1<T> {
    void invoke(T arg) throws Exception;
}
