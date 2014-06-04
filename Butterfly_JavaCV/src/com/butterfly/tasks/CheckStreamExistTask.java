package com.butterfly.tasks;

import com.butterfly.listeners.IAsyncTaskListener;

import android.app.Activity;
import android.os.AsyncTask;
import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class CheckStreamExistTask extends AbstractAsyncTask<String, Void, Boolean> {

	public CheckStreamExistTask(IAsyncTaskListener taskListener,
			Activity context) {
		super(taskListener, context);
		
	}

	private String streamName;

	@Override
	protected Boolean doInBackground(String... params) {
		Boolean result = false;
		
		streamName = params[1];

		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			amfConnection.connect(params[0]);
			result = (Boolean) amfConnection.call("isLiveStreamExist", streamName);

		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {

			e.printStackTrace();
		}
		amfConnection.close();

		return result;
	}

}