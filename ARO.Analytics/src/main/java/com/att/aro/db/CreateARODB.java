package com.att.aro.db;


import java.io.File;
import com.orientechnologies.orient.core.db.object.ODatabaseObject;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;

/**
 * Created by Harikrishna Yaramachu on 4/15/14.
 * 
 * Modified by Borey Sao
 * On November 19, 2014
 * Description: check if runningFile exist because there is already an instance of ODatabaseObject
 * , if exist we don't want to run two instance of it, it will keep Analyzer from starting.
 */
public class CreateARODB {
	private String dbFolder = System.getProperty( "user.home" ) + "/orient/db";
	//this file is created when a new instance of ODatabaseObject is created
	private String runningFile = dbFolder + "/db.wmr";
	private String url =  "local:" + dbFolder;
	private String user = "";
	private String pass = "";
	private static Object syncObj = new Object();
	private ODatabaseObject db = null;
	
	
	private CreateARODB(){
		
	}
	boolean init(){
		if(canInit()){
			db = new OObjectDatabaseTx(url);
			return true;
		}else{
			return false;
		}
	}
	private static CreateARODB createDB = null;
	public static CreateARODB getInstance(){
		
		if(createDB == null){
			synchronized(syncObj){
				if(createDB == null){
					createDB = new CreateARODB();
					if(!createDB.init()){
						createDB = null;
					}
				}
			}
		}
		return createDB;
	}
	private boolean canInit(){
		File file = new File(runningFile);
		if(file.exists()){
			return false;
		}else{
			return true;
		}
	}
	public ODatabaseObject getObjectDB(){

    	  //If Database doesn't exists then create one
    	 if(!db.exists()){ 		        	
	          db.create();
	          
	     }else{
	        db.open(user, pass);	            
	     }
    	
        return db;

    }

	 public void closeDB(){
		 if(db != null){
			 if(!db.isClosed()){
				 db.close();
			 }
		 }
	 }
}
