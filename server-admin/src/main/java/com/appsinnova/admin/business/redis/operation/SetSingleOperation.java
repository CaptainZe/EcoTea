package com.appsinnova.admin.business.redis.operation;

import java.util.Set;

public interface SetSingleOperation {
    Set<String> sdiff(String... keys);

    Set<String> sunion(String... keys);

    long sunionstore(String key, String... keys);
}

