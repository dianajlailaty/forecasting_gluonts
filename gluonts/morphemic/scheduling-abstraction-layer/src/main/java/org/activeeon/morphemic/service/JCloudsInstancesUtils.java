package org.activeeon.morphemic.service;

import org.apache.log4j.Logger;
import org.jclouds.ec2.domain.InstanceType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JCloudsInstancesUtils {

    private static final Set<String> handledAWSInstanceTypes;

    private static final Logger LOGGER = Logger.getLogger(JCloudsInstancesUtils.class);

    static {
        handledAWSInstanceTypes = new HashSet<>();
        Arrays.stream(InstanceType.class.getFields()).forEach(field -> {
            try {
                handledAWSInstanceTypes.add(field.get(InstanceType.class).toString());
            } catch (IllegalAccessException e) {
                LOGGER.warn(e.getStackTrace());
            }
        });
    }

    private JCloudsInstancesUtils() {
    }

    /**
     * Some hardware instance types (like t3a and t4g for AWS) are not YET, handled by jclouds.
     * This method is here to check if the instance type is handled or not.
     * @param providerName The Cloud provider id
     * @param instanceType The instance type to be checked
     * @return true if the instance type is handled by jclouds, false otherwise
     */
    public static boolean isHandledHardwareInstanceType(String providerName, String instanceType) {
        if ("aws-ec2".equals(providerName))
            return handledAWSInstanceTypes.contains(instanceType);
        // TODO: To check if for other cloud providers all instance types are handled by JClouds
        return true;
    }
}
