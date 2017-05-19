package com.att.aro.analytics;

import java.util.List;
import java.util.logging.Logger;
import com.att.aro.db.AROObjectDao;

/**
 * Main class for tracking google analytics data.
 *
 * Created by Harikrishna Yaramachu on 3/18/14.
 * 
 * Modified by Borey Sao
 * On November 19, 2014
 * Description: use lazy initialization to speed up startup time and add close() for database clean up
 */
public class GoogleAnalyticsTracker {

	private static final Logger LOGGER = Logger.getLogger(GoogleAnalyticsTracker.class.getName());
    private URLBuildingStrategy urlBuildingStrategy = null;
    private HTTPGetMethod httpRequest = new HTTPGetMethod();
    private AROObjectDao aroDAO = null;
    private GoogleAnalyticsTracker gaTracker;
    
    private static boolean runninDBThreadTracker = false; // Check DB thread is running or not
    private int maxDBRecords;

    /**
     * Constructor passing the application name, application version & google analytics tracking code
     *
     * @param appName
     * @param appVersion
     * @param googleAnalyticsTrackingCode
     */
    public GoogleAnalyticsTracker(String appName, String appVersion, String googleAnalyticsTrackingCode, int maxDBRecords){ //ARO, version Name, UA-48887240-1
    	this.urlBuildingStrategy = new GAURLBuildingStrategy(appName, appVersion, googleAnalyticsTrackingCode);
        this.httpRequest.setApplicationName(appName);
        this.maxDBRecords = maxDBRecords;
        this.gaTracker = this;
        inetChecker();
        aroDAO = AROObjectDao.getInstance();
    }

    public GoogleAnalyticsTracker(){
    	this.gaTracker = this;
    };

    /**
     * Setter injection for URLBuildingStrategy incase if you want to use a different url building logic.
     * @param urlBuildingStrategy implemented instance of URLBuildingStrategy
     */
    public void setUrlBuildingStrategy(URLBuildingStrategy urlBuildingStrategy) {
        this.urlBuildingStrategy = urlBuildingStrategy;
    }
    

    public AROObjectDao getaroDAO() {
    	
        return aroDAO;
    }
    
    /**
     * PC internet setting verification
     */
    public void inetChecker(){
    	new NetworkConnectionChecker(httpRequest).run();
    }
    
    public static void setRunninDBThreadTracker(boolean runninDBThreadTracker) {
		GoogleAnalyticsTracker.runninDBThreadTracker = runninDBThreadTracker;
	}

    /**
     * Push all the events to google Analytics server if internet is available</br>
     * Other wise save all the records into local database.
     * @param focusPoint
     * @param appCloseEvent
     */
	public void pushToCloud(FocusPoint focusPoint, boolean appCloseEvent){
		//LOGGER.info("Max Number records stored : " + this.maxDBRecords);
		//LOGGER.info("Number of records in DB : " + this.aroDAO.recordCount(focusPoint));
    	if(httpRequest.isValidIConnection()){ //Check for Internet
    		if(appCloseEvent){
    			boolean pushReqFlag = httpRequest.request(urlBuildingStrategy.buildURL(focusPoint));
    			if(!pushReqFlag){
    				if(this.aroDAO.recordCount(focusPoint) < maxDBRecords){ // add not to save more than Max number of records
	    				//focusPoint.setEventLabel("offline");
    					focusPoint.setEventValue("1");
	    				focusPoint.resetSession();
	    				this.aroDAO.put(focusPoint);
    				}
    			}
    		}else{ // send Async req
    			trackAsynchronously(focusPoint); // Send GA req..Send separate thread
    			  			
    			if(this.aroDAO.recordCount(focusPoint) > 0){ // Invoke DB thread
        			
        			if(!runninDBThreadTracker){ // Check for db records Thread running. On separate thread push all the db records to cloud
        				runninDBThreadTracker = true;
        				new TrackDBRecordsThread(this.aroDAO.get(focusPoint)).start();
        				
        				//GARecordPojo testRecord = new GARecordPojo();
    
        				
        			}
        			
        		} 
    			 
    		}
    		
    	} else { //Since No internet save the record to DB and check again for internet
    		if(this.aroDAO.recordCount(focusPoint) < maxDBRecords){ // add not to save more than Max number of records
	    		//focusPoint.setEventLabel("offline");
    			focusPoint.setEventValue("1");
	    		focusPoint.resetSession();
	    		this.aroDAO.put(focusPoint);
    		}
    		
     	   inetChecker(); //Check for internet connection since no internet.
        }
    	
    }

