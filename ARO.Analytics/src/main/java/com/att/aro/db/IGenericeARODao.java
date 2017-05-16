package com.att.aro.db;

//import com.sun.org.apache.bcel.internal.generic.RET;


/**
 * Created by HArikrishna Yaramachu on 4/16/14.
 */
public interface IGenericeARODao {

    public <T> T getDataBase();

    public void closeDB();

}
