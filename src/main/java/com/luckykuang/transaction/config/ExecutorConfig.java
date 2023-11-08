/*
 * Copyright 2015-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.luckykuang.transaction.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author luckykuang
 * @date 2023/11/7 11:32
 */
public class ExecutorConfig {
    private static final int FULL_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static volatile ExecutorService executorService;
    private ExecutorConfig(){}

    public static ExecutorService getThreadPool() {
        if (executorService == null){
            synchronized (ExecutorConfig.class){
                if (executorService == null){
                    executorService =  newThreadPool();
                }
            }
        }
        return executorService;
    }

    private static ExecutorService newThreadPool(){
        int corePool = Math.max(4, FULL_PROCESSORS);
        System.out.println("线程数量：" + corePool);
        return new ThreadPoolExecutor(
                corePool,
                corePool,
                0L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(FULL_PROCESSORS * 100),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