    /**
     * Track the focusPoint in the application synchronously. <br/>
     * <red><b>Please be cognizant while using this method. Since, it would have a peformance hit on the actual application.
     * Use it unless it's really needed</b></red>
     *
     * @param focusPoint Focus point of the application like application load, application module load, user actions, error events etc.
     */
    public boolean trackSynchronously(FocusPoint focusPoint) {
    		boolean pushNotifyFlag = httpRequest.request(urlBuildingStrategy.buildURL(focusPoint));
    		return pushNotifyFlag;
     }


    /**
     * Track the focusPoint in the application asynchronously. <br/>
     *
     * @param focusPoint Focus point of the application like application load, application module load, user actions, error events etc.
     */
    public void trackAsynchronously(FocusPoint focusPoint) {
    	   new TrackingThread(focusPoint).start();
    }

    /**
     * Send the google analytics request Asynchronously 
     *
     *
     */
    private class TrackingThread extends Thread {
        private FocusPoint focusPointObj;
       //private IARODatabaseObject aDAOThread = AROObjectDao.getInstance();

        public TrackingThread(FocusPoint focusPoint) {
            this.focusPointObj = focusPoint;
            this.setPriority(Thread.MIN_PRIORITY);
        }

        public void run() {
        	String url = "";
        	if (this.focusPointObj.getExceptionDesc()==null) {
        		url = urlBuildingStrategy.buildURL(this.focusPointObj);
        	} 
            boolean pushCloudFlag = httpRequest.request(url);
            
            //If fails...Update Internet Flag. 
            //send record to DB.
            if(!pushCloudFlag){
            	httpRequest.setIsValidIConnection(false);
	            	if(gaTracker.aroDAO.recordCount(this.focusPointObj) < gaTracker.maxDBRecords){
		            	//focusPointObj.setEventLabel("offline");
	            		focusPointObj.setEventValue("1");
		            	focusPointObj.resetSession();
		            	gaTracker.aroDAO.put(focusPointObj);
		           	}
            }
         }
    }
    
    /**
     * Separate thread push all the records in database to cloud. 
     *
     *
     */
    private class TrackDBRecordsThread extends Thread{
        //private FocusPoint focusPoint;
        private List<FocusPoint> dbObjList;
    	
    	public TrackDBRecordsThread(List<FocusPoint> dbList){
    		this.dbObjList = dbList;
    		this.setPriority(Thread.MIN_PRIORITY);
    	}
    	
    	public void run(){
    		for (FocusPoint focusPointObj : dbObjList){  // Push all the off-line records to cloud when internet is up.
    			boolean pushCloudFlag = httpRequest.request(urlBuildingStrategy.buildURL(focusPointObj));
    
    			if(pushCloudFlag){
    				gaTracker.aroDAO.delete(focusPointObj);
    			} else {
    				
    				httpRequest.setIsValidIConnection(false);
    				break;
    			}
    		}
    		
    		GoogleAnalyticsTracker.setRunninDBThreadTracker(false);
			
    	}
    }

    /**
     * properly close database
     */
    public void close(){
    	if(aroDAO != null){
    		aroDAO.closeDB();
    		LOGGER.info("Properly closed database");
    	}
    }
}
