package org.red5.core;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.mina.proxy.utils.StringUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.red5.core.dbModel.GcmUserMails;
import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.RegIds;
import org.red5.core.dbModel.StreamProxy;
import org.red5.core.dbModel.StreamViewers;
import org.red5.core.dbModel.Streams;
import org.red5.core.manager.StreamManager;
import org.red5.core.manager.StreamProxyManager;
import org.red5.core.manager.UserManager;
import org.red5.core.utils.Red5Timer;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.ISubscriberStream;
import org.slf4j.Logger;

import antlr.StringUtils;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

/**
 * Sample application that uses the client manager.murat
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class Application extends MultiThreadedApplicationAdapter implements IWebService {

	private static Logger log = Red5LoggerFactory.getLogger(Application.class);

	private static final String SENDER_ID = "AIzaSyCFmHIbJO0qCtPo6klp7Ade3qjeGLgtZWw";
	private static final String WEB_PLAY_URL = "http://www.butterflytv.net/player.html?videoId=";
	private ResourceBundle messagesTR;
	private ResourceBundle messagesEN;
	private java.util.Timer streamDeleterTimer;
	private static long MILLIS_IN_HOUR = 60 * 60 * 1000;
	public UserManager userManager;
	public StreamManager streamManager;
	public StreamProxyManager streamProxyManager;
	private boolean isNotificationSent;

	public Application() {
		messagesTR = ResourceBundle.getBundle("resources/LanguageBundle",
				new Locale("tr"));
		messagesEN = ResourceBundle.getBundle("resources/LanguageBundle");
		userManager = new UserManager();
		streamManager = new StreamManager();
		streamProxyManager = new StreamProxyManager();

		scheduleStreamDeleterTimer(1 * MILLIS_IN_HOUR, 24 * MILLIS_IN_HOUR);

		log.info("app started");
	}

	public void cancelStreamDeleteTimer() {
		if (streamDeleterTimer != null) {
			streamDeleterTimer.cancel();
			streamDeleterTimer = null;
		}
	}

	public void scheduleStreamDeleterTimer(long runPeriod, final long deleteTime) {
		TimerTask streamDeleteTask = new Red5Timer(this, deleteTime);
		cancelStreamDeleteTimer();
		streamDeleterTimer = new java.util.Timer();
		streamDeleterTimer.schedule(streamDeleteTask, 0, runPeriod);
	}

	public void removeTimeUpStreams(String key) {
		Streams stream = streamManager.getStream(key);
		if (stream != null)
			streamManager.deleteStream(stream);

		streamProxyManager.removeProxyStream(key);
	}
	@Override
	public void appStop(IScope arg0) {
		log.info("app stop");
		super.appStop(arg0);
		cancelStreamDeleteTimer();

	}

	/** {@inheritDoc} */
	@Override
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		log.info("app connect");
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect(IConnection conn, IScope scope) {
		super.disconnect(conn, scope);
	}

	public String getLiveStreams(String mails) {
		return getLiveStreams(mails, "0", "10");
	}

	public String getLiveStreams(String mails, String start, String batchSize) {
		List<String> mailList = null;
		if (mails != null) {
			String[] mailArray = mails.split(",");
			mailList = new ArrayList<String>(Arrays.asList(mailArray));
		}
		List<Streams> streamList = streamManager.getLiveStreams(
				streamProxyManager.getLiveStreamProxies(), mailList, start, batchSize);
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;

		for (Streams stream : streamList) {
			jsonObject = new JSONObject();
			jsonObject.put("url", stream.getStreamUrl());
			jsonObject.put("name", stream.getStreamName());
			jsonObject.put("viewerCount", this.getViewerCount(stream.getStreamUrl()));
			jsonObject.put("latitude", stream.getLatitude());
			jsonObject.put("longitude", stream.getLongitude());
			jsonObject.put("altitude", stream.getAltitude());
			jsonObject.put("isLive", stream.getIsLive());
			jsonObject.put("isPublic", stream.getIsPublic());

			boolean isMailAdded = isBroadcasterMailAdded(mailList, stream);

			if (isMailAdded == true) {
				StringBuilder sb = getMailCommaSeparated(stream.getGcmUsers().getGcmUserMailses());
				jsonObject.put("broadcasterMail", sb.toString());
			}


			jsonObject.put("isDeletable",
					streamManager.isDeletable(stream, mailList));
			jsonObject.put("registerTime", stream.getRegisterTime().getTime());
			jsonArray.add(jsonObject);
		}

		return jsonArray.toString();
	}

	private StringBuilder getMailCommaSeparated(Set<GcmUserMails> userMailSet) {
		Iterator<GcmUserMails> it = userMailSet.iterator();
		StringBuilder sb = new StringBuilder();
		while (it.hasNext()) {
			GcmUserMails gcmUserMail = (GcmUserMails) it.next();
			if (sb.length() > 0) sb.append(',');
			sb.append(gcmUserMail.getMail());

		}
		return sb;
	}

	/**
	 * Determines if the broadcaster mail will be added to stream list
	 * @param mailList
	 * mail list of a user who fetches the stream
	 * @param stream
	 * if the user who has the maillist in this functions, can see the broadcaster mail of this stream 
	 * @return
	 * true if broadcaster shared stream with that user or if the user and the broadcaster are the same
	 * false otherwise
	 */
	private boolean isBroadcasterMailAdded(List<String> mailList, Streams stream) {
		boolean addBroadcasterMail = false;
		
		if (mailList != null) {
			
			List<StreamViewers> streamViewers = streamManager.getStreamViewers(stream);
			for (StreamViewers viewer : streamViewers) {
				addBroadcasterMail = isMailExistInSet(mailList, viewer.getGcmUsers().getGcmUserMailses());
				if (addBroadcasterMail == true) {
					break;
				}
			}

			if (addBroadcasterMail == false) {
				addBroadcasterMail = isMailExistInSet(mailList, stream.getGcmUsers().getGcmUserMailses());
			}
		}
		return addBroadcasterMail;

	}



	private boolean isMailExistInSet(List<String> mailList, Set<GcmUserMails> gcmUserMailSet) {
		boolean exist = false;
		if (mailList != null && gcmUserMailSet != null) 
		{
			for (int i = 0; i < mailList.size(); i++) {
				for (GcmUserMails userMail : gcmUserMailSet) {
					if (userMail.getMail().equals(mailList.get(i))) {
						exist = true;
						break;
					}
				}

				if (exist == true) {
					break;
				}
			}
		}
		return exist;
	}

	public boolean isLiveStreamExist(String url) {

		IScope target = Red5.getConnectionLocal().getScope();
		Set<String> streamNames = getBroadcastStreamNames(target);

		return streamManager.isLiveStreamExist(url, streamNames);

	}

	public boolean registerLiveStream(String streamName, String url,
			String mailsToBeNotified, String broadcasterMail, boolean isPublic,
			String deviceLanguage) {
		Map<String, StreamProxy> registeredStreams = streamProxyManager.getLiveStreamProxies();
		GcmUsers user = this.userManager.getGcmUserByEmails(broadcasterMail);
		boolean result = false;
		if (registeredStreams.containsKey(url) == false) {

			Streams stream = new Streams(user, Calendar.getInstance().getTime(),
					streamName, url);
			stream.setIsPublic(isPublic);
			stream.setIsLive(true);

			StreamProxy proxy = streamManager.registerLiveStream(
					url, mailsToBeNotified, stream);
			if(proxy != null)
			{
				registeredStreams.put(url, proxy);
				result = true;
			}
		}

		String firstMail = getFirstEmail(broadcasterMail);

		if (result)
			this.sendNotificationsOrMail(mailsToBeNotified, firstMail,
					url, streamName, deviceLanguage);
		return result;
	}

	private String getFirstEmail(String broadcasterMail) {
		String[] mails = broadcasterMail.split(",");
		String firstMail = "";
		if(mails.length > 0)
			firstMail = mails[0];
		return firstMail;
	}

	public boolean registerLocationForStream(String url, double longitude,
			double latitude, double altitude) {
		return streamManager.registerLocationForStream(url, longitude,
				latitude, altitude);
	}

	/**
	 * register user to database
	 * 
	 * @param register_id
	 *            gcm registration id
	 * @param mail
	 *            mail adress of the user
	 * @return
	 */
	public boolean registerUser(String register_id, String mail) {
		return userManager.registerUser(register_id, mail);

	}

	/**
	 * @param register_id
	 *            new register id
	 * @param mail
	 *            user mail
	 * @param oldRegID
	 *            old register id
	 * @return true if the user is updated succesfully , false if fails
	 */
	public boolean updateUser(String register_id, String mail, String oldRegID) {
		return userManager.updateUser(register_id, mail, oldRegID);

	}

	/**
	 * 
	 * @param mails
	 *            , The mail address to be notified that a video stream is
	 *            shared with them
	 * @param broadcasterMail
	 *            The mail address of broadcaster
	 * @param streamURL
	 *            published name of the stream in Red5
	 * @param deviceLanguage
	 *            language of the device. According to this parameter,
	 *            notification mail language is selected
	 * @param deviceLanguage
	 */
	public void sendNotificationsOrMail(String mails, String broadcasterMail,
			String streamURL, String streamName, String deviceLanguage) {

		Set<RegIds> result = null;

		// This List will be used for mails, which are not available on the
		// database
		ArrayList<String> mailListNotifiedByMail = new ArrayList<String>();

		// This List will be used for registerIds, which are available on the
		// database
		Set<RegIds> regIdSet = new HashSet<RegIds>();
		if (mails != null) {
			String[] splits = mails.split(",");

			for (int i = 0; i < splits.length; i++) {
				result = getRegistrationIdList(splits[i]);
				if (result == null) {

					// using as a parameter for sendMail() function
					mailListNotifiedByMail.add(splits[i]);
				} else {
					// using as a parameter for sendNotification() function
					regIdSet.addAll(result);
				}
			}
			// JPAUtils.closeEntityManager();

			if (!mailListNotifiedByMail.isEmpty())
				sendMail(mailListNotifiedByMail, broadcasterMail, streamName,
						streamURL, deviceLanguage);

			if (regIdSet.size() > 0)
				sendNotification(regIdSet, broadcasterMail, streamURL,
						streamName, deviceLanguage);
		}
	}

	/**
	 * @param mail
	 * @return user with reg ids of mail in the table if mail is not exist, null
	 *         returns else return GcmUsers of mail
	 */
	public Set<RegIds> getRegistrationIdList(String mail) {
		return userManager.getRegistrationIdList(mail);
	}

	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		String streamUrl = stream.getPublishedName();
		// getPublishedName means streamurl to us

		removeStream(streamUrl);
		super.streamBroadcastClose(stream);
	}

	@Override
	public void streamPublishStart(IBroadcastStream stream) {

		stream.addStreamListener(streamProxyManager);
		super.streamPublishStart(stream);
	}

	public boolean removeStream(String streamUrl) {
		Map<String, StreamProxy> registeredLiveStreams = streamProxyManager.getLiveStreamProxies();
		return streamManager.removeStream(streamUrl, registeredLiveStreams);
	}

	public void sendMail(final ArrayList<String> email, String broadcasterMail,
			String streamName, String streamURL, String deviceLanguage) {
		setNotificationSent(false);
		ResourceBundle messages = messagesEN;
		if (deviceLanguage != null && deviceLanguage.equals("tur")) {
			messages = messagesTR;
		}

		String webURL = WEB_PLAY_URL + streamURL;
		final String subject = messages.getString("mail_notification_subject");
		final String messagex = MessageFormat.format(
				messages.getString("mail_notification_message"),
				broadcasterMail, streamName, webURL);

		Thread mailSenderThread = new Thread() {
			@Override
			public void run() {
				final String username = "notification@butterflytv.net";
				final String password = "Nybn~Dx-E5-$";
				Properties props = new Properties();
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.host", "mail.butterflytv.net");
				props.put("mail.smtp.port", "26");
				System.out.println("Done1");
				Session session = Session.getInstance(props,
						new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username,
								password);
					}
				});

				try {
					javax.mail.Message message = new MimeMessage(session);
					message.setFrom(new InternetAddress(username));
					System.out.println("Done2");
					for (int i = 0; i < email.size(); i++) {
						System.out.println("Done3");
						message.setRecipients(
								javax.mail.Message.RecipientType.TO,
								InternetAddress.parse(email.get(i)));
						message.setSubject(subject);
						message.setContent(messagex, "text/html; charset=utf-8");
						// message.setText(broadcasterMail);
						System.out.println("Done4");
						Transport.send(message);
						System.out.println("Done5");
					}

					System.out.println("Done");
				} catch (MessagingException e) {
					System.out.println(e.getMessage());
				}
				setNotificationSent(true);
			}
		};
		mailSenderThread.start();

	}

	private void sendNotification(final Set<RegIds> regIdSet,
			final String broadcasterMail, final String streamURL,
			final String streamName, final String deviceLanguage) {
		setNotificationSent(false);
		Thread notifSender = new Thread() {
			public void run() {
				// Instance of com.android.gcm.server.Sender, that does the
				// transmission of a Message to the Google Cloud Messaging
				// service.
				Sender sender = new Sender(SENDER_ID);

				// This Message object will hold the data that is being
				// transmitted
				// to the Android client devices. For this demo, it is a simple
				// text
				// string, but could certainly be a JSON object.
				Message message = new Message.Builder()

				// If multiple messages are sent using the same
				// .collapseKey()
				// the android target device, if it was offline during
				// earlier
				// message
				// transmissions, will only receive the latest message
				// for that
				// key when
				// it goes back on-line.
				.collapseKey("1").timeToLive(30).delayWhileIdle(true)
				.addData("URL", streamURL)
				.addData("broadcaster", broadcasterMail)
				.addData("name", streamName).build();

				ArrayList<String> failedNotificationMails = new ArrayList<String>();

				try {
					// use this for multicast messages. The second parameter
					// of sender.send() will need to be an array of register
					// ids.
					List<String> targetRegIDList = new ArrayList<String>();
					for (RegIds regIds : regIdSet) {
						targetRegIDList.add(regIds.getGcmRegId());
					}

					MulticastResult result = sender.send(message,
							targetRegIDList, 1);

					List<Result> resultList = result.getResults();
					if (resultList != null) {
						int canonicalRegId = result.getCanonicalIds();
						for (int i = 0; i < resultList.size(); i++) {
							Result innerResult = resultList.get(i);
							if (innerResult.getMessageId() != null) {
								if (canonicalRegId != 0) {

									String canoID = innerResult
											.getCanonicalRegistrationId();
									String oldRegID = targetRegIDList.get(i);
									GcmUsers user = userManager
											.getGcmUserByRegId(oldRegID);

									Set<GcmUserMails> gcmUserMailses = user
											.getGcmUserMailses();
									GcmUserMails userMail = gcmUserMailses
											.iterator().next();

									if (userMail != null
											&& userMail.getMail() != null) {
										updateUser(canoID, userMail.getMail(),
												oldRegID);
									}
								}

							} else {
								String error = innerResult.getErrorCodeName();
								if (error
										.equals(Constants.ERROR_NOT_REGISTERED)) {
									// application has been removed from device
									// -
									// unregister database
									String oldRegID = targetRegIDList.get(i);
									GcmUsers user = userManager
											.getGcmUserByRegId(oldRegID);
									Set<GcmUserMails> gcmUserMailses = user
											.getGcmUserMailses();
									GcmUserMails userMail = gcmUserMailses
											.iterator().next();

									if (userMail != null
											&& userMail.getMail() != null) {
										if (!failedNotificationMails
												.contains(userMail.getMail()))
											failedNotificationMails
											.add(userMail.getMail());
									}
									deleteRegId(oldRegID);
								}
							}
						}

					} else {
						int error = result.getFailure();
						System.out.println("Broadcast failure: " + error);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				if (failedNotificationMails.size() > 0)
					sendMail(failedNotificationMails, broadcasterMail,
							streamName, streamURL, deviceLanguage);

				// We'll pass the CollapseKey and Message values back to
				// index.jsp, only
				// so
				// we can display it in our form again
				System.out.println("OK");
				setNotificationSent(true);
			};
		};

		notifSender.start();

	}

	public boolean deleteRegId(String oldRegID) {
		return userManager.deleteRegId(oldRegID);
	}

	@Override
	public void streamPlayItemPlay(ISubscriberStream subscriberStream,
			IPlayItem item, boolean isLive) {
		super.streamPlayItemPlay(subscriberStream, item, isLive);

		String name = item.getName();
		Map<String, StreamProxy> registeredStreams = streamProxyManager.getLiveStreamProxies();

		if (registeredStreams.containsKey(name)) {
			StreamProxy streamProxy = registeredStreams.get(name);
			streamProxy.addViewer(subscriberStream.getName());

			Streams stream = streamManager.getStream(name);

			notifyUserAboutViewerCount(getViewerCount(stream.getStreamUrl()),
					stream.getGcmUsers().getRegIdses());
		}

	}

	private void notifyUserAboutViewerCount(int viewerCount,
			Set<RegIds> regIdset) {

		Sender sender = new Sender(SENDER_ID);

		Message message = new Message.Builder().collapseKey("1").timeToLive(30)
				.delayWhileIdle(true)
				.addData("viewerCount", String.valueOf(viewerCount)).build();

		try {
			List<String> regIds = new ArrayList<String>();
			for (RegIds regIDs : regIdset) {
				regIds.add(regIDs.getGcmRegId());
			}
			if (regIds.size() >= 0) {
				MulticastResult result = sender.send(message, regIds, 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void streamSubscriberClose(ISubscriberStream subcriberStream) {
		super.streamSubscriberClose(subcriberStream);

		Set<Entry<String, StreamProxy>> entrySet = streamProxyManager.getLiveStreamProxies()
				.entrySet();
		for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<String, StreamProxy> entry = (Entry<String, StreamProxy>) iterator
					.next();
			StreamProxy value = entry.getValue();
			if (value.containsViewer(subcriberStream.getName())) {
				value.removeViewer(subcriberStream.getName());

				Streams stream = streamManager.getStream(value.streamUrl);

				notifyUserAboutViewerCount(
						getViewerCount(stream.getStreamUrl()), stream
						.getGcmUsers().getRegIdses());
				break;
			}

		}
	}


	/**
	 * This methods gets image in byte array and saves as a file in the server
	 * 
	 * @param data
	 * @param streamURL
	 */
	public boolean savePreview(byte[] data, String streamURL) {
		boolean result = true;
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
			File outputfile = new File("webapps/ButterFly_Red5/" + streamURL
					+ ".png");
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		return result;
	}

	public boolean isNotificationSent() {
		return isNotificationSent;
	}

	private void setNotificationSent(boolean mailsSent) {
		this.isNotificationSent = mailsSent;
	}

	public synchronized int getViewerCount(String broadcastUrl) {
		IConnection conn = Red5.getConnectionLocal();
		int count = 0;
		try {
			count = conn.getScope().getBroadcastScope(broadcastUrl)
					.getConsumers().size();
			log.info("viewer number for " + broadcastUrl + " is " + count);
		} catch (Exception e) {
			log.debug(e.getMessage());
		}
		return count;
	}

	public boolean deleteStream(String url) {
		log.info("Stream to be deleted url is " + url);
		Streams stream = streamManager.getStream(url);
		if (stream == null) {
			log.info("Stream to be deleted is null");
			return false;
		}
		boolean result = streamManager.deleteStream(stream);
		if (result) {
			deleteStreamFiles(url);
		}
		return result;
	}

	public void deleteStreamFiles(String url) {
		File dirStream = new File("webapps/ButterFly_Red5/streams");
		File dirPreview = new File("webapps/ButterFly_Red5");

		File fStream = new File(dirStream, url + ".flv");
		File fPreview = new File(dirPreview, url + ".png");
		if (fStream.isFile() == true && fStream.exists() == true) {
			fStream.delete();
		}

		if (fPreview.isFile() == true && fPreview.exists() == true) {
			fPreview.delete();
		}
	}
}
