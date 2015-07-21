/*******************************************************************************
 * Copyright 2015 - CNRS (Centre National de Recherche Scientifique)
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *******************************************************************************/
package fr.univnantes.lina;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * The access point-class of all uima-profiler functionalities.
 * 
 * @author Damien Cram
 *
 */
public class UIMAProfiler implements ITaskProfiler {
	
	private static boolean activated = false;
	private static boolean latexFormat = false;
//	private static int skipExamples = 0;
	private static int exampleLimit = 5;
	private static int overSizeRatio = 5;
	private static float addToExampleProba = 0.02f;
	public static void setActivated(boolean activated) {
		UIMAProfiler.activated = activated;
	}
//	public static void setSkipExamples(int skipExamples) {
//		TaskProfiler.skipExamples = skipExamples;
//	}
	public static void setExampleLimit(int exampleLimit) {
		UIMAProfiler.exampleLimit = exampleLimit;
	}
	public static void setLatexFormat(boolean latexFormat) {
		UIMAProfiler.latexFormat = latexFormat;
	}
	
	private static final Map<String, ITaskProfiler> profilers = Maps.newLinkedHashMap();

	public static final ITaskProfiler getProfiler(String name) {
		if(activated) {
			ITaskProfiler profiler = profilers.get(name);
			if(profiler != null)
				return profiler;
			else {
				profiler = new UIMAProfiler(name);
				profilers.put(name, profiler);
				return profiler;
			}
		} else {
			return DoNothingProfiler.getInstance();
		}
	}
	
	private String name;
	private Map<String, Object> pointStatus = Maps.newLinkedHashMap();
	private Map<String, MutableInt> counters = Maps.newLinkedHashMap();
	private Map<String, Set<Object>> examples = Maps.newHashMap();
	private Multimap<String, ProfilingTask> tasks = LinkedListMultimap.create();
	private Map<String, ProfilingTask> tasksById = Maps.newHashMap();
	

