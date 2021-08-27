package org.activeeon.morphemic.infrastructure.deployment;

import lombok.SneakyThrows;
import org.activeeon.morphemic.model.Credentials;
import org.activeeon.morphemic.model.PACloud;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PAConnectorIaasGateway {

    private final String paURL;

    private final String CONNECTOR_IAAS_PATH = "/connector-iaas";

    private static final Logger LOGGER = Logger.getLogger(PAConnectorIaasGateway.class);

    public PAConnectorIaasGateway(String paServerURL) {
        this.paURL = paServerURL;
    }

    @SneakyThrows
    public JSONArray getNodeCandidates(String nodeSourceName, String region, String imageReq) {
        Validate.notNull(nodeSourceName, "nodeSourceName must not be null");
        Validate.notNull(region, "region must not be null");
        LOGGER.info("Retrieving node candidates for cloud " + nodeSourceName + " region " +
                region + " and imageReq " + imageReq);
        JSONArray nodeCandidates = null;

        URIBuilder uriBuilder = new URIBuilder(new URL(paURL).toURI());
        URI requestUri = uriBuilder.setPath(CONNECTOR_IAAS_PATH + "/infrastructures/" + nodeSourceName + "/nodecandidates")
                .addParameter("region", region)
                .addParameter("imageReq", imageReq)
                .build();

        HttpURLConnection connection = (HttpURLConnection) requestUri.toURL().openConnection();
        connection.setRequestMethod(HttpMethod.POST.toString());
        LOGGER.info("requestUri = " + requestUri.toString());

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("Failed : HTTP error code : " + connection.getResponseCode());
            return null;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (connection.getInputStream())));

        nodeCandidates = new JSONArray(new JSONTokener(br));
        LOGGER.info("Node candidates retrieved for successfully: " + nodeCandidates.toString());

        connection.disconnect();

        return nodeCandidates;
    }

    @SneakyThrows
    public JSONArray getImages(String nodeSourceName) {
        Validate.notNull(nodeSourceName, "nodeSourceName must not be null");
        LOGGER.info("Retrieving images for cloud " + nodeSourceName);
        JSONArray images = null;

        URIBuilder uriBuilder = new URIBuilder(new URL(paURL).toURI());
        URI requestUri = uriBuilder.setPath(CONNECTOR_IAAS_PATH + "/infrastructures/" + nodeSourceName + "/images")
                .build();

        HttpURLConnection connection = (HttpURLConnection) requestUri.toURL().openConnection();
        connection.setRequestMethod(HttpMethod.GET.toString());
        LOGGER.info("requestUri = " + requestUri.toString());

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("Failed : HTTP error code : " + connection.getResponseCode());
            return null;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (connection.getInputStream())));

        images = new JSONArray(new JSONTokener(br));
        LOGGER.info("Images retrieved for cloud " + nodeSourceName + ". Images: " + images.toString());

        connection.disconnect();

        return images;
    }

    @SneakyThrows
    public JSONArray getRegions(String nodeSourceName) {
        Validate.notNull(nodeSourceName, "nodeSourceName must not be null");
        LOGGER.debug("Retrieving regions for cloud " + nodeSourceName);
        JSONArray regions = null;

        URIBuilder uriBuilder = new URIBuilder(new URL(paURL).toURI());
        URI requestUri = uriBuilder.setPath(CONNECTOR_IAAS_PATH + "/infrastructures/" + nodeSourceName + "/regions")
                .build();

        HttpURLConnection connection = (HttpURLConnection) requestUri.toURL().openConnection();
        connection.setRequestMethod(HttpMethod.GET.toString());
        LOGGER.debug("requestUri = " + requestUri.toString());

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("Failed : HTTP error code : " + connection.getResponseCode());
            return null;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (connection.getInputStream())));

        regions = new JSONArray(new JSONTokener(br));
        LOGGER.debug("Regions retrieved for cloud " + nodeSourceName + ". Images: " + regions.toString());

        connection.disconnect();

        return regions;
    }

    @SneakyThrows
    public void defineInfrastructure(String infrastructureName, PACloud cloud, String region) {
        Validate.notNull(infrastructureName, "infrastructureName must not be null");
        Validate.notNull(cloud.getCloudProviderName(), "cloudProviderName must not be null");

        URIBuilder uriBuilder = new URIBuilder(new URL(paURL).toURI());
        URI requestUri = uriBuilder.setPath(CONNECTOR_IAAS_PATH + "/infrastructures").build();

        HttpURLConnection connection = (HttpURLConnection) requestUri.toURL().openConnection();
        connection.setRequestMethod(HttpMethod.POST.toString());
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);

        String jsonOutputString;
        switch (cloud.getCloudProviderName()) {
            case "aws-ec2":
                jsonOutputString = "{\"id\": \"" + infrastructureName + "\"," +
                        "\"type\": \"" + cloud.getCloudProviderName() + "\"," +
                        "\"credentials\": {\"username\": \"" + cloud.getCredentials().getUserName() + "\", \"password\": \"" +
                        cloud.getCredentials().getPrivateKey() + "\"}, \"region\": \"" + region + "\"}";
                break;
            case "openstack":
                jsonOutputString = "{\"id\": \"" + infrastructureName + "\"," +
                        "\"type\": \"openstack-nova\", \"endpoint\": \"" + cloud.getEndpoint() +
                        "\", \"scope\":{\"prefix\": \"" + cloud.getScopePrefix() + "\", \"value\":\"" +
                        cloud.getScopeValue() + "\"}, \"identityVersion\": \"" + cloud.getIdentityVersion() + "\", " +
                        "\"credentials\": {\"username\": \"" + cloud.getCredentials().getUserName() +
                        "\", \"password\": \"" + cloud.getCredentials().getPrivateKey() + "\", \"domain\": \"" +
                        cloud.getCredentials().getDomain() + "\"}, \"region\": \"" + region + "\"}";
                break;
            default:
                throw new IllegalArgumentException("The infrastructure " + cloud.getCloudProviderName() + " is not handled yet.");
        }

        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonOutputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        LOGGER.debug("requestUri = " + requestUri.toString());

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("Failed : HTTP error code : " + connection.getResponseCode());
        }

        LOGGER.debug("Infrastructure defined successfully.");

        connection.disconnect();
    }

    @SneakyThrows
    public List<String> getAllRegions(String infrastructureName) {
        Validate.notNull(infrastructureName, "infrastructureName must not be null");
        List<String> regions = null;

        URIBuilder uriBuilder = new URIBuilder(new URL(paURL).toURI());
        URI requestUri = uriBuilder.setPath(CONNECTOR_IAAS_PATH + "/infrastructures/" + infrastructureName + "/regions").build();

        HttpURLConnection connection = (HttpURLConnection) requestUri.toURL().openConnection();
        connection.setRequestMethod(HttpMethod.GET.toString());
        LOGGER.debug("requestUri = " + requestUri.toString());

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("Failed : HTTP error code : " + connection.getResponseCode());
            return null;
        }

        StringWriter writer = new StringWriter();
        IOUtils.copy(connection.getInputStream(), writer, "utf-8");
        String response = writer.toString();

        response = response.replaceAll("\\[", "")
                .replaceAll("]", "")
                .replaceAll("\"", "");

        regions = Stream.of(response.split(",")).map(String::trim).collect(Collectors.toList());
        LOGGER.debug("Regions retrieved successfully: " + regions);

        connection.disconnect();

        return regions;
    }

    @SneakyThrows
    public void deleteInfrastructure(String infrastructureName) {
        Validate.notNull(infrastructureName, "infrastructureName must not be null");

        URIBuilder uriBuilder = new URIBuilder(new URL(paURL).toURI());
        URI requestUri = uriBuilder.setPath(CONNECTOR_IAAS_PATH + "/infrastructures/" + infrastructureName).build();

        HttpURLConnection connection = (HttpURLConnection) requestUri.toURL().openConnection();
        connection.setRequestMethod(HttpMethod.DELETE.toString());

        LOGGER.debug("requestUri = " + requestUri.toString());

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("Failed : HTTP error code : " + connection.getResponseCode());
        }

        LOGGER.debug("Infrastructure deleted successfully.");

        connection.disconnect();
    }
}
