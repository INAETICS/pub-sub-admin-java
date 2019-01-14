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
package org.inaetics.pubsub.impl.discovery.etcd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.Header;
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
    private final String DEFAULT_URL = "http://localhost:2379/v2/keys";

    private final String url;
    private String charset = "UTF-8";
    private ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean stopped = false;

    public class ResultEntry {
        public final String key;
        public final String value;

        protected ResultEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public class EtcdResult {
        public final String action; // "get", "expire", "update", "set", "delete"
        public final List<ResultEntry> entries;
        public final long nextWaitIndex;

        protected EtcdResult(String action, List<ResultEntry> entries, long nextWaitIndex) {
            this.action = action;
            this.entries = entries;
            this.nextWaitIndex = nextWaitIndex;
        }
    }

    public EtcdWrapper(String url) {
        this.url = url;
    }

    public EtcdWrapper() {
        this.url = DEFAULT_URL;
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
    public EtcdResult get(String key, boolean wait, boolean recursive, long index)
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
        String strIndex = connection.getHeaderField("X-Etcd-Index");
        long nextWaitForIndex = strIndex != null ? Long.parseLong(strIndex) + 1 : -1L;
        JsonNode result = new ObjectMapper().readTree(response);
        response.close();

        return parseJSONResponse(nextWaitForIndex, result);
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
    public void put(String key, String value, int timeToLive)
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
    }

    public void refreshTTL(String key, int timeToLive) throws IOException {
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
    }

    /**
     * Delete a node in etcd
     *
     * @param key
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public void delete(String key) throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url + key).openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded;charset=" + charset);

        InputStream response = connection.getInputStream();
        JsonNode result = new ObjectMapper().readTree(response);
        response.close();
    }


    private EtcdResult parseJSONResponse(long nextWaitForIndex, JsonNode response) {
        JsonNode action = response.get("action");
        JsonNode rootNode = response.get("node");
        List<ResultEntry> entries = new ArrayList<>();
        findEntries(entries, rootNode);

        if (action != null && action.isTextual()) {
            return new EtcdResult(action.asText(), entries, nextWaitForIndex);
        } else {
            return null;
        }
    }

    private void findEntries(List<ResultEntry> entries, JsonNode node) {
        if (node != null && node.isObject()) {
            JsonNode dir = node.get("dir");
            boolean isDir = dir != null && dir.asBoolean(false);
            if (isDir) {
                JsonNode nodes = node.get("nodes");
                if (nodes != null && nodes.isArray()) {
                    for (int i = 0; i < nodes.size(); ++i) {
                        JsonNode child = nodes.get(i);
                        findEntries(entries, child);
                    }
                }
            } else {
                JsonNode key = node.get("key");
                JsonNode value = node.get("value");
                if (key != null && key.isTextual()) {
                    String v = value != null && value.isTextual() ? value.asText() : null;
                    entries.add(new ResultEntry(key.asText(), v));
                }
            }
        }
    }

    /**
     * Wait for a change on the given etcd key.
     * TODO make interruptable
     *
     * @param key
     * @param recursive
     * @param index
     * @throws IOException
     * @throws MalformedURLException
     */
    public EtcdResult waitForChange(String key, boolean recursive, long index) throws IOException {
        String keyUrl = url + key + "?wait=true";
        if (index > 0) {
            keyUrl += "&waitIndex=" + index;
        }
        if (recursive) {
            keyUrl += "&recursive=true";
        }

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(keyUrl);


        InputStream content = null;
        long nextWaitIndex = -1L;
        JsonNode result = null;
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            content = entity.getContent();

            Header[] headers = response.getHeaders("X-Etcd-Index");
            if (headers.length > 0) {
                nextWaitIndex = Long.parseLong(headers[0].getValue());
                result = new ObjectMapper().readTree(content);
            }
        } finally {
            if (content != null) {
                content.close();
            }
        }
        return parseJSONResponse(nextWaitIndex, result);
    }
}
