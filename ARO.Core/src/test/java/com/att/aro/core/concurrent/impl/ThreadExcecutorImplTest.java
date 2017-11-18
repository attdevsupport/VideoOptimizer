package com.att.aro.core.concurrent.impl;

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import com.att.aro.core.BaseTest;
import com.att.aro.core.concurrent.IThreadExecutor;

public class ThreadExcecutorImplTest extends BaseTest {
	IThreadExecutor exec;
	
	@Before
	public void setup(){
		exec = context.getBean(IThreadExecutor.class);
	}
	
	@Test
	public void execute(){
		MyWorker worker = new MyWorker();
		exec.execute(worker);
	}
	@Test
	public void executeFuture() throws InterruptedException, ExecutionException{
		MyWorker worker = new MyWorker();
		exec.executeFuture(worker);
	}
	private class MyWorker implements Runnable{
		@Override
		public void run() {
		}
	}
}
