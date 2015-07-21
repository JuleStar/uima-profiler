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

public class DoNothingProfiler implements ITaskProfiler {

	private static final DoNothingProfiler instance = new DoNothingProfiler();
	
	public static DoNothingProfiler getInstance() {
		return instance;
	}
	
	@Override
	public void hit(String hitName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void hit(String hitName, Object example) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pointStatus(String point, Object message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start(Object o, String taskName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long stop(Object o, String taskName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void display(PrintStream stream) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initHit(String name) {
		// TODO Auto-generated method stub
		
	}
}
