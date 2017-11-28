/*
 *  Copyright 2017 AT&T
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
package com.att.aro.db;

/**
 * Created by geethakrishna on 4/15/14.
 */
public class ObjectTestPojo {
    public String getItem() {
        return item;
    }

    public String getValue() {
        return value;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String item;
    private String value;

}
