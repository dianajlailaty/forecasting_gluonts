package org.activeeon.morphemic.service;

import org.activeeon.morphemic.application.deployment.PAFactory;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    private Utils() { }

    public static String getContentWithFileName(String fileName) {
        String script;
        String newLine = System.getProperty("line.separator");
        String scriptFileNameWithSeparator = (fileName.startsWith(File.separator)) ?
                fileName : File.separator + fileName;
        LOGGER.debug("Creating a simple script from the file : " + scriptFileNameWithSeparator);
        try (Stream<String> lines = new BufferedReader(new InputStreamReader(
                PAFactory.class.getResourceAsStream(scriptFileNameWithSeparator))).lines()) {
            script = lines.collect(Collectors.joining(newLine));
        }
        return script;
    }
}
