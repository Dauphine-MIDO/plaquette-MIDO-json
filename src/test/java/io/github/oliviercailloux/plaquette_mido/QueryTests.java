package io.github.oliviercailloux.plaquette_mido;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.oliviercailloux.json.PrintableJsonObjectFactory;

class QueryTests {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryTests.class);
	public static final String PREFIX = "FRUAI0750736TPR";
	public static final URI ENDPOINT = URI
			.create("https://rof.testapi.dauphine.fr/ebx-dataservices/rest/data/v1/BpvRefRof/RefRof/root/Mention");
	public static final UriBuilder MENTION_URL = UriBuilder.fromUri(ENDPOINT).path(PREFIX + "{ident}");

	@Test
	void test() throws Exception {
		final Client client = ClientBuilder.newClient();
		client.register(HttpAuthenticationFeature.basic("plaquette-mido", getTokenValue()));
		final WebTarget t1 = client.target(ENDPOINT);
		final JsonObject result = t1.request(MediaType.APPLICATION_JSON).get(JsonObject.class);
		LOGGER.debug("Result: {}.", PrintableJsonObjectFactory.wrapObject(result));
		client.close();
		final JsonArray rows = result.getJsonArray("rows");
		assertTrue(rows.size() >= 1);
		final JsonObject mention = rows.getJsonObject(0);
		final String mentionLabel = mention.getString("label");
		final JsonObject mentionContent = mention.getJsonObject("content");
		final String mentionLabelEmbedded = mentionContent.getJsonObject("mentionID").getString("content");
		assertTrue(mentionLabel.equals(mentionLabelEmbedded));
		final String mentionIdent = mentionContent.getJsonObject("ident").getString("content");
		assertTrue(mentionLabel.equals(PREFIX + mentionIdent));
		final URI builtMentionUri = MENTION_URL.build(mentionIdent);
		assertTrue(mention.getString("details").equals(builtMentionUri.toString()));
	}

	private static String getTokenValue() throws IOException, IllegalStateException {
		final Optional<String> tokenOpt = getTokenOpt();
		return tokenOpt
				.orElseThrow(() -> new IllegalStateException("No token found in environment, in property or in file."));
	}

	private static Optional<String> getTokenOpt() throws IOException {
		{
			final String token = System.getenv("API_password");
			if (token != null) {
				return Optional.of(token);
			}
		}
		{
			final String token = System.getProperty("API_password");
			if (token != null) {
				return Optional.of(token);
			}
		}
		final Path path = Paths.get("API_password.txt");
		if (!Files.exists(path)) {
			return Optional.empty();
		}
		final String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		return Optional.of(content.replaceAll("\n", ""));
	}

}
