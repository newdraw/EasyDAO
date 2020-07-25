/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easydao.utils;

@FunctionalInterface
public interface Action {
    void invoke() throws Exception;
}
