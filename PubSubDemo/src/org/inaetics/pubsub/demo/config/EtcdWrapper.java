/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
package org.inaetics.pubsub.demo.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A simple wrapper for the etcd HTTP API
 */
public class EtcdWrapper {

  private final String url;
  private String charset = "UTF-8";
  private ExecutorService executor = Executors.newCachedThreadPool();
  private volatile boolean stopped = false;

  public EtcdWrapper(String url) {
    this.url = url;
  }

  public EtcdWrapper() {
    this.url = "http://localhost:2379/v2/keys";
  }

  /**
   * Get a node from etcd. If wait is true it won't return until an etcd update happened. Cannot be
   * interrupted.
   * 
   * @param key
   * @param wait
   * @param recursive
   * @param index
   * @return
   * @throws MalformedURLException
   * @throws IOException
   */
  public JsonNode get(String key, boolean wait, boolean recursive, long index)
      throws MalformedURLException, IOException {
    String getUrl = url + key;

    if (wait || recursive) { // needs refactoring but works for now
      getUrl += "?";
      if (wait) {
        getUrl += "wait=true";
        if (index > 0) {
          getUrl += "&waitIndex=" + index;
        }
      }

      if (recursive) {
        if (wait) {
          getUrl += "&";
        }
        getUrl += "recursive=true";
      }
    }

    URLConnection connection = new URL(getUrl).openConnection();
    InputStream response = connection.getInputStream();
    JsonNode result = new ObjectMapper().readTree(response);
    response.close();
    AddEtcdIndex(result, Long.parseLong(connection.getHeaderFields().get("X-Etcd-Index").get(0)));
    return result;
  }

  /**
   * Put a node in etcd
   * 
   * @param key
   * @param value
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   */
  public JsonNode put(String key, String value, int timeToLive)
      throws UnsupportedEncodingException, IOException {

    HttpURLConnection connection = (HttpURLConnection) new URL(url + key).openConnection();
    connection.setDoOutput(true); // Triggers POST.
    connection.setRequestMethod("PUT");
    connection.setRequestProperty("Accept-Charset", charset);
    connection.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded;charset=" + charset);

    String query = "";
    if (timeToLive > 0) {
      query = String.format("value=%s&ttl=%d", URLEncoder.encode(value, charset),
          timeToLive);
    } else {
      query = String.format("value=%s", URLEncoder.encode(value, charset));
    }

    try (OutputStream output = connection.getOutputStream()) {
      output.write(query.getBytes(charset));
    }

    InputStream response = connection.getInputStream();
    JsonNode result = new ObjectMapper().readTree(response);
    response.close();


    AddEtcdIndex(result, Long.parseLong(connection.getHeaderFields().get("X-Etcd-Index").get(0)));
    return result;
  }

