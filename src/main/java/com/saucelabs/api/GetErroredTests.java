package com.saucelabs.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import io.restassured.path.json.JsonPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GetErroredTests
{
	static String TESTS_ENDPOINT = "https://saucelabs.com/rest/v1/analytics/tests";

	static String USERNAME = System.getProperty("USERNAME", System.getenv("SAUCE_USERNAME"));
	static String ACCESS_KEY = System.getProperty("ACCESS_KEY", System.getenv("SAUCE_ACCESS_KEY"));
	static String TIME_RANGE = System.getProperty("TIME_RANGE", "-1d"); // -1d, -1h, -1m, -1s; maximum -29d
	static String SCOPE = System.getProperty("SCOPE", "me"); // me, organization, single
	static String STATUS = System.getProperty("STATUS", "errored"); // errored, complete, passed, failed
	static String SHOW_TESTS = System.getProperty("SHOW_TESTS", "false"); // true, false
	static String ERROR_MESSAGES = System.getProperty("ERROR_MESSAGES", "Test did not see a new command,Internal Server Error,Infrastructure Error");

	public static void main(String[] args) throws UnirestException
	{
		System.out.println("USERNAME: " + USERNAME);
		System.out.println("ACCESS_KEY: " + ACCESS_KEY);
		System.out.println("TIME_RANGE:" + TIME_RANGE);
		System.out.println("SCOPE:" + SCOPE);
		System.out.println("STATUS: " + STATUS);
		System.out.println("SHOW_TESTS: " + SHOW_TESTS);
		System.out.println("ERROR_MESSAGES: " + ERROR_MESSAGES);

		boolean showTests = Boolean.parseBoolean(SHOW_TESTS);
		System.out.println(showTests);

		List<String> errorMessages = Arrays.asList(ERROR_MESSAGES.split(","));
		System.out.println(errorMessages);

		int from = 0; //starting test sequence
		int size = 100;
		int max = 10000;
		boolean has_more;

		List<TestResponse> allErrors = new ArrayList<>();
		HashMap<String, List<TestResponse>> errorBuckets = new HashMap<>();

		do
		{
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put("time_range", TIME_RANGE);
			parameters.put("scope", SCOPE);
			parameters.put("status", STATUS);
//			parameters.put("pretty", true);
			parameters.put("size", size);
			parameters.put("from", from);

			HttpRequest request = Unirest.get(TESTS_ENDPOINT)
					.queryString(parameters)
					.basicAuth(USERNAME, ACCESS_KEY);

			HttpResponse<String> response = request.asString();
			String body = response.getBody();

			JsonPath jsonPath = JsonPath.from(body);
			has_more = jsonPath.getBoolean(("has_more"));
			List<TestResponse> items = jsonPath.getList("items", TestResponse.class);

			// collect all errored tests
			allErrors.addAll((items));

			// filter them all into separate buckets based on error message
			errorMessages.forEach(errorMessage -> {
				errorMessage = errorMessage.trim();
				List<TestResponse> errors = new ArrayList<>();

				// get new errors
				errors.addAll(filterByError(items, errorMessage));

				// add any existing errors (fix for https://github.com/sauceaaron/erroredtests/issues/2)
				if (errorBuckets.containsKey(errorMessage))
				{
					errors.addAll(errorBuckets.get(errorMessage));
				}

				// put them all back in the bucket -- NOTE: this is an inefficient copy
				errorBuckets.put(errorMessage, errors);
			});

			// get the next 100 tests until all are collected or max is achieved
			from+= size;
		}
		while (has_more && from <= max);

		if (showTests == true)
		{
			allErrors.forEach(testResponse -> {
				System.out.println(testResponse.asJson());
			});
		}

		System.out.println("allErrors: " + allErrors.size());

		errorBuckets.forEach( (errorMessage, testResponses) -> {
			System.out.println("error: " + errorMessage + ": " +  testResponses.size());
		});
	}

	public static List<TestResponse> filterByError(List<TestResponse> items, String errorMessage)
	{
		return items.stream().filter(
				test -> test.error.contains(errorMessage)
		).collect(Collectors.toList());
	}

	public class TestResponse
	{
		public String id;
		public String owner;
		public String ancestor;
		public String name;
		public String build;
		public String creation_time;
		public String start_time;
		public String end_time;
		public String duration;
		public String status;
		public String error;
		public String os;
		public String os_normalized;
		public String browser;
		public String browser_normalized;
		public String details_url;

		public String asJson()
		{
			Gson gson = new GsonBuilder().create();
			return gson.toJson(this);
		}
	}
}