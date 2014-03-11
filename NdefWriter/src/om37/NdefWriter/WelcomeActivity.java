package om37.NdefWriter;

import android.nfc.NfcAdapter;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.view.Menu;

public class WelcomeActivity extends Activity {
	
	NfcAdapter mAdapter;
	
	PendingIntent pending;
	IntentFilter[] intentFiltersArray;
	String[][] techListArray;
	boolean writeMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_screen);
		
		mAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
		
		writeMode = false;
		setupPendingActivity();
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
		
	}
	
	@Override
	public void onResume()
	{
		
	}
	
	@Override
	public void onNewIntent(Intent intent)
	{
		if(writeMode)
		{
			//Write to tag
		}
		else
		{
			//Something else...
			//Maybe display tag's contents
		}
	}
	
	public void setupPendingActivity()
	{
		pending = PendingIntent.getActivity(
			this, 
			0, 
			new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
			0
		);		
		IntentFilter ndefPlainTextFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);		
		try
		{
			//ndefPlainTextFilter.addDataType("text/plain");
			ndefPlainTextFilter.addDataType("application/"+getPackageName());
		}
		catch(MalformedMimeTypeException e)
		{
		}
		intentFiltersArray = new IntentFilter[]{ndefPlainTextFilter,};
		techListArray = new String[][]{new String[]{Ndef.class.getName()}};
	}
	
}
