/*
 * Copyright (c) 2011 APP-SOLUT
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.appsolut.example.queryContacts;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;

public class MainActivity extends Activity {
	private final List<SpinnerEntry> spinnerContent = new LinkedList<SpinnerEntry>();
	private Spinner contactSpinner;
	private ListView contactListView;
	private final ContactsSpinnerAdapater adapter = new ContactsSpinnerAdapater(spinnerContent, this);
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		contactSpinner = (Spinner)findViewById(R.id.contactsSpinner);
		contactListView = (ListView)findViewById(R.id.contactsListView);

		contactSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				updateList(position);
			}

			public void onNothingSelected(AdapterView<?> parent) {
				updateList(contactSpinner.getSelectedItemPosition());
			}
			
			private void updateList(int position) {
				if(position < adapter.getCount() && position >= 0) {
					SpinnerEntry currentEntry = adapter.getItem(position);
					contactListView.setAdapter(null);
					final List<ListViewEntry> content = new LinkedList<ListViewEntry>();
					queryAllPhoneNumbersForContact(currentEntry.getContactId(), content);
					queryAllEmailAddressesForContact(currentEntry.getContactId(), content);
					contactListView.setAdapter(new ContactListViewAdapter(content,MainActivity.this));
				}
			}

		});

		queryAllRawContacts();
		contactSpinner.setAdapter(adapter);
	}

	private void queryAllRawContacts() {
		
		final String[] projection = new String[] {
				RawContacts.CONTACT_ID,					// the contact id column
				RawContacts.DELETED						// column if this contact is deleted
		};
		
		final Cursor rawContacts = managedQuery(
				RawContacts.CONTENT_URI,				// the uri for raw contact provider
				projection,	
				null,									// selection = null, retrieve all entries
				null,									// not required because selection does not contain parameters
				null);									// do not order

		final int contactIdColumnIndex = rawContacts.getColumnIndex(RawContacts.CONTACT_ID);
		final int deletedColumnIndex = rawContacts.getColumnIndex(RawContacts.DELETED);
		
		spinnerContent.clear();
		if(rawContacts.moveToFirst()) {					// move the cursor to the first entry
			while(!rawContacts.isAfterLast()) {			// still a valid entry left?
				final int contactId = rawContacts.getInt(contactIdColumnIndex);
				final boolean deleted = (rawContacts.getInt(deletedColumnIndex) == 1);
				if(!deleted) {
					spinnerContent.add(queryDetailsForContactSpinnerEntry(contactId));
				}
				rawContacts.moveToNext();				// move to the next entry
			}
		}

		rawContacts.close();
	}

	private SpinnerEntry queryDetailsForContactSpinnerEntry(int contactId) {
		final String[] projection = new String[] {
				Contacts.DISPLAY_NAME,					// the name of the contact
				Contacts.PHOTO_ID						// the id of the column in the data table for the image
		};

		final Cursor contact = managedQuery(
				Contacts.CONTENT_URI,
				projection,
				Contacts._ID + "=?",						// filter entries on the basis of the contact id
				new String[]{String.valueOf(contactId)},	// the parameter to which the contact id column is compared to
				null);
		
		if(contact.moveToFirst()) {
			final String name = contact.getString(
					contact.getColumnIndex(Contacts.DISPLAY_NAME));
			final String photoId = contact.getString(
					contact.getColumnIndex(Contacts.PHOTO_ID));
			final Bitmap photo;
			if(photoId != null) {
				photo = queryContactBitmap(photoId);
			} else {
				photo = null;
			}
			contact.close();
			return new SpinnerEntry(contactId, photo, name);
		}
		contact.close();
		return null;
	}

	private Bitmap queryContactBitmap(String photoId) {
		final Cursor photo = managedQuery(
				Data.CONTENT_URI,
				new String[] {Photo.PHOTO},		// column where the blob is stored
				Data._ID + "=?",				// select row by id
				new String[]{photoId},			// filter by the given photoId
				null);
		
		final Bitmap photoBitmap;
		if(photo.moveToFirst()) {
			byte[] photoBlob = photo.getBlob(
					photo.getColumnIndex(Photo.PHOTO));
			photoBitmap = BitmapFactory.decodeByteArray(
					photoBlob, 0, photoBlob.length);
		} else {
			photoBitmap = null;
		}
		photo.close();
		return photoBitmap;
	}

	public void queryAllPhoneNumbersForContact(int contactId, List<ListViewEntry> content) {
		final String[] projection = new String[] {
				Phone.NUMBER,
				Phone.TYPE,
		};

		final Cursor phone = managedQuery(
				Phone.CONTENT_URI,	
				projection,
				Data.CONTACT_ID + "=?",
				new String[]{String.valueOf(contactId)},
				null);

		if(phone.moveToFirst()) {
			final int contactNumberColumnIndex = phone.getColumnIndex(Phone.NUMBER);
			final int contactTypeColumnIndex = phone.getColumnIndex(Phone.TYPE);
			
			while(!phone.isAfterLast()) {
				final String number = phone.getString(contactNumberColumnIndex);
				final int type = phone.getInt(contactTypeColumnIndex);
				content.add(new ListViewEntry(number, Phone.getTypeLabelResource(type),R.string.type_phone));
				phone.moveToNext();
			}
			
		}
		phone.close();
	}

	
	public void queryAllEmailAddressesForContact(int contactId, List<ListViewEntry> content) {
		final String[] projection = new String[] {
				Email.DATA,							// use Email.ADDRESS for API-Level 11+
				Email.TYPE
		};

		final Cursor email = managedQuery(
				Email.CONTENT_URI,	
				projection,
				Data.CONTACT_ID + "=?",
				new String[]{String.valueOf(contactId)},
				null);

		if(email.moveToFirst()) {
			final int contactEmailColumnIndex = email.getColumnIndex(Email.DATA);
			final int contactTypeColumnIndex = email.getColumnIndex(Email.TYPE);
			
			while(!email.isAfterLast()) {
				final String address = email.getString(contactEmailColumnIndex);
				final int type = email.getInt(contactTypeColumnIndex);
				content.add(new ListViewEntry(address, Email.getTypeLabelResource(type),R.string.type_email));
				email.moveToNext();
			}
			
		}
		email.close();
	}


}
