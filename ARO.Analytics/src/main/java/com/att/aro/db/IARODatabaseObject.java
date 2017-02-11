package com.att.aro.db;

import java.util.List;

/**
 * Created by Harikrishna Yaramachu on 4/16/14.
 */
public interface IARODatabaseObject extends IGenericeARODao {

    public <T extends Object> void put(T iPojo);
    public <T> List<T> get(T iTypeObject);
    public <T extends Object> void delete(T iPojo);
    public <T> long recordCount(T iPojoClass);


}
