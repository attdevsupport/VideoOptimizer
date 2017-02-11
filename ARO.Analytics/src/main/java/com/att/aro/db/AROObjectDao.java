package com.att.aro.db;


import com.orientechnologies.orient.core.db.object.ODatabaseObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Harikrishna Yaramachu on 4/16/14.
 * Modified by Borey Sao 
 * On November 19, 2014
 * Description: to correctly implement Singleton pattern and to use database only if initialized 
 * successfully.
 */
public class AROObjectDao implements IARODatabaseObject{

	CreateARODB aroDB = null;
	private static AROObjectDao instance = null;
	private static Object syncObj = new Object();
    private AROObjectDao(){
    	aroDB = CreateARODB.getInstance();
    }
    
    public static AROObjectDao getInstance(){
    	if(instance == null){
    		synchronized(syncObj){
    			if(instance == null){
    				instance = new AROObjectDao();
    			}
    		}
    	}
    	return instance;
    }
    /**
     *
     * @param iPojo
     * @param <T>
     */
//    @Override
    public synchronized <T extends Object> void put(T iPojo){
    	if(aroDB != null){
	    	ODatabaseObject objectDB = aroDB.getObjectDB();
	    	try{
		    	objectDB.getEntityManager().registerEntityClass(iPojo.getClass());
		        objectDB.save(iPojo);
		
		        objectDB.commit();
	    	} catch (Exception ex){
	    		//Log the Error 
	    		
	    	}finally{
	    		objectDB.close();
	    	}
    	}
    }

    /**
     *
     * @param iPojo
     * @param <T>
     */
//    @Override
    public synchronized <T extends Object> void delete(T iPojo){
    	if(aroDB != null){
	    	ODatabaseObject objectDB = aroDB.getObjectDB();
	    	try{
		        objectDB.getEntityManager().registerEntityClass(iPojo.getClass());
		        objectDB.delete(iPojo);
		
		        objectDB.commit();
	    	}catch (Exception e){
	    		//Log the Error 
	    	} finally {
	    		objectDB.close();
	    	}
    	}
    }

    /**
     *
     * @param iPojoClass
     * @param <T>
     * @return
     */
//    @Override
    public synchronized <T> List<T> get(T iPojoClass) {

       List<T> returnObectList = new ArrayList<T>();
       if(aroDB != null){
	       ODatabaseObject objectDB = aroDB.getObjectDB();
	       
	       try{
		        objectDB.getEntityManager().registerEntityClass(iPojoClass.getClass());
		    
		    //    if(numberOfrecords > 0){
		            for(Object returnObj :  objectDB.browseClass(iPojoClass.getClass())){
		            	objectDB.setDirty(returnObj);
		                returnObectList.add((T)returnObj);
		            }
		     //   }
	       }catch (Exception ex){
	    	 //Log the Error 
	       } finally{
	    	   objectDB.close();
	       }
       }
        return returnObectList;
    }
    
    /**
     * 
     * @param iPojoClass
     * @return
     */
    public synchronized <T> long recordCount(T iPojoClass){
    	long numberOfrecords = 0;
    	if(aroDB != null){
	    	ODatabaseObject objectDB = aroDB.getObjectDB();
	    	
	    	try{
		        objectDB.getEntityManager().registerEntityClass(iPojoClass.getClass());
		        numberOfrecords = objectDB.countClass(iPojoClass.getClass().getSimpleName());
		
		   }catch (Exception ex){
	    	 //Log the Error 
	       } finally{
	    	   objectDB.close();
	       }
    	}
       return numberOfrecords;
    }

    /**
     * properly close database
     */
//    @Override
    public void closeDB(){
    	if(aroDB != null){
    		aroDB.closeDB();
    	}
    }

    /**
     * Dummy not implemented now for future use
     */
//    @Override
    public <T> T getDataBase(){
    	//ODatabaseObject objectDB = CreateARODB.getObjectDB();
        return (T) null;
    	//return (T) CreateARODB.getObjectDB();
    }
}
