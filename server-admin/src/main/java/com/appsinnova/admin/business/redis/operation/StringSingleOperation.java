package com.appsinnova.admin.business.redis.operation;

import java.util.List;

public interface StringSingleOperation {
    List<String> mget(String... keys);

    void mset(String... keysvalues);
}
