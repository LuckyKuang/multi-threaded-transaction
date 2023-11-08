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

package com.luckykuang.transaction.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckykuang.transaction.config.ExecutorConfig;
import com.luckykuang.transaction.config.SqlContext;
import com.luckykuang.transaction.entity.User;
import com.luckykuang.transaction.mapper.UserMapper;
import com.luckykuang.transaction.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author luckykuang
 * @date 2023/11/7 10:56
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private SqlContext sqlContext;

    @Override
    public List<User> getUserList() {
        return userMapper.getUserList();
    }

    /**
     * 队列中等待的线程过多时，本方法速度最慢，超级无敌慢，除非业务需要，否则强烈不推荐
     * @param users
     * @throws SQLException
     */
    @Override
    public void saveUsersByInvokeAll(List<User> users) throws SQLException {
        // 获取数据库连接,获取会话(内部自有事务)
        SqlSession sqlSession = sqlContext.getSqlSession();
        Connection connection = sqlSession.getConnection();
        try {
            // 设置手动提交
            connection.setAutoCommit(false);
            UserMapper userMapperSession = sqlSession.getMapper(UserMapper.class);
            // 先做删除操作
            userMapperSession.delete(null);
            ExecutorService threadPool = ExecutorConfig.getThreadPool();
            List<Callable<Integer>> callableList = new ArrayList<>();
            List<List<User>> lists = ListUtils.partition(users, 200);
            for (List<User> list : lists) {
                Callable<Integer> callable = () -> userMapperSession.saveBatch(list);
                callableList.add(callable);
            }
            // 执行子线程 此处等待所有的子线程全部执行完成，程序才会继续往下走
            List<Future<Integer>> futures = threadPool.invokeAll(callableList);
            for (Future<Integer> future : futures) {
                Integer integer = future.get();
                if (integer <= 0) {
                    connection.rollback();
                    return;
                }
            }
            connection.commit();
            log.info("添加用户完毕");
        } catch (Exception e) {
            connection.rollback();
            log.info("添加用户异常", e);
            throw new RuntimeException("添加用户异常");
        }
    }

    /**
     * 推荐此种写法
     * @param users
     * @throws SQLException
     */
    @Override
    public void saveUsersBySubmit(List<User> users) throws SQLException {
        // 获取数据库连接,获取会话(内部自有事务)
        SqlSession sqlSession = sqlContext.getSqlSession();
        Connection connection = sqlSession.getConnection();
        try {
            // 设置手动提交
            connection.setAutoCommit(false);
            UserMapper userMapperSession = sqlSession.getMapper(UserMapper.class);
            // 先做删除操作
            userMapperSession.delete(null);
            ExecutorService threadPool = ExecutorConfig.getThreadPool();
            List<Future<Integer>> futures = new ArrayList<>();
            List<List<User>> lists = ListUtils.partition(users, 200);
            for (List<User> list : lists) {
                // 异步处理数据
                Future<Integer> future = threadPool.submit(() -> userMapperSession.saveBatch(list));
                futures.add(future);
            }
            for (Future<Integer> future : futures) {
                Integer integer = future.get();
                if (integer <= 0) {
                    connection.rollback();
                    return;
                }
            }
            connection.commit();
            log.info("添加用户完毕");
        } catch (Exception e) {
            connection.rollback();
            log.info("添加用户异常", e);
            throw new RuntimeException("添加用户异常");
        }
    }

    /**
     * 此方法速度接近Future，也推荐
     * @param users
     * @throws SQLException
     */
    @Override
    public void saveUsersByCompletableFuture(List<User> users) throws SQLException {
        // 获取数据库连接,获取会话(内部自有事务)
        SqlSession sqlSession = sqlContext.getSqlSession();
        Connection connection = sqlSession.getConnection();
        try {
            // 设置手动提交
            connection.setAutoCommit(false);
            UserMapper userMapperSession = sqlSession.getMapper(UserMapper.class);
            // 先做删除操作
            userMapperSession.delete(null);
            ExecutorService threadPool = ExecutorConfig.getThreadPool();
            List<CompletableFuture<Integer>> futures = new ArrayList<>();
            List<List<User>> lists = ListUtils.partition(users, 200);
            for (List<User> list : lists) {
                // 异步处理数据
                CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> userMapperSession.saveBatch(list), threadPool);
                futures.add(future);
            }
            // 等待所有异步任务完成(此处会阻塞，直到所有线程处理完成)
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            connection.commit();
            log.info("添加用户完毕");
        } catch (Exception e) {
            connection.rollback();
            log.info("添加用户异常", e);
            throw new RuntimeException("添加用户异常");
        }
    }
}
