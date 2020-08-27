package com.jeffdisher.thinktank;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;
import org.junit.Test;


public class ThinkTankRestTest {
	private static final String MEMBRANE_URL = "http://localhost:8080/";

	@Test
	public void testStaticResources() throws Throwable {
		ThinkTankWrapper membrane = ThinkTankWrapper.localWrapper();
		
		HttpClient httpClient = new HttpClient();
		httpClient.start();
		String content = _sendRequest(httpClient, HttpMethod.GET, MEMBRANE_URL + "likeness.js", null);
		Assert.assertNotNull(content);
		content = _sendRequest(httpClient, HttpMethod.GET, MEMBRANE_URL + "chat.html", null);
		Assert.assertNotNull(content);
		content = _sendRequest(httpClient, HttpMethod.GET, MEMBRANE_URL + "auth.html", null);
		Assert.assertNotNull(content);
		httpClient.stop();
		
		membrane.stop();
	}


	private String _sendRequest(HttpClient httpClient, HttpMethod method, String url, String message) throws Throwable {
		Request request = httpClient.newRequest(url);
		request.method(method);
		if (null != message) {
			request.content(new StringContentProvider(message));
		}
		String content = new String(request.send().getContent(), StandardCharsets.UTF_8);
		return content;
	}


	private static class ThinkTankWrapper {
		public static ThinkTankWrapper localWrapper() throws InterruptedException {
			CountDownLatch latch = new CountDownLatch(1);
			Thread runner = new Thread(() -> {
				ThinkTankRest.mainInTest(latch, new String[] {"--local_only"});
			});
			runner.start();
			latch.await();
			return new ThinkTankWrapper(runner);
		}
		
		private final Thread _runner;
		private ThinkTankWrapper(Thread runner) {
			_runner = runner;
		}
		public void stop() throws MalformedURLException, IOException, InterruptedException {
			HttpURLConnection connection = (HttpURLConnection)new URL("http://localhost:8080/exit").openConnection();
			connection.setRequestMethod("DELETE");
			// Note that sometimes the shutdown happens so quickly that we don't even get a response so just force the send.
			connection.getContentLength();
			connection.disconnect();
			_runner.join();
		}
	}
}
