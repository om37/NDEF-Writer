package om37.NdefWriter;

import java.io.IOException;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.Resources.Theme;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 
 * @author om37
 *
 */
public class WelcomeActivity extends Activity {

	Activity self = this;

	NfcAdapter mAdapter;

	NdefMessage messageToWrite;

	TextView statusDisplay;

	PendingIntent pending;
	IntentFilter[] intentFiltersArray;
	String[][] techListArray;
	boolean writeMode;
	String statusMessage;

	/**
	 * Called when app is started
	 * Assign global TextView, statusDisplay
	 * 
	 * Initialise writeMode to false
	 * Prepare pending activity
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_screen);
		setupPendingActivity();

		//TextView 
		statusDisplay = (TextView)findViewById(R.id.txtStatusMessage);
		statusDisplay.setText("Write mode disabled. Hit 'Write' to enable.");

		mAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());		
		writeMode = false;
	}

	/**
	 * Event handler for write button
	 * 
	 * Hide keyboard to make sure status box is visible
	 * Hide instructions and welcome message
	 * 
	 * Show  status bar and message area
	 * Create NDEF message
	 * Enable foreground dispatch - implement writing to tag in onNewIntent()
	 * 
	 * @param theView requires as this is called in response to button press
	 */
	public void writeButton(View theView)
	{		
		//Hide keyboard via InputMethodManager
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(findViewById(R.id.txtToWrite).getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

		findViewById(R.id.txtInstruction).setVisibility(View.GONE);
		findViewById(R.id.txtWelcome).setVisibility(View.INVISIBLE);

		findViewById(R.id.txtStatus).setVisibility(View.VISIBLE);
		findViewById(R.id.txtStatusMessage).setVisibility(View.VISIBLE);

		messageToWrite = createNdefMessage();//Create the NDEF message
		if(messageToWrite == null)
		{
			return;
		}		

		writeMode = true;//We are in write mode
		statusDisplay.setBackgroundResource(R.drawable.rounded_corer_yellow);
		statusDisplay.setText("WRITE MODE ACTIVE!\nTouch tag to phone");
		findViewById(R.id.btnWriter).setEnabled(false);
		findViewById(R.id.btnCancel).setVisibility(View.VISIBLE);
		
		//Enable foreground dispatch so that when a tag is touched, onNewIntent(). is called
		mAdapter.enableForegroundDispatch(this, pending, intentFiltersArray, techListArray);		
	}
	
	/**
	 * Button handler for cancel button
	 * 
	 * Ends write mode
	 * Disables foreground dispatch
	 * Updates status message to user
	 * Re-enable write button
	 * 
	 * Hide cancel button
	 * @param theButton
	 */
	public void cancelButton(View theButton)
	{
		mAdapter.disableForegroundDispatch(this);
		writeMode = false;
		statusDisplay.setBackground(null);
		statusDisplay.setText("Write mode now DISABLED\nUser cancelled from process.");
		findViewById(R.id.btnCancel).setVisibility(View.GONE);
		findViewById(R.id.btnWriter).setEnabled(true);
	}

	/**
	 * Called when write button is pressed
	 * This method prepares a properly formed NDEF message
	 * using the custom mime type taken from this app's
	 * package name.
	 * 
	 * If EditText is empty do nothing and return error
	 * Else take the entered text and create an NDEF message from it
	 * 
	 * @return the created NDEF message
	 */
	public NdefMessage createNdefMessage()
	{
		String mimeType = "application/om37.ndefwriter";//custom mime, listened for by reader app. AAR wouldn't work
		EditText txtEntry = (EditText) findViewById(R.id.txtToWrite);

		String theContents = txtEntry.getText().toString();//Get text from user entry

		if(theContents.length() <= 0)
		{
			statusDisplay.setText("ERROR:\nTag not written - no text was entered.\nWrite mode now DISABLED");//Possible change here!
			statusDisplay.setBackgroundResource(R.drawable.rounded_corer_red);
			return null;
		}

		NdefRecord textRecord = NdefRecord.createMime(mimeType, theContents.getBytes());//Create text record from entered text
		NdefRecord textRecord2 = new NdefRecord(
				NdefRecord.TNF_MIME_MEDIA,//tnf
				mimeType.getBytes(),//mime type
				new byte[0],//id 
				theContents.getBytes()//payload
				);

		NdefMessage message = new NdefMessage(new NdefRecord[]{ textRecord });

		return message;
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}

	/**
	 * Hide onscreen keyboard.
	 */
	@Override
	public void onPause()
	{
		super.onPause();
		//Hide keyboard via InputMethodManager
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(findViewById(R.id.txtToWrite).getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
	}

	/**
	 * If write mode active, write text
	 * to tag in intent
	 */
	@Override
	public void onNewIntent(Intent intent)
	{		
		final Ndef ndefTag = Ndef.get((Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));//Get tag from intent
		if(writeMode)
		{
			//Write to tag
			writeToTag(ndefTag);
		}
		else
		{
			//Shouldn't be called if not in write mode.
			//Do Something else...
			//Maybe display tag's contents
			//Or ignore...
		}		
	}

	/**
	 * Uses runnable to write a message to a tag
	 * Perform required validation
	 * (message isn't tull,
	 * tag isn't read only,
	 * etc)
	 * Write to tag
	 * 
	 * Update status message area
	 * with error or success
	 * @param ndefTag
	 */
	public void writeToTag(final Ndef ndefTag)//Ndef has to be final for use in Runnable
	{
		final Handler handler = new Handler();
		Runnable r = new Runnable()
		{
			@Override
			public void run() 
			{					
				if(messageToWrite == null)
				{
					statusMessage = "ERROR:\nTag not written - No NDEF message was created.\nWrite mode now DISABLED";
					endWriteMode();
					return;
				}

				if(ndefTag.isWritable())
				{
					if(ndefTag != null)
					{
						try
						{
							ndefTag.connect();
							ndefTag.writeNdefMessage(messageToWrite);
							ndefTag.close();
							statusMessage = "SUCCESS:\nTag written. Hit 'Write' to write to another.";
						}
						catch(Exception e)
						{
							e.printStackTrace();
							statusMessage = "ERROR:\nTag NOT written - Exception:\n" + e.getMessage();
						}
					}
					else
					{
						statusMessage = "ERROR:\nTag not written - could not connect to tag.";
					}
				}
				else
				{
					statusMessage="ERROR:\nTag not written - the tag is not writable";
				}
				
				endWriteMode();
			}

			public void endWriteMode()
			{
				handler.post(new Runnable()
				{
					public void run()
					{
						writeMode = false;
						findViewById(R.id.btnWriter).setEnabled(true);
						findViewById(R.id.btnCancel).setVisibility(View.GONE);
						
						statusDisplay.setText("Write mode now DISABLED:\n" + statusMessage);
						
						if(statusMessage.contains("SUCCESS"))
							statusDisplay.setBackgroundResource(R.drawable.rounded_corer_green);

						else if(statusMessage.contains("ERROR"))
							statusDisplay.setBackgroundResource(R.drawable.rounded_corer_red);
						
						mAdapter.disableForegroundDispatch(self);
					}
				});
			}
		};

		Thread t = new Thread(r);
		t.start();
	}

	/**
	 * Sets up the pending activity object required to use foreground dispatch
	 * Uses PendingIntent.getActivity() to instantiate the object and then creates
	 * an array of IntentFilters and a techList array.
	 * 
	 * This is called from onCreate();
	 */
	public void setupPendingActivity()
	{
		pending = PendingIntent.getActivity(
				this, 
				0, 
				new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
				0
				);		

		IntentFilter ndefPlainTextFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);//Listen for NDEF discoveries		
		try
		{
			//ndefPlainTextFilter.addDataType("text/plain");
			ndefPlainTextFilter.addDataType("application/"+getPackageName());
		}
		catch(MalformedMimeTypeException e)
		{
			e.printStackTrace();
		}
		intentFiltersArray = new IntentFilter[]{ndefPlainTextFilter,};
		techListArray = new String[][]{new String[]{Ndef.class.getName()}};
	}

}