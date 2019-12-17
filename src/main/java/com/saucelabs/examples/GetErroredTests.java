package com.saucelabs.examples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import io.restassured.path.json.JsonPath;

import java.util.ArrayList;
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

	public static void main(String[] args) throws UnirestException
	{
		int from = 0; //starting test sequence
		int size = 100;
		int max = 10000;

		boolean has_more;

		List<TestResponse> allErrors = new ArrayList<>();
		List<TestResponse> timeoutErrors = new ArrayList<>();
		List<TestResponse> internalServerErrors = new ArrayList<>();
		List<TestResponse> infrastructureErrors = new ArrayList<>();

		do
		{
			HashMap<String, Object> parameters = new HashMap<>();
			parameters.put("time_range", TIME_RANGE);
			parameters.put("scope", SCOPE);
			parameters.put("status", STATUS);
			parameters.put("pretty", true);
			parameters.put("size", size);
			parameters.put("from", from);

			HttpRequest request = Unirest.get(TESTS_ENDPOINT)
					.queryString(parameters)
					.basicAuth(USERNAME, ACCESS_KEY);
			System.out.println("request URL: " + request.getUrl());

			HttpResponse<String> response = request.asString();
			System.out.println("status: " + response.getStatus() + " " + response.getStatusText());

			String body = response.getBody();
			System.out.println("body:" + body);

			JsonPath jsonPath = JsonPath.from(body);
			has_more = jsonPath.getBoolean(("has_more"));
			List<TestResponse> items = jsonPath.getList("items", TestResponse.class);

			// collect all errored tests
			allErrors.addAll((items));

			// filter and collect timeout errors
			timeoutErrors.addAll(filterByError(items, "Test did not see a new command"));

			// filter and collect internal server errors
			internalServerErrors.addAll(filterByError(items, "Internal Server Error"));

			// filter and collect infrastructure errors
			infrastructureErrors.addAll(filterByError(items, "Infrastructure Error"));

			// get the next 100 tests until all are collected or max is achieved
			from+= size;

			System.out.println("has_more: " + has_more);
			System.out.println("current set: " + items.size());
		}
		while (has_more && from <= max);

		System.out.println("allErrors: " + allErrors.size());
		System.out.println("timeoutErrors: " + timeoutErrors.size());
		System.out.println("internalServerErrors: " + internalServerErrors.size());
		System.out.println("infrastructureErrors: " + infrastructureErrors.size());

		// output results of all tests with a particular Error
		infrastructureErrors.forEach(test-> {
			System.out.println(test.asJson());
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