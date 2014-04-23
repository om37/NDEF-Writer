package om37.NdefWriter;

import java.io.IOException;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 
 * @author om37
 *
 */
public class WelcomeActivity extends Activity {
	
	Activity self = this;
	
	NfcAdapter mAdapter;

	NdefMessage messageToWrite;

	PendingIntent pending;
	IntentFilter[] intentFiltersArray;
	String[][] techListArray;
	boolean writeMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_screen);
		setupPendingActivity();
		
		mAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());		
		writeMode = false;
	}

	public void writeButton(View theView)
	{
		messageToWrite = createNdefMessage();//Create the NDEF message
		
		//Hide keyboard via InputMethodManager
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromInputMethod(((EditText)findViewById(R.id.txtToWrite)).getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
		
		writeMode = true;//We are in write mode
		findViewById(R.id.txtWriteMode).setVisibility(View.VISIBLE);
		
		showToast("Touch tag", false);//Alert user to write mode activation
		
		//Enable foreground dispatch so that when a tag is touched, onNewIntent(). is called
		mAdapter.enableForegroundDispatch(this, pending, intentFiltersArray, techListArray);		
	}

	/*
	 * TO DO:
	 *  Remember to change "om37.phpcall" to correct package name when other app is finished
	 */
	public NdefMessage createNdefMessage()
	{
		String mimeType = "application/om37.ndefwriter";
		EditText txtEntry = (EditText) findViewById(R.id.txtToWrite);
		CheckBox chk = (CheckBox)findViewById(R.id.chkAttendance);

		String theContents = txtEntry.getText().toString();//Get text from user entry

		if(theContents.length() <= 0)
			theContents = "This is some text from om37.NdefWriter";
		
		NdefRecord textRecord = NdefRecord.createMime("text/plain", theContents.getBytes());//Create text record from entered text
		NdefRecord textRecord2 = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,mimeType.getBytes(),new byte[0], theContents.getBytes());
		NdefRecord aar;
		
		//Create aar to start app
		if(chk.isChecked())
			aar = NdefRecord.createApplicationRecord("om37.attendancetracker");
		else
			aar = NdefRecord.createApplicationRecord("om37.phpcall");
		
		//NdefRecord aar = NdefRecord.createApplicationRecord(getPackageName());//Creates aar for this package/app
		
		NdefMessage message = new NdefMessage(new NdefRecord[]{ textRecord2 });

		return message;
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

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
					doToastHandler("Message is null. Returning", false);
					endWriteMode();
					return;
				}

				if(ndefTag != null && ndefTag.isWritable())
				{
					try
					{
						ndefTag.connect();
						ndefTag.writeNdefMessage(messageToWrite);
						ndefTag.close();
						doToastHandler("Write complete", false);
						endWriteMode();
					}
					catch(Exception e)
					{
						e.printStackTrace();
						doToastHandler("Write FAILED", false);
						endWriteMode();
					}
				}
			}
			
			public void doToastHandler(final String message, final boolean longShow)
			{
				handler.post(new Runnable()
				{
					public void run()
					{
						showToast(message, longShow);
					}
				});
			}
			
			public void endWriteMode()
			{
				handler.post(new Runnable()
				{
					public void run()
					{
						writeMode = false;
						findViewById(R.id.txtWriteMode).setVisibility(View.INVISIBLE);
						mAdapter.disableForegroundDispatch(self);
					}
				});
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}
	
	public void showToast(String text, boolean longShow)
	{
		Toast t = longShow ? 
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG) :
					Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);

				t.show();
	}

	
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
