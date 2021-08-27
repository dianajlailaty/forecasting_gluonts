package org.activeeon.morphemic.service;

import org.activeeon.morphemic.infrastructure.deployment.PAConnectorIaasGateway;
import org.activeeon.morphemic.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class NodeCandidateUtils {

    public PAConnectorIaasGateway connectorIaasGateway;

    private final GeoLocationUtils geoLocationUtils;

    private static final Logger LOGGER = Logger.getLogger(NodeCandidateUtils.class);

    public NodeCandidateUtils(String paURL) {
        connectorIaasGateway = new PAConnectorIaasGateway(paURL);
        geoLocationUtils = new GeoLocationUtils();
    }

    public static boolean verifyAllFilters(List<Requirement> requirements, NodeCandidate nodeCandidate) {
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        if (requirements.get(0) instanceof NodeTypeRequirement) {
            if (satisfyNodeTypeRequirement((NodeTypeRequirement) requirements.get(0), nodeCandidate)) {
                return verifyAllFilters(requirements.subList(1, requirements.size()), nodeCandidate);
            }
            return false;
        }
        if (requirements.get(0) instanceof AttributeRequirement) {
            if (satisfyAttributeRequirement((AttributeRequirement) requirements.get(0), nodeCandidate)) {
                return verifyAllFilters(requirements.subList(1, requirements.size()), nodeCandidate);
            }
            return false;
        }
        LOGGER.warn("Unknown requirement type. It could not be applied: " + requirements.get(0).toString());
        return verifyAllFilters(requirements.subList(1, requirements.size()), nodeCandidate);
    }

    private static boolean satisfyAttributeRequirement(AttributeRequirement attributeRequirement, NodeCandidate nodeCandidate) {
        if (attributeRequirement.getRequirementClass().equals("hardware")) {
            switch (attributeRequirement.getRequirementAttribute()) {
                case "ram":
                    return attributeRequirement.getRequirementOperator().compare(nodeCandidate.getHardware().getRam(),
                            Long.valueOf(attributeRequirement.getValue()));
                case "cores":
                    return attributeRequirement.getRequirementOperator().compare(nodeCandidate.getHardware().getCores(),
                            Integer.valueOf(attributeRequirement.getValue()));
                case "disk":
                    return attributeRequirement.getRequirementOperator().compare(nodeCandidate.getHardware().getDisk(),
                            Double.valueOf(attributeRequirement.getValue()));
            }
        }
        if (attributeRequirement.getRequirementClass().equals("location")) {
            if (attributeRequirement.getRequirementAttribute().equals("geoLocation.country")) {
                return attributeRequirement.getRequirementOperator().compare(
                        nodeCandidate.getLocation().getGeoLocation().getCountry(), attributeRequirement.getValue());
            }
        }
        if (attributeRequirement.getRequirementClass().equals("image")) {
            switch (attributeRequirement.getRequirementAttribute()) {
                case "name":
                    return attributeRequirement.getRequirementOperator().compare(nodeCandidate.getImage().getName(),
                            attributeRequirement.getValue());
                case "operatingSystem.family":
                    return attributeRequirement.getRequirementOperator().compare(
                            nodeCandidate.getImage().getOperatingSystem().getOperatingSystemFamily().name(),
                            attributeRequirement.getValue());
                case "operatingSystem.version":
                    return attributeRequirement.getRequirementOperator().compare(
                            nodeCandidate.getImage().getOperatingSystem().getOperatingSystemVersion().toString(),
                            attributeRequirement.getValue());
            }
        }
        if (attributeRequirement.getRequirementClass().toLowerCase(Locale.ROOT).equals("cloud")) {
            if (attributeRequirement.getRequirementAttribute().equals("type")) {
                return attributeRequirement.getRequirementOperator().compare(
                        nodeCandidate.getCloud().getCloudType().name(), attributeRequirement.getValue());
            }
        }
        if (attributeRequirement.getRequirementClass().toLowerCase(Locale.ROOT).equals("environment")) {
            if (attributeRequirement.getRequirementAttribute().equals("runtime")) {
                return attributeRequirement.getRequirementOperator().compare(
                        nodeCandidate.getEnvironment().getRuntime().name(), attributeRequirement.getValue());
            }
        }
        LOGGER.warn("Unknown requirement type. It could not be applied: " + attributeRequirement.toString());
        return true;
    }

    private static boolean satisfyNodeTypeRequirement(NodeTypeRequirement requirement, NodeCandidate nodeCandidate) {
        return (requirement.getNodeType().getLiteral().equals(nodeCandidate.getNodeCandidateType().name()));
    }

    private Hardware createHardware(JSONObject nodeCandidateJSON, PACloud paCloud) {
        JSONObject hardwareJSON = nodeCandidateJSON.optJSONObject("hw");
        String hardwareId = paCloud.getCloudID() + "/" +
                nodeCandidateJSON.optString("region") + "/" +
                hardwareJSON.optString("type");
        Hardware hardware = EntityManagerHelper.find(Hardware.class, hardwareId);
        if (hardware == null) {
           hardware = new Hardware();
           hardware.setId(hardwareId);
           hardware.setName(hardwareJSON.optString("type"));
           hardware.setProviderId(hardwareJSON.optString("type"));
           hardware.setCores(Math.round(Float.parseFloat(hardwareJSON.optString("minCores"))));
           hardware.setRam(Long.valueOf(hardwareJSON.optString("minRam")));
           if ("aws-ec2".equals(nodeCandidateJSON.optString("cloud"))) {
               hardware.setDisk((double) 8);
           } else {
               hardware.setDisk((double) 0);
           }
           hardware.setLocation(createLocation(nodeCandidateJSON, paCloud));
        }

        return hardware;
    }

    private Location createLocation(JSONObject nodeCandidateJSON, PACloud paCloud) {
        LOGGER.debug("Creating location ...");
        String locationId = paCloud.getCloudID() + "/" + nodeCandidateJSON.optString("region");
        Location location = EntityManagerHelper.find(Location.class, locationId);
        if (location == null) {
            location = new Location();
            location.setId(locationId);
            location.name(nodeCandidateJSON.optString("region"));
            location.setProviderId(nodeCandidateJSON.optString("region"));
            location.setLocationScope(Location.LocationScopeEnum.REGION);
            location.setIsAssignable(true);
            location.setGeoLocation(createGeoLocation(paCloud.getCloudProviderName(), location.getName()));
        }
        LOGGER.debug("Location created: " + location.toString());
        return location;
    }

    private GeoLocation createGeoLocation(String cloud, String region) {
        switch (cloud) {
            case "aws-ec2":
                return new GeoLocation(geoLocationUtils.findGeoLocation("AWS", region));
            case "azure":
                return new GeoLocation(geoLocationUtils.findGeoLocation("Azure", region));
            case "gce":
                return new GeoLocation(geoLocationUtils.findGeoLocation("GCE", region));
            case "openstack":
                return new GeoLocation(geoLocationUtils.findGeoLocation("OVH", region));
        }
        LOGGER.warn("Cloud provider name no handled for Geo Location.");
        return new GeoLocation();
    }

    private Image createImage(JSONObject nodeCandidateJSON, JSONObject imageJSON, PACloud paCloud) {
        String imageId = paCloud.getCloudID() + "/" + imageJSON.optString("id");
        Image image = EntityManagerHelper.find(Image.class, imageId);
        if(image == null) {
            image = new Image();
            image.setId(imageId);
            image.setName(imageJSON.optString("name"));
            image.setProviderId(StringUtils.substringAfterLast(imageJSON.optString("id"), "/"));
            OperatingSystem os = new OperatingSystem();
            JSONObject osJSON = imageJSON.optJSONObject("operatingSystem");
            os.setOperatingSystemFamily(OperatingSystemFamily.fromValue(osJSON.optString("family")));

            String arch = "";
            if ("aws-ec2".equals(nodeCandidateJSON.optString("cloud"))) {
                if (nodeCandidateJSON.optJSONObject("hw").optString("type").startsWith("a")) {
                    arch = osJSON.optBoolean("is64Bit") ? "ARM64" : "ARM" ;
                } else {
                    arch = osJSON.optBoolean("is64Bit") ? "AMD64" : "i386";
                }
            }
            os.setOperatingSystemArchitecture(OperatingSystemArchitecture.fromValue(arch));
            os.setOperatingSystemVersion(osJSON.optBigDecimal("version",
                    BigDecimal.valueOf(0)));
            image.setOperatingSystem(os);
            image.setLocation(createLocation(nodeCandidateJSON, paCloud));
        }

        return image;
    }

    private Cloud createCloud(JSONObject nodeCandidateJSON, PACloud paCloud) {
        Cloud cloud = EntityManagerHelper.find(Cloud.class, paCloud.getCloudID());
        if(cloud == null) {
            cloud = new Cloud();
            cloud.setId(paCloud.getCloudID());
            cloud.setCloudType(paCloud.getCloudType());
            cloud.setApi(new Api(nodeCandidateJSON.optString("cloud")));
            cloud.setCredential(new CloudCredential());
            cloud.setCloudConfiguration(new CloudConfiguration("", new HashMap<>()));
        }
        return cloud;
    }

    public NodeCandidate createNodeCandidate(JSONObject nodeCandidateJSON, JSONObject imageJSON, PACloud paCloud) {
        LOGGER.debug("Creating node candidate ...");
        NodeCandidate nodeCandidate = new NodeCandidate();
        nodeCandidate.setNodeCandidateType(NodeCandidate.NodeCandidateTypeEnum.IAAS);
        nodeCandidate.setPrice(nodeCandidateJSON.optDouble("price"));
        nodeCandidate.setCloud(createCloud(nodeCandidateJSON, paCloud));

        nodeCandidate.setImage(createImage(nodeCandidateJSON, imageJSON, paCloud));
        nodeCandidate.setLocation(createLocation(nodeCandidateJSON, paCloud));
        nodeCandidate.setHardware(createHardware(nodeCandidateJSON, paCloud));

        nodeCandidate.setPricePerInvocation((double) 0);
        nodeCandidate.setMemoryPrice((double) 0);
        nodeCandidate.setEnvironment(new Environment());
        LOGGER.debug("Node candidate created: " + nodeCandidate.toString());
        return nodeCandidate;
    }

    private static JSONObject convertObjectToJson(Object object) {
        JSONObject myJson = null;
        try {
            myJson = new JSONObject(new ObjectMapper().writeValueAsString(object));
        } catch (IOException e) {
            LOGGER.error("Error in casting Hashmap to JSON: " + Arrays.toString(e.getStackTrace()));
        }
        return myJson;
    }

    public void updateNodeCandidates(List<String> newCloudIds) {
        EntityManagerHelper.begin();

        newCloudIds.forEach(newCloudId -> {
            PACloud paCloud = EntityManagerHelper.find(PACloud.class, newCloudId);
            LOGGER.info("Getting blacklisted regions...");
            List<String> blacklistedRegions = Arrays.asList(paCloud.getBlacklist().split(","));
            LOGGER.info("Blacklisted regions: " + blacklistedRegions.toString());

            LOGGER.info("Getting images from Proactive ...");
            JSONArray images = connectorIaasGateway.getImages(paCloud.getDummyInfrastructureName());
            LOGGER.info("Returned images: " + images.toString());
            List<JSONObject> consolidatedImages = images.toList().parallelStream()
                    .map(NodeCandidateUtils::convertObjectToJson)
                    .filter(record -> !blacklistedRegions.contains(record.get("location")))
                    .collect(Collectors.toList());
            LOGGER.info("Consolidated images: " + consolidatedImages.toString());

            //TODO: (Optimization) An images per region map structure <region,[image1,image2]> could be the best here.
            // It can reduce the getNodeCandidates calls to PA.
            consolidatedImages.forEach(image -> {
                String region = image.optString("location");
                String imageReq;
                switch (paCloud.getCloudProviderName()) {
                    case "aws-ec2":
                        imageReq = "Linux";
                        break;
                    case "openstack":
                        imageReq = "linux";
                        break;
                    default:
                        throw new IllegalArgumentException("The infrastructure " + paCloud.getCloudProviderName() + " is not handled yet.");
                }
                JSONArray nodeCandidates = connectorIaasGateway.getNodeCandidates(paCloud.getDummyInfrastructureName(),
                        region, imageReq);
                nodeCandidates.forEach(nc -> {
                    JSONObject nodeCandidate = (JSONObject) nc;
                    EntityManagerHelper.persist(createLocation(nodeCandidate, paCloud));
                    EntityManagerHelper.persist(createNodeCandidate(nodeCandidate, image, paCloud));
                });
            });
        });

        EntityManagerHelper.commit();
    }
}
