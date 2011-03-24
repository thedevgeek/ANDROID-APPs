/* ========================================================================
 * Copyright 2010, Handy Codeworks LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */
package com.handycodeworks.tweety;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Tweety extends Activity implements OnClickListener, OnKeyListener {
    // Class variables
    private static final String TAG = "Tweety";
    private static final String USERNAME = "thedevgeek";
    private static final String PASSWORD = "Gulpcup1";
    private SharedPreferences prefs;
    static LocationHelper sLocationHelper;

    // Instance variables
    private Twitter mTwitter;
    private String username, password;

    // UI Elements
    private Button tweetButton, locationButton, clearButton;
    private TextView textStatus, numChars;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	Debug.startMethodTracing("tweety");

	setContentView(R.layout.main);

	// Find views by id
	tweetButton = (Button) findViewById(R.id.UpdateButton);
	tweetButton.setOnClickListener(this);
	textStatus = (TextView) findViewById(R.id.TextStatus);
	textStatus.setOnKeyListener(this);
	numChars = (TextView) findViewById(R.id.NumChars);
	locationButton = (Button) findViewById(R.id.locationButton);
	locationButton.setOnClickListener(this);
	clearButton = (Button) findViewById(R.id.clearButton);
	clearButton.setOnClickListener(this);

	// Setup preferences
	prefs = PreferenceManager.getDefaultSharedPreferences(this);
	prefs.registerOnSharedPreferenceChangeListener(
		new OnSharedPreferenceChangeListener() {

		    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			    String key) {
			mTwitter = null;
		    }
		});

	// Start update service
	startService(new Intent(this,UpdateService.class));

	// Init location manager
	sLocationHelper = new LocationHelper(this);
	sLocationHelper.startUpdates();
    }

    private Twitter getTwitter(){
	if(mTwitter == null){
	    username = prefs.getString("username", USERNAME);
	    password = prefs.getString("password", PASSWORD);


	    if(username == "" || username == null ||
	       password == "" || password == null ){
		Toast.makeText(getApplicationContext(), "Username and/or password not set!",
			Toast.LENGTH_SHORT).show();
	    }
	    mTwitter = new Twitter(username, password);
	    mTwitter.setSource(TAG); // Set Tweety as our twitter app name
	}
	return mTwitter;
    }

    public void onClick(View v) {

	String enteredText = textStatus.getText().toString();

	switch (v.getId()) {
	case R.id.clearButton:
	    textStatus.setText("");
	    break;
	case R.id.locationButton:
	    String newEntry = enteredText + sLocationHelper.getLocationString();
	    textStatus.setText(newEntry);
	    break;
	case R.id.UpdateButton:

	    // Status can't be empty
	    if (enteredText.length() == 0) {
		Toast.makeText(this, R.string.empty_status, Toast.LENGTH_SHORT)
			.show();
	    } else if (enteredText.length() < 0) {
		Toast.makeText(this, R.string.status_too_long,
			Toast.LENGTH_SHORT).show();
	    }
	    // Only process the button if status is not the same as the hint
	    else if (tweetButton.getId() == v.getId()) {
		try {
		    getTwitter().setStatus(textStatus.getText().toString());
		    Toast.makeText(this, R.string.status_posted,
			    Toast.LENGTH_SHORT).show();

		    // Show hint
		    textStatus.setText("");
		    // Refresh character label
		    updateCharacterCount();
		} catch (NullPointerException npe){
			Log.e(TAG, "Null twitter object, check username and password");
		    Toast.makeText(this, R.string.no_account,
				    Toast.LENGTH_SHORT).show();
		} catch (TwitterException te) {
		    Log.e(TAG, te.toString());
		    Toast.makeText(this, R.string.no_twitter,
			    Toast.LENGTH_SHORT).show();
		}

		Debug.stopMethodTracing();

	    }
	    break;
	}
	// Update count of characters
	updateCharacterCount();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.menu, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

	switch (item.getItemId()) {
		case R.id.about:
        	    Intent ia = new Intent(Tweety.this, About.class);
        	    startActivity(ia);
        	    break;
        	case R.id.menuPrefs:
        	    Intent ip = new Intent(Tweety.this, Prefs.class);
        	    startActivity(ip);
        	    break;
        	case R.id.menuTimeline:
        	    Intent it = new Intent(Tweety.this, Timeline.class);
        	    startActivity(it);
        	    break;
	}
	return true;
    }

    @Override
    protected void onStop() {
	sLocationHelper.stopUpdates();
	super.onStop();
    }

    private final int NUM_CHARS = 140;

    public boolean onKey(View v, int keyCode, KeyEvent event) {
	updateCharacterCount();
	return false;
    }

    private void updateCharacterCount() {
	int charsLeft = NUM_CHARS - textStatus.getText().length();
	if(charsLeft < 0){
	    tweetButton.setEnabled(false);
	    numChars.setTextColor(Color.RED);
	}
	else{
	    tweetButton.setEnabled(true);
	    numChars.setTextColor(Color.LTGRAY);
	}
	numChars.setText(String.valueOf(charsLeft));
    }

}