package com.att.aro.analytics;

/**
 * Created by Harikrishna Yaramachu on 3/28/14.
 */
public enum GARequiredParameter {

    version ("v="), //version
    trackid ("tid="), //Id created on Google analytics server
    clientid ("cid="), // it is a unique ID eack client will send
    hittype ("t="),   //Type of information which we are sending ex page, app etc.
    noninteractionhit ("ni=")// either hit type or non interactive hit type we have to use
    ;

    private String param;

    private GARequiredParameter(String param){
        this.param = param;
    }

    public String param(){
        return param;
    }


}
