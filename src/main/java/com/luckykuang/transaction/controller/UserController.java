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

package com.luckykuang.transaction.controller;

import com.luckykuang.transaction.entity.User;
import com.luckykuang.transaction.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

/**
 * @author luckykuang
 * @date 2023/11/7 10:55
 */
@RestController
@RequestMapping("user")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("find")
    public List<User> find(){
        return userService.getUserList();
    }

    @PostMapping("save1")
    public String save(@RequestBody List<User> users) throws SQLException {
        userService.saveUsersByInvokeAll(users);
        return "success";
    }

    @PostMapping("save2")
    public String save2(@RequestBody List<User> users) throws SQLException {
        userService.saveUsersBySubmit(users);
        return "success";
    }

    @PostMapping("save3")
    public String save3(@RequestBody List<User> users) throws SQLException {
        userService.saveUsersByCompletableFuture(users);
        return "success";
    }
}
