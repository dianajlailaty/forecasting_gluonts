package org.activeeon.morphemic.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;

import java.util.List;

@AllArgsConstructor
public class UpdatingNodeCandidatesThread extends Thread {

    private final String paURL;

    private final List<String> newCloudIds;

    private static final Logger LOGGER = Logger.getLogger(UpdatingNodeCandidatesThread.class);

    @SneakyThrows
    @Override
    public void run() {
        LOGGER.info("Thread updating node candidates related to clouds " + newCloudIds.toString() + " started.");
        NodeCandidateUtils nodeCandidateUtils = new NodeCandidateUtils(this.paURL);
        nodeCandidateUtils.updateNodeCandidates(newCloudIds);
        LOGGER.info("Thread updating node candidates related to clouds " + newCloudIds.toString() + " ended properly.");
    }
}
