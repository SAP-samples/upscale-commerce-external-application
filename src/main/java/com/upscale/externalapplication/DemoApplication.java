/**
 * 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.upscale.externalapplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * POC for an Upscale External Application
 * 
 * It has 2 endpoints 
 *  - GET /locations
 *  - GET /inventory
 */

@SpringBootApplication
@RestController
@EnableScheduling
public class DemoApplication {

	private static final Logger LOG = LoggerFactory.getLogger(DemoApplication.class);

	/**
	 * Upscale generated clientId - Workbench > Configuration > Applications > External Application
	 * 
	 * Unilever note - replace .../client-id field in application.properties
	 */
	@Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
	private String clientId;

	/**
	 * Upscale generated clientSecret Workbench > Configuration > Applications > External Application
	 * 
	 * Unilever note - replace .../client-secret field in application.properties
	 */
	@Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
	private String clientSecret;

	/**
	 * API key
	 */
	@Value("${spring.sendgrid.api-key}")
	private String apiKey;


	/**
	 * Bearer token to set as header on Upscale service requests
	 */
	private String bearerToken;
	
	/**
	 * Tenant URL
	 */
	private String tenant = "<tenant-name>-approuter-caas2-sap-<environment>.cfapps.us10.hana.ondemand.com";

	@PostConstruct
	public void postConstruct() {
		this.setToken();
	}

	@Scheduled(cron = "0 0 0/8 1/1 * ?")
	public void scheduled() {
		this.setToken();
	}

	/**
	 *	Local debug http://127.0.0.1:7888/locations
	 */
	@GetMapping(value = "/locations")
	public ResponseEntity<String> getMethodName(
		@RequestParam(required = false) List<String> ids, 
		@RequestHeader Map<String, String> headers
	) throws Exception {

		try {
			this.checkApiKey(headers.get("x-api-key"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad request");
		}
		
		return this.getLocations(ids);
	}

	/**
	 *   http://127.0.0.1:7888/inventory
	 */
	@GetMapping(value = "/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getMethodName(@RequestParam String id, @RequestHeader Map<String, String> headers) throws Exception {
		
		try {
			this.checkApiKey(headers.get("x-api-key"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad request");
		}
		
		return this.getInventory(id);
	}

	private ResponseEntity<String> getInventory(String id) {
		String url = "https://" + this.tenant + "/inventory-service/inventory"
			+ "?expand=location" 
			+ "&location.fulfillmentLocationTypes=" + "FULFILLMENT_STORE,STORE"
			+ "&productIds=" + id;


		try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(url);
			// Set authorization and content-type headers
			httpGet.addHeader(HttpHeaders.AUTHORIZATION, this.bearerToken);
			httpGet.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

			CloseableHttpResponse response = httpclient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == HttpStatus.OK.value()) {
				// HttpHeaders responseHeaders = new HttpHeaders();
				// responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, expectedOrigin);

				ResponseEntity<String> responseEntity = 
					ResponseEntity
						.ok()
						.body(EntityUtils.toString(response.getEntity()));

				return responseEntity;

			} else if (response.getStatusLine().getStatusCode() == HttpStatus.NOT_FOUND.value()) {
				return ResponseEntity.notFound().build();
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
			}
		} catch (final IOException e) {
			LOG.error("Error getting inventory", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

	}

	private ResponseEntity<String> getLocations(List<String> ids) throws Exception {
		String url = "https://" + this.tenant + "/inventory-service/locations";

		if ((ids != null) && !ids.isEmpty())  {
			url = url + "?fulfillmentLocationIds="+ String.join(",", ids);
		}

		try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(url);
			// Set authorization and content-type headers
			httpGet.addHeader(HttpHeaders.AUTHORIZATION, this.bearerToken);
			httpGet.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

			CloseableHttpResponse response = httpclient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == HttpStatus.OK.value()) {
				// HttpHeaders responseHeaders = new HttpHeaders();
				// responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, expectedOrigin);

				ResponseEntity<String> responseEntity = 
					ResponseEntity
						.ok()
						// .headers(responseHeaders)
						.body(EntityUtils.toString(response.getEntity()));

				return responseEntity;
			} else if (response.getStatusLine().getStatusCode() == HttpStatus.NOT_FOUND.value()) {
				return ResponseEntity.notFound().build();
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
			}
		} catch (final IOException e) {
			LOG.error("Error getting locations", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

	}

	/**
	 * Util method to set bearer token
	 * 
	 * @throws Exception
	 */
	private void setToken() {
		String url = "https://" + this.tenant + "/oauth2/token";


		try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
			final HttpPost httpPost = new HttpPost(url);

			// set request body
			final List<NameValuePair> nameValuePairs = new ArrayList<>(3);
			nameValuePairs.add(new BasicNameValuePair("grant_type", "client_credentials"));
			nameValuePairs.add(new BasicNameValuePair("client_id", this.clientId));
			nameValuePairs.add(new BasicNameValuePair("client_secret", this.clientSecret));

			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			final CloseableHttpResponse response = httpclient.execute(httpPost);

			final JSONObject jsonObj = new JSONObject(EntityUtils.toString(response.getEntity()));

			// set token field
			this.bearerToken = "Bearer " + jsonObj.getString("access_token");
		} catch (final IOException e) {
			LOG.error("Error getting token", e);
		}
	}

	private void checkApiKey(String apiKey) throws Exception {
		if (!this.apiKey.equals(apiKey)) {
			throw new Exception("Bad request");
		}
		return;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}