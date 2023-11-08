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

package com.luckykuang.transaction.mapper;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * @author luckykuang
 * @date 2023/11/7 11:06
 */
class UserMapperTest {

    private static final int MAX = 100 ;

    private SqlSessionFactory sqlSessionFactory ;
    private Thread[] threads = new Thread[MAX] ;
    private CountDownLatch cdl = new CountDownLatch(MAX) ;

    @BeforeEach
    void setUp() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Test
    void getUserList() throws IOException {
        SqlSession session = sqlSessionFactory.openSession() ;
        UserMapper mapper = session.getMapper(UserMapper.class) ;

        for (int i = 0; i < MAX; i++) {
            threads[i] = new Thread(() -> {
                try {
                    cdl.await() ;
                    System.out.println(mapper.getUserList()) ;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }) ;
        }
        for (int i = 0; i < MAX; i++) {
            threads[i].start() ;
            cdl.countDown() ;
        }
        System.in.read() ;
    }
}