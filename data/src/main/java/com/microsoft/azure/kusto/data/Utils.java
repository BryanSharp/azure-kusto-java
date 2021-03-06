// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.azure.kusto.data;

import com.microsoft.azure.kusto.data.exceptions.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

class Utils {

    static KustoOperationResult post(String url, String payload, InputStream stream, Integer timeoutMs, HashMap<String, String> headers, boolean leaveOpen) throws DataServiceException, DataClientException {
        HttpClient httpClient;
        if (timeoutMs != null) {
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeoutMs).build();
            httpClient = HttpClientBuilder.create().useSystemProperties().setDefaultRequestConfig(requestConfig).build();
        } else {
            httpClient = HttpClients.createSystem();
        }

        try (InputStream ignored = (stream != null && !leaveOpen) ? stream : null) {
            URL cleanUrl = new URL(url);
            URI uri = new URI(cleanUrl.getProtocol(), cleanUrl.getUserInfo(), cleanUrl.getHost(), cleanUrl.getPort(), cleanUrl.getPath(), cleanUrl.getQuery(), cleanUrl.getRef());

            // Request parameters and other properties. We use UncloseableStream to prevent HttpClient From closing it
            HttpEntity requestEntity = (stream == null) ? new StringEntity(payload, ContentType.APPLICATION_JSON)
                    : new InputStreamEntity(new UncloseableStream(stream));

            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(requestEntity);
            httpPost.addHeader("Accept-Encoding", "gzip,deflate");
            httpPost.addHeader("Accept", "application/json");
            for (HashMap.Entry<String, String> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }

            // Execute and get the response.
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                StatusLine statusLine = response.getStatusLine();
                String responseContent = EntityUtils.toString(entity);
                if (statusLine.getStatusCode() == 200) {
                    return new KustoOperationResult(responseContent, url.endsWith("v2/rest/query") ? "v2" : "v1");
                }
                else {
                    if(StringUtils.isBlank(responseContent)){
                        responseContent = response.getStatusLine().toString();
                    }
                    String message = "";
                    DataWebException ex = new DataWebException(responseContent, response);
                    try {
                        message = ex.getApiError().getDescription();
                    } catch (Exception ignored1) { }
                    throw new DataServiceException(url, message, ex);
                }
            }
        } catch (JSONException | IOException | URISyntaxException | KustoServiceError e) {
            throw new DataClientException(url, "Error in post request:" + e.getMessage(), e);
        }
        return null;
    }

    static String GetPackageVersion() {
        try {
            Properties props = new Properties();
            try (InputStream versionFileStream = Utils.class.getResourceAsStream("/app.properties")) {
                props.load(versionFileStream);
                return props.getProperty("version").trim();
            }
        } catch (Exception ignored) {}
        return "";
    }
}