  public JsonNode refreshTTL(String key, int timeToLive) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url + key).openConnection();
    connection.setDoOutput(true); // Triggers POST.
    connection.setRequestMethod("PUT");
    connection.setRequestProperty("Accept-Charset", charset);
    connection.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded;charset=" + charset);

    String query =
        String.format("refresh=%s&ttl=%s&prevExist=%s", URLEncoder.encode("true", charset),
            URLEncoder.encode(timeToLive + "", charset), URLEncoder.encode("true", charset));

    try (OutputStream output = connection.getOutputStream()) {
      output.write(query.getBytes(charset));
    }

    InputStream response = connection.getInputStream();
    JsonNode result = new ObjectMapper().readTree(response);
    response.close();

    AddEtcdIndex(result, Long.parseLong(connection.getHeaderFields().get("X-Etcd-Index").get(0)));
    return result;
  }

  public JsonNode createDirectory(String key) throws UnsupportedEncodingException, IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url + key).openConnection();
    connection.setDoOutput(true); // Triggers POST.
    connection.setRequestMethod("PUT");
    connection.setRequestProperty("Accept-Charset", charset);
    connection.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded;charset=" + charset);

    String query = String.format("dir=%s", URLEncoder.encode("true", charset));

    try (OutputStream output = connection.getOutputStream()) {
      output.write(query.getBytes(charset));
    }

    InputStream response = connection.getInputStream();
    JsonNode result = new ObjectMapper().readTree(response);
    response.close();

    AddEtcdIndex(result, Long.parseLong(connection.getHeaderFields().get("X-Etcd-Index").get(0)));
    return result;
  }

  /**
   * Delete a node in etcd
   * 
   * @param key
   * @return
   * @throws MalformedURLException
   * @throws IOException
   */
  public JsonNode delete(String key) throws MalformedURLException, IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url + key).openConnection();
    connection.setRequestMethod("DELETE");
    connection.setRequestProperty("Accept-Charset", charset);
    connection.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded;charset=" + charset);

    InputStream response = connection.getInputStream();
    JsonNode result = new ObjectMapper().readTree(response);
    response.close();
    AddEtcdIndex(result, Long.parseLong(connection.getHeaderFields().get("X-Etcd-Index").get(0)));
    return result;
  }

  /**
   * Delete an empty directory in etcd
   * 
   * @param key
   * @return
   * @throws MalformedURLException
   * @throws IOException
   */
  public JsonNode deleteEmptyDirectory(String key) throws MalformedURLException, IOException {
    HttpURLConnection connection =
        (HttpURLConnection) new URL(url + key + "?dir=true").openConnection();
    connection.setRequestMethod("DELETE");
    connection.setRequestProperty("Accept-Charset", charset);
    connection.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded;charset=" + charset);

    InputStream response = connection.getInputStream();
    JsonNode result = new ObjectMapper().readTree(response);
    response.close();
    AddEtcdIndex(result, Long.parseLong(connection.getHeaderFields().get("X-Etcd-Index").get(0)));
    return result;
  }

  /**
   * Wait for a change on the given etcd key.
   * 
   * @param key
   * @param recursive
   * @param index
   * @param callback
   * @throws IOException
   * @throws MalformedURLException
   */
  public void waitForChange(String key, boolean recursive, long index, EtcdCallback callback) {
    EtcdWaiter waiter = new EtcdWaiter(key, recursive, index, callback);
    if (!stopped) {
      executor.submit(waiter);
    }
  }

  public void stop() {
    stopped = true;
    executor.shutdownNow();
  }

  /**
   * This class can wait for an update in etcd. It can be interrupted by calling interrupt.
   *
   */
  private class EtcdWaiter extends Thread {
    private EtcdCallback callback;
    private HttpClient client;
    private HttpGet get;
    private HttpResponse response;
    private HttpEntity entity;
    private InputStream content;
    private String getUrl;
    long index;

    public EtcdWaiter(String key, boolean recursive, long index, EtcdCallback callback) {
      this.callback = callback;
      this.index = index;
      getUrl = url + key + "?wait=true";
      if (index > 0) {
        getUrl += "&waitIndex=" + index;
      }
      if (recursive) {
        getUrl += "&recursive=true";
      }
      
      client = new DefaultHttpClient();
      get = new HttpGet(getUrl);
    }

    @Override
    public void run() {
      try {
        response = client.execute(get);
        entity = response.getEntity();
        content = entity.getContent();
        
        long index = Long.parseLong(response.getHeaders("X-Etcd-Index")[0].getValue());
        
        JsonNode result = new ObjectMapper().readTree(content);

        result = AddEtcdIndex(result, index);
        callback.onResult(result);
      } catch (Exception e) {
        e.printStackTrace();
        callback.onException(e);
      } finally {
        try {
          content.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          
        }
      }
    }

    @Override
    public void interrupt() {
      get.abort();
      try {
        content.close();
      } catch (IOException e) {
        e.printStackTrace();
        callback.onException(e);
      }
    }
  }

  private static JsonNode AddEtcdIndex(JsonNode node, long index) {
    ObjectNode objectNode = (ObjectNode) node;
    objectNode.put("EtcdIndex", index);
    return objectNode;
  }
}
