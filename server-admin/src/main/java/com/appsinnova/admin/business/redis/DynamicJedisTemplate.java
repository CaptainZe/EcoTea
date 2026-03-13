package com.appsinnova.admin.business.redis;

import java.util.Map;

public class DynamicJedisTemplate {
    private Map<String, JedisTemplate> jedisClusterTemplateMap;

    public DynamicJedisTemplate() {
    }

    public Map<String, JedisTemplate> getJedisClusterTemplateMap() {
        return this.jedisClusterTemplateMap;
    }

    public void setJedisClusterTemplateMap(final Map<String, JedisTemplate> jedisClusterTemplateMap) {
        this.jedisClusterTemplateMap = jedisClusterTemplateMap;
    }

    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DynamicJedisTemplate)) {
            return false;
        } else {
            DynamicJedisTemplate other = (DynamicJedisTemplate)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object this$jedisClusterTemplateMap = this.getJedisClusterTemplateMap();
                Object other$jedisClusterTemplateMap = other.getJedisClusterTemplateMap();
                if (this$jedisClusterTemplateMap == null) {
                    if (other$jedisClusterTemplateMap != null) {
                        return false;
                    }
                } else if (!this$jedisClusterTemplateMap.equals(other$jedisClusterTemplateMap)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(final Object other) {
        return other instanceof DynamicJedisTemplate;
    }

    public int hashCode() {
        int result = 1;
        Object $jedisClusterTemplateMap = this.getJedisClusterTemplateMap();
        result = result * 59 + ($jedisClusterTemplateMap == null ? 43 : $jedisClusterTemplateMap.hashCode());
        return result;
    }

    public String toString() {
        return "DynamicJedisTemplate(jedisClusterTemplateMap=" + this.getJedisClusterTemplateMap() + ")";
    }
}
