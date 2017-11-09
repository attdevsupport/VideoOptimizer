/*
 *  Copyright 2014 AT&T
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
package com.att.arocollector.privatedata;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

class ContactsDataCollector extends AbstractDeviceDataCollector {

	private Context context;
	private int numContactsToCollect;
	private static final String TAG = ContactsDataCollector.class.getSimpleName();
	
	ContactsDataCollector(Context context, String dataFilePath, int numContactsToCollect) {
		
		super(dataFilePath);
		this.context = context;
		this.numContactsToCollect = numContactsToCollect;
	}
	
	@Override
	List<NameValuePair> getData(){

		String[] projection = new String[] { ContactsContract.Contacts._ID, 
				ContactsContract.Contacts.DISPLAY_NAME,
				ContactsContract.Contacts.HAS_PHONE_NUMBER };

		Cursor cur = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, 
				projection, null, null, null);
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		
		int count = 0;
		
		/*
		 * Go through the contacts retrieved from the query one by one
		 * until we have found the specified number of contacts with
		 * an existing phone number and email. 
		 */
		while (cur.moveToNext() && count < numContactsToCollect) {

			String id = cur.getString(0);
			String name = cur.getString(1);
			String hasPhoneNumber = cur.getString(2);
			String phone = null;
			String email = null;

			/*
			 * Continue to look up the phone number and email of
			 * the current contact if the query result indicates
			 *  that the contact has phone number and email.
			 */
			if (Integer.parseInt(hasPhoneNumber) > 0) {

				Cursor phoneCur = context.getContentResolver().query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
						ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", 
						new String[] { id }, null);

				if (phoneCur.moveToNext()) {

					phone = phoneCur
						.getString(phoneCur.getColumnIndex(
								ContactsContract.CommonDataKinds.Phone.NUMBER));
				}
 
				phoneCur.close();

				Cursor emailCur = context.getContentResolver().query(
						ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
						ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", 
						new String[] { id }, null);
	
				if (emailCur.moveToNext()) {
	
					email = emailCur
						.getString(emailCur.getColumnIndex(
								ContactsContract.CommonDataKinds.Email.ADDRESS));
				}
	
				emailCur.close();
	
				if (phone != null && email != null) {
					data.add(new NameValuePair(PrivateDataCollectionConst.CONTACT_NAME, name));
					data.add(new NameValuePair(PrivateDataCollectionConst.CONTACT_EMAIL, email));
					data.add(new NameValuePair(PrivateDataCollectionConst.CONTACT_PHONE_NUMBER, phone));
					count++;
	
					Log.d(TAG, "Contact Name, Phone, Email: " 
							+ name + ", " + email + ", " + phone);				
				}
			}
		}
			
		return data;
	}
	
}