	private UIMAProfiler(String name) {
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see fr.cnrs.uima.profiler.ITaskProfiler#hit(java.lang.String)
	 */
	@Override
	public void hit(String hitName) {
		if(counters.get(hitName) == null) 
			counters.put(hitName, new MutableInt(1));
		else
			counters.get(hitName).increment();
	}
	
	@Override
	public void initHit(String hitName) {
		if(counters.get(hitName) == null) 
			counters.put(hitName, new MutableInt(0));
	}
	
	/* (non-Javadoc)
	 * @see fr.cnrs.uima.profiler.ITaskProfiler#hit(java.lang.String, java.lang.Object)
	 */
	@Override
	public void hit(String hitName, Object example) {
		this.hit(hitName);
		if(this.examples.get(hitName) == null) 
			this.examples.put(hitName, new HashSet<Object>());
				
		Set<Object> exBuffer = this.examples.get(hitName);
		
		if(!exBuffer.contains(example)) {
			if(exBuffer.size() <= overSizeRatio*exampleLimit) 
				exBuffer.add(example);
			else {
				if(new Random().nextFloat() < addToExampleProba) {
					removeRandomElement(exBuffer);
					exBuffer.add(example);
				}
			}
		}
	}
	
	private void removeRandomElement(Set<Object> set) {
		int atIndex = new Random().nextInt(set.size());
		int k = 0;
		Iterator<Object> it = set.iterator();
		it.next();
		while(k < atIndex) {
			k++;
			it.next();
		}
		it.remove();
	}
	
	/* (non-Javadoc)
	 * @see fr.cnrs.uima.profiler.ITaskProfiler#pointStatus(java.lang.String, java.lang.Object)
	 */
	@Override
	public void pointStatus(String point, Object message) {
		pointStatus.put(point, message);
	}


	/* (non-Javadoc)
	 * @see fr.cnrs.uima.profiler.ITaskProfiler#start(java.lang.Object, java.lang.String)
	 */
	@Override
	public void start(Object o, String taskName) {
		String classTaskId = getClassTaskId(o, taskName);
		ProfilingTask task = new ProfilingTask();
		tasksById.put(classTaskId, task);
		task.start();
	}

	private String getClassTaskId(Object o, String taskName) {
		return System.identityHashCode(o) + ":" + taskName;
	}

	private String getClassTaskName(Object o, String taskName) {
		return o.getClass().getSimpleName() + ":" + taskName;
	}

	/* (non-Javadoc)
	 * @see fr.cnrs.uima.profiler.ITaskProfiler#stop(java.lang.Object, java.lang.String)
	 */
	@Override
	public long stop(Object o, String taskName) {
		String classTaskName = getClassTaskName(o, taskName);
		ProfilingTask task = tasksById.get(getClassTaskId(o, taskName));
		tasks.put(classTaskName, task);
		return task.stop();
	}
	
	/* (non-Javadoc)
	 * @see fr.cnrs.uima.profiler.ITaskProfiler#display(java.io.PrintStream)
	 */
	@Override
	public void display(PrintStream stream) {
		if(isEmpty())
			return;
		stream.println("###########################################################################################");
		stream.println("#################################### " + this.name + " ####################################");
		stream.println("###########################################################################################");
		String formatString = "%30s %10sms\n";
		if(!tasks.isEmpty()) {
			stream.println("--------------- Tasks --------------");
			for(String taskName:tasks.keySet()) {
				long t = 0;
				for(ProfilingTask task:tasks.get(taskName))
					t += task.getTotal();
				stream.format(
						formatString,
						taskName,
						t
				);
			}
			stream.format(
					formatString,
					"TOTAL",
					getTotal());
		}
		if(!counters.isEmpty()) {
			stream.println("--------------- Hits ------------------");
			formatString = "%30s %10s\n";
			long total = 0;
			Comparator<String> comp = new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return Integer.compare(counters.get(o2).intValue(), counters.get(o1).intValue());
				}
			};
			List<String> sortedkeys = new LinkedList<String>();
			sortedkeys.addAll(counters.keySet());
			Collections.sort(sortedkeys, comp);
			for(String hitName:sortedkeys) {
				int h = counters.get(hitName).intValue();
				total += h;
				stream.format(
						formatString,
						hitName,
						h
				);
			}
			stream.format(
					formatString,
					"TOTAL",
					total);
			if(latexFormat) {
				stream.println("--------------- Hits (Latex) ----------");
				total = 0;
				sortedkeys = new LinkedList<String>();
				sortedkeys.addAll(counters.keySet());
				Collections.sort(sortedkeys, comp);
				for(String hitName:sortedkeys) {
					int h = counters.get(hitName).intValue();
					total += h;
					stream.println(hitName + " & " + counters.get(hitName).intValue() + " \\\\");
				}
				stream.println("TOTAL & " + total + " \\\\");
			}
		}
		if(!examples.isEmpty()) {
			stream.println("-------------- Examples ---------------");
			List<String> keySet = Lists.newArrayList(examples.keySet());
			Collections.shuffle(keySet);
			for(String hitName:keySet) {
				int i = 0;
				stream.println("* " + hitName);
				for(Object o:examples.get(hitName)) {
					i++;
					if(i>exampleLimit)
						break;
					String str = o == null ? "null" : o.toString().replaceAll("\n", " ").replaceAll("\r", " ").toLowerCase();
					stream.println("\t" + str);
				}
			}
		}
		if(!pointStatus.isEmpty()) {
			stream.println("-------------- Point status -----------");
			for(String point:pointStatus.keySet()) {
				stream.println(point + ": " + pointStatus.get(point));
			}
		}
		stream.flush();
	}

	private boolean isEmpty() {
		return tasks.isEmpty() && counters.isEmpty() && pointStatus.isEmpty();
	}


	private long getTotal() {
		long t = 0;
		for(String taskName:tasks.keySet()) {
			for(ProfilingTask task:tasks.get(taskName))
				t += task.getTotal();
		}
		return t;
	}

	public static void displayAll(PrintStream ps) {
		for(String name:profilers.keySet())
			profilers.get(name).display(ps);
	}

	public static void resetAll() {
		profilers.clear();
	}
	
	public static boolean isActivated() {
		return activated;
	}

}
