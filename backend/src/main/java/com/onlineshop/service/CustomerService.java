package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.dto.RegisterRequest;
import com.onlineshop.entity.Customer;
import com.onlineshop.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 客户服务：注册（US-36）、登录校验、查询当前客户
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final String USERNAME_TAKEN = "用户名已被注册";

    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册客户，密码用 BCrypt 加密后保存。用户名已存在时提示不可用；
     * 两个人同时注册同一个用户名时，数据库的唯一约束保证只有一个能成功。
     */
    public Customer register(RegisterRequest request) {
        if (customerMapper.selectByUsername(request.getUsername()) != null) {
            throw new BizException(USERNAME_TAKEN);
        }
        Customer customer = new Customer();
        customer.setUsername(request.getUsername());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setPhone(request.getPhone());
        customer.setDefaultLocation(request.getDefaultLocation().trim());
        customer.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        try {
            customerMapper.insert(customer);
        } catch (DuplicateKeyException e) {
            throw new BizException(USERNAME_TAKEN);
        }
        return customer;
    }

    /**
     * 校验用户名和密码，成功返回客户。
     * 用户名不存在和密码错误给同一个提示，避免别人借此试出用户名。
     */
    public Customer login(String username, String password) {
        Customer customer = customerMapper.selectByUsername(username);
        if (customer == null || !passwordEncoder.matches(password, customer.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        return customer;
    }

    /**
     * 按 id 查客户，查不到时按未登录处理
     */
    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BizException(401, "请先登录");
        }
        return customer;
    }
}
