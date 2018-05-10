/*
 *  Copyright 2018 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.analytics;

public enum GARequiredParameter {
    version ("v="),
    trackid ("tid="), //Id created on Google analytics server
    clientid ("cid="), // it is a unique ID eack client will send
    hittype ("t="), //Type of information which we are sending ex page, app etc.
    noninteractionhit ("ni="); // either hit type or non interactive hit type we have to use

    private String param;

    private GARequiredParameter(String param){
        this.param = param;
    }

    public String param(){
        return param;
    }

}
