/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.test.media;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.test.base.BrowserMediaApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.Recorder;
import com.kurento.kmf.test.client.WebRtcChannel;

/**
 * <strong>Description</strong>: WebRTC in loopback.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browsers start before 60 seconds (default timeout)</li>
 * <li>Color received by client should be green (RGB #008700, video test of
 * Chrome)</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */

public class MediaApiWebRtcTest extends BrowserMediaApiTest {

	private static int PLAYTIME = 10; // seconds to play in HTTP player

	@Test
	public void testWebRtcLoopbackChrome() throws InterruptedException {
		doTest(Browser.CHROME, getPathTestFiles() + "/video/10sec/red.y4m",
				"http://files.kurento.org/audio/10sec/fiware.wav", Color.RED);
	}

	public void doTest(Browser browserType, String video, String audio,
			Color color) throws InterruptedException {
		MediaPipeline mp = pipelineFactory.create();
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		webRtcEndpoint.connect(webRtcEndpoint);

		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);
		if (video != null) {
			builder = builder.video(video);
		}
		if (audio != null) {
			builder = builder.audio(audio).recordAudio(PLAYTIME);
		}

		try (BrowserClient browser = builder.build()) {
			browser.subscribeEvents("playing");
			browser.connectToWebRtcEndpoint(webRtcEndpoint,
					WebRtcChannel.AUDIO_AND_VIDEO);

			// Wait until event playing in the remote stream
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));

			// Guard time to play the video
			Thread.sleep(PLAYTIME * 1000);

			// Assert play time
			double currentTime = browser.getCurrentTime();
			Assert.assertTrue("Error in play time of HTTP player (expected: "
					+ PLAYTIME + " sec, real: " + currentTime + " sec)",
					compare(PLAYTIME, currentTime));

			// Assert color
			if (color != null) {
				Assert.assertTrue("The color of the video should be " + color,
						browser.colorSimilarTo(color));
			}

			// Assert audio quality
			if (audio != null) {
				float minPesqMos = 1.5F;
				float realPesqMos = Recorder.getPesqMos(audio);
				Assert.assertTrue(
						"Bad perceived audio quality: PESQ MOS too low (expected="
								+ String.valueOf(minPesqMos) + ", real="
								+ String.valueOf(realPesqMos) + ")",
						realPesqMos >= minPesqMos);
			}

		}
	}
}
