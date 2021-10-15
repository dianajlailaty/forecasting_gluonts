package org.activeeon.morphemic.application.deployment;

import org.activeeon.morphemic.service.TemporaryFilesHelper;
import org.activeeon.morphemic.service.Utils;
import org.apache.log4j.Logger;
import org.ow2.proactive.scheduler.common.job.JobVariable;
import org.ow2.proactive.scheduler.common.task.ScriptTask;
import org.ow2.proactive.scheduler.common.task.TaskVariable;
import org.ow2.proactive.scripting.InvalidScriptException;
import org.ow2.proactive.scripting.SelectionScript;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.scripting.TaskScript;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PAFactory {

    private static final Logger LOGGER = Logger.getLogger(PAFactory.class);

    private PAFactory() {
    }

    /**
     * Create a simple script
     * @param implementation The script implementation
     * @param scriptLanguage The script language
     * @return A ProActive SimpleScript instance
     */
    public static SimpleScript createSimpleScript(String implementation, String scriptLanguage) {
        SimpleScript mySQLSimpleScript = null;
        LOGGER.debug("Creating a simple script from its implementation ");
        try {
            mySQLSimpleScript = new SimpleScript(implementation, scriptLanguage);
        } catch (InvalidScriptException ise) {
            LOGGER.error("ERROR: Simple script not created due to an InvalidScriptException: " + ise.toString());
        }
        LOGGER.debug("Simple script created.");
        return mySQLSimpleScript;
    }

    /**
     * Create a simple script from a file
     * @param scriptFileName The script implementation file name
     * @param scriptLanguage The script language
     * @return A ProActive SimpleScript instance
     */
    public static SimpleScript createSimpleScriptFromFIle(String scriptFileName, String scriptLanguage) {
        SimpleScript mySQLSimpleScript = null;
        String script = Utils.getContentWithFileName(scriptFileName);
        mySQLSimpleScript = createSimpleScript(script, scriptLanguage);
        LOGGER.debug("Simple script created.");
        return mySQLSimpleScript;
    }

    /**
     * Create a script task
     * @param taskName The name of the task
     * @param implementationScript The script implementation
     * @param scriptLanguage The script language
     * @return A ProActive ScriptTask instance
     */
    public static ScriptTask createScriptTask(String taskName, String implementationScript, String scriptLanguage) {
        ScriptTask scriptTask = new ScriptTask();
        scriptTask.setName(taskName);
        SimpleScript simpleScript;
        TaskScript taskScript = null;
        LOGGER.debug("Creating a bash script task");
        try {
            simpleScript = createSimpleScript(implementationScript, scriptLanguage);
            taskScript = new TaskScript(simpleScript);
        } catch (InvalidScriptException ie) {
            LOGGER.error("ERROR: Task " + taskName + " script not created due to an InvalidScriptException: " + ie.toString());
        }
        LOGGER.debug("Bash script task created.");
        scriptTask.setScript(taskScript);
        return scriptTask;
    }

    /**
     * Create a groovy script task
     * @param taskName The name of the task
     * @param implementationScript The script implementation
     * @return A ProActive ScriptTask instance
     */
    public static ScriptTask createGroovyScriptTask(String taskName, String implementationScript) {
        return createScriptTask(taskName, implementationScript, "groovy");
    }

    /**
     * Create a Bash script task
     * @param taskName The name of the task
     * @param implementationScript The script implementation
     * @return A ProActive ScriptTask instance
     */
    public static ScriptTask createBashScriptTask(String taskName, String implementationScript) {
        return createScriptTask(taskName, implementationScript, "bash");
    }

    /**
     * Create a groovy script task
     * @param taskName The name of the task
     * @param scriptFileName The script implementation file name
     * @return A ProActive ScriptTask instance
     */
    public static ScriptTask createGroovyScriptTaskFromFile(String taskName, String scriptFileName) {
        return createScriptTaskFromFile(taskName, scriptFileName, "groovy");
    }

    /**
     * Create a Bash script task
     * @param taskName The name of the task
     * @param scriptFileName The script implementation file name
     * @return A ProActive ScriptTask instance
     */
    public static ScriptTask createBashScriptTaskFromFile(String taskName, String scriptFileName) {
        return createScriptTaskFromFile(taskName, scriptFileName, "bash");
    }

    /**
     * Create a script task
     * @param taskName The name of the task
     * @param scriptFileName The script implementation file name
     * @param scriptLanguage The script language
     * @return A ProActive ScriptTask instance
     */
    private static ScriptTask createScriptTaskFromFile(String taskName, String scriptFileName, String scriptLanguage) {
        ScriptTask scriptTask = new ScriptTask();
        scriptTask.setName(taskName);
        SimpleScript simpleScript;
        TaskScript taskScript = null;
        LOGGER.debug("Creating a bash script task from the file : " + scriptFileName);
        try {
            simpleScript = createSimpleScriptFromFIle(scriptFileName, scriptLanguage);
            taskScript = new TaskScript(simpleScript);
        } catch (InvalidScriptException ie) {
            LOGGER.error("ERROR: Task " + taskName + " script not created due to an InvalidScriptException: " + ie.toString());
        }
        LOGGER.debug("Bash script task created.");
        scriptTask.setScript(taskScript);
        return scriptTask;
    }

    /**
     * Create a script task
     * @param taskName The name of the task
     * @param scriptFileName The script implementation file name
     * @param scriptLanguage The script language
     * @param preScriptFileName The pre-script implementation file name
     * @param preScriptLanguage The pre-script language
     * @param postScriptFileName The post-script implementation file name
     * @param postScriptLanguage The post-script language
     * @return A ProActive ScriptTask instance
     */
    public static ScriptTask createComplexScriptTaskFromFiles(String taskName, String scriptFileName, String scriptLanguage, String preScriptFileName, String preScriptLanguage, String postScriptFileName, String postScriptLanguage  ) {
        ScriptTask scriptTask = new ScriptTask();
        scriptTask.setName(taskName);
        TaskScript taskScript = null;
        TaskScript taskPreScript = null;
        TaskScript taskPostScript = null;
        LOGGER.debug("Creating a script task from the files : scriptFileName=" + scriptFileName + " preScriptFileName=" + preScriptFileName + " postScriptFileName=" + postScriptFileName);
        try {
            taskScript = new TaskScript(createSimpleScriptFromFIle(scriptFileName, scriptLanguage));
            taskPreScript = new TaskScript(createSimpleScriptFromFIle(preScriptFileName, preScriptLanguage));
            taskPostScript = new TaskScript(createSimpleScriptFromFIle(postScriptFileName, postScriptLanguage));
        } catch (InvalidScriptException ie) {
            LOGGER.error("ERROR: Task " + taskName + " script not created due to an InvalidScriptException: " + ie.toString());
        }
        LOGGER.debug("Bash script task created.");
        scriptTask.setScript(taskScript);
        scriptTask.setPreScript(taskPreScript);
        scriptTask.setPostScript(taskPostScript);
        return scriptTask;
    }

    /**
     * Create a Groovy node selection script
     * @param scriptFileName The script implementation file name
     * @param parameters The selection script parameters
     * @return A ProActive SelectionScript instance
     */
    public static SelectionScript createGroovySelectionScript(String scriptFileName, String[] parameters) throws IOException {
        SelectionScript selectionScript = null;
        LOGGER.debug("Creating a groovy selection script");
        File scriptFile;
        scriptFile = TemporaryFilesHelper.createTempFileFromResource(scriptFileName);
        try {
            selectionScript = new SelectionScript(scriptFile, parameters);
        } catch (InvalidScriptException ie) {
            LOGGER.error("ERROR: Selection script not created due to an InvalidScriptException: " + ie.toString());
        }
        LOGGER.debug("Groovy selection script created.");
        TemporaryFilesHelper.delete(scriptFile);
        return selectionScript;
    }

    /**
     * Create a Map of variables of (String (key), TaskVariable (value))
     * @param variables A Map of variables in (String (key), String (value))
     * @return A Map of variables in (String, TaskVariable)
     */
    public static Map<String, TaskVariable> variablesToTaskVariables(Map<String, String> variables) {
        Map<String, TaskVariable> taskVariables = new HashMap<>();
        variables.forEach((k, v) -> taskVariables.put(k, new TaskVariable(k, v)));
        return taskVariables;
    }

    /**
     * Create a Map of variables of (String (key), JobVariable (value))
     * @param variables A Map of variables in (String (key), String (value))
     * @return A Map of variables in (String, JobVariable)
     */
    public static Map<String, JobVariable> variablesToJobVariables(Map<String, String> variables) {
        Map<String, JobVariable> jobVariables = new HashMap<>();
        variables.forEach((k, v) -> jobVariables.put(k, new TaskVariable(k, v)));
        return jobVariables;
    }
}
