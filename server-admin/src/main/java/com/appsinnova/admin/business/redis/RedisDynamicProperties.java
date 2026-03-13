package com.appsinnova.admin.business.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(
        prefix = "igg.redis.dynamic"
)
public class RedisDynamicProperties {
    private Map<String, RedisProperties> redis;
    private boolean enabled = false;

    public RedisDynamicProperties() {
    }

    public Map<String, RedisProperties> getRedis() {
        return this.redis;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setRedis(final Map<String, RedisProperties> redis) {
        this.redis = redis;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof RedisDynamicProperties)) {
            return false;
        } else {
            RedisDynamicProperties other = (RedisDynamicProperties)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object this$redis = this.getRedis();
                Object other$redis = other.getRedis();
                if (this$redis == null) {
                    if (other$redis == null) {
                        return this.isEnabled() == other.isEnabled();
                    }
                } else if (this$redis.equals(other$redis)) {
                    return this.isEnabled() == other.isEnabled();
                }

                return false;
            }
        }
    }

    protected boolean canEqual(final Object other) {
        return other instanceof RedisDynamicProperties;
    }

    public int hashCode() {
        int result = 1;
        Object $redis = this.getRedis();
        result = result * 59 + ($redis == null ? 43 : $redis.hashCode());
        result = result * 59 + (this.isEnabled() ? 79 : 97);
        return result;
    }

    public String toString() {
        return "RedisDynamicProperties(redis=" + this.getRedis() + ", enabled=" + this.isEnabled() + ")";
    }
}
