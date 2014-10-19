package org.joda.beans.gradle.tasks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class AbstractJodaBeansTask extends DefaultTask {
	public String sourceDir;

	protected String getSourceDir() {
		return hasProperty("testSourceDir") ? (String) property("testSourceDir")
				: getProject().getBuildDir().toString();
	}

	protected String getTestSourceDir() {
		return hasProperty("testSourceDir") ? (String) property("testSourceDir")
				: null;
	}

	protected String getIndent() {
		return hasProperty("indent") ? (String) property("indent") : null;
	}

	protected String getPrefix() {
		return hasProperty("prefix") ? (String) property("prefix") : null;
	}

	protected String getVerbose() {
		return hasProperty("verbose") ? (String) property("verbose") : null;
	}

	/**
	 * Builds the arguments to the tool.
	 * 
	 * @return the arguments, not null
	 */
	protected List<String> buildArgs() {
		List<String> argsList = Lists.newArrayList();
		argsList.add("-R");
		if (getIndent() != null) {
			argsList.add("-indent=" + getIndent());
		}
		if (getPrefix() != null) {
			argsList.add("-prefix=" + getPrefix());
		}
		if (getVerbose() != null) {
			argsList.add("-verbose=" + getVerbose());
		}
		return argsList;
	}

	/**
	 * Obtains the classloader from a set of file paths.
	 * 
	 * @return the classloader, not null
	 */
	protected ClassLoader obtainClassLoader() {
		return AbstractJodaBeansTask.class.getClassLoader();
	}

	protected int runTool(Class<?> toolClass, List<String> argsList) {
		// invoke main source
		argsList.add(getSourceDir());
		int count = invoke(toolClass, argsList);
		// optionally invoke test source
		if (!Strings.isNullOrEmpty(getTestSourceDir())) {
			argsList.set(argsList.size() - 1, getTestSourceDir());
			count += invoke(toolClass, argsList);
		}
		return count;
	}

	private int invoke(Class<?> toolClass, List<String> argsList) {
		Method createFromArgsMethod = findCreateFromArgsMethod(toolClass);
		Method processMethod = findProcessMethod(toolClass);
		Object beanCodeGen = createBuilder(argsList, createFromArgsMethod);
		return invokeBuilder(processMethod, beanCodeGen);
	}

	private Object createBuilder(List<String> argsList,
			Method createFromArgsMethod) throws GradleException {
		String[] args = argsList.toArray(new String[argsList.size()]);
		try {
			return createFromArgsMethod.invoke(null, new Object[] { args });
		} catch (IllegalArgumentException ex) {
			throw new GradleException(
					"Error invoking BeanCodeGen.createFromArgs()");
		} catch (IllegalAccessException ex) {
			throw new GradleException(
					"Error invoking BeanCodeGen.createFromArgs()");
		} catch (InvocationTargetException ex) {
			throw new GradleException("Invalid Joda-Beans Mojo configuration: "
					+ ex.getCause().getMessage(), ex.getCause());
		}
	}

	private int invokeBuilder(Method processMethod, Object beanCodeGen)
			throws GradleException {
		try {
			return (Integer) processMethod.invoke(beanCodeGen);
		} catch (IllegalArgumentException ex) {
			throw new GradleException("Error invoking BeanCodeGen.process()");
		} catch (IllegalAccessException ex) {
			throw new GradleException("Error invoking BeanCodeGen.process()");
		} catch (InvocationTargetException ex) {
			throw new GradleException("Error while running Joda-Beans tool: "
					+ ex.getCause().getMessage(), ex.getCause());
		}
	}

	private Method findCreateFromArgsMethod(Class<?> toolClass) {
		Method createFromArgsMethod = null;
		try {
			createFromArgsMethod = toolClass.getMethod("createFromArgs",
					String[].class);
		} catch (Exception ex) {
			throw new GradleException(
					"Unable to find method BeanCodeGen.createFromArgs()");
		}
		return createFromArgsMethod;
	}

	private Method findProcessMethod(Class<?> toolClass) throws GradleException {
		Method processMethod = null;
		try {
			processMethod = toolClass.getMethod("process");
		} catch (Exception ex) {
			throw new GradleException(
					"Unable to find method BeanCodeGen.process()");
		}
		return processMethod;
	}
}
