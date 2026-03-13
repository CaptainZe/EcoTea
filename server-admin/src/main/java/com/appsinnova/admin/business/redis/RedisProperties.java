package com.appsinnova.admin.business.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(
        prefix = "igg.redis"
)
public class RedisProperties {
    private int maxTotal = 100;
    private int maxIdle = 100;
    private int minIdle = 5;
    private int maxWaitMillis = 10000;
    private boolean testOnBorrow = false;
    private boolean testOnReturn = false;
    private boolean testWhileIdle = true;
    private int timeout = 30000;
    private int timeBetweenEvictionRunsMillis = 30000;
    private int minEvictableIdleTimeMills = 60000;
    private int numTestsPerEvictionRun = 50;
    private String keyPrefix;
    private String ip;
    private int port;
    private List<String> host;
    private String password;

    public RedisProperties() {
    }

    public int getMaxTotal() {
        return this.maxTotal;
    }

    public int getMaxIdle() {
        return this.maxIdle;
    }

    public int getMinIdle() {
        return this.minIdle;
    }

    public int getMaxWaitMillis() {
        return this.maxWaitMillis;
    }

    public boolean isTestOnBorrow() {
        return this.testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return this.testOnReturn;
    }

    public boolean isTestWhileIdle() {
        return this.testWhileIdle;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public int getTimeBetweenEvictionRunsMillis() {
        return this.timeBetweenEvictionRunsMillis;
    }

    public int getMinEvictableIdleTimeMills() {
        return this.minEvictableIdleTimeMills;
    }

    public int getNumTestsPerEvictionRun() {
        return this.numTestsPerEvictionRun;
    }

    public String getKeyPrefix() {
        return this.keyPrefix;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public List<String> getHost() {
        return this.host;
    }

    public String getPassword() {
        return this.password;
    }

    public void setMaxTotal(final int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public void setMaxIdle(final int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public void setMinIdle(final int minIdle) {
        this.minIdle = minIdle;
    }

    public void setMaxWaitMillis(final int maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public void setTestOnBorrow(final boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public void setTestOnReturn(final boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public void setTestWhileIdle(final boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public void setTimeBetweenEvictionRunsMillis(final int timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public void setMinEvictableIdleTimeMills(final int minEvictableIdleTimeMills) {
        this.minEvictableIdleTimeMills = minEvictableIdleTimeMills;
    }

    public void setNumTestsPerEvictionRun(final int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    public void setKeyPrefix(final String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setHost(final List<String> host) {
        this.host = host;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof RedisProperties)) {
            return false;
        } else {
            RedisProperties other = (RedisProperties)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getMaxTotal() != other.getMaxTotal()) {
                return false;
            } else if (this.getMaxIdle() != other.getMaxIdle()) {
                return false;
            } else if (this.getMinIdle() != other.getMinIdle()) {
                return false;
            } else if (this.getMaxWaitMillis() != other.getMaxWaitMillis()) {
                return false;
            } else if (this.isTestOnBorrow() != other.isTestOnBorrow()) {
                return false;
            } else if (this.isTestOnReturn() != other.isTestOnReturn()) {
                return false;
            } else if (this.isTestWhileIdle() != other.isTestWhileIdle()) {
                return false;
            } else if (this.getTimeout() != other.getTimeout()) {
                return false;
            } else if (this.getTimeBetweenEvictionRunsMillis() != other.getTimeBetweenEvictionRunsMillis()) {
                return false;
            } else if (this.getMinEvictableIdleTimeMills() != other.getMinEvictableIdleTimeMills()) {
                return false;
            } else if (this.getNumTestsPerEvictionRun() != other.getNumTestsPerEvictionRun()) {
                return false;
            } else {
                Object this$keyPrefix = this.getKeyPrefix();
                Object other$keyPrefix = other.getKeyPrefix();
                if (this$keyPrefix == null) {
                    if (other$keyPrefix != null) {
                        return false;
                    }
                } else if (!this$keyPrefix.equals(other$keyPrefix)) {
                    return false;
                }

                Object this$ip = this.getIp();
                Object other$ip = other.getIp();
                if (this$ip == null) {
                    if (other$ip != null) {
                        return false;
                    }
                } else if (!this$ip.equals(other$ip)) {
                    return false;
                }

                if (this.getPort() != other.getPort()) {
                    return false;
                } else {
                    label75: {
                        Object this$host = this.getHost();
                        Object other$host = other.getHost();
                        if (this$host == null) {
                            if (other$host == null) {
                                break label75;
                            }
                        } else if (this$host.equals(other$host)) {
                            break label75;
                        }

                        return false;
                    }

                    Object this$password = this.getPassword();
                    Object other$password = other.getPassword();
                    if (this$password == null) {
                        if (other$password != null) {
                            return false;
                        }
                    } else if (!this$password.equals(other$password)) {
                        return false;
                    }

                    return true;
                }
            }
        }
    }

    protected boolean canEqual(final Object other) {
        return other instanceof RedisProperties;
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + this.getMaxTotal();
        result = result * 59 + this.getMaxIdle();
        result = result * 59 + this.getMinIdle();
        result = result * 59 + this.getMaxWaitMillis();
        result = result * 59 + (this.isTestOnBorrow() ? 79 : 97);
        result = result * 59 + (this.isTestOnReturn() ? 79 : 97);
        result = result * 59 + (this.isTestWhileIdle() ? 79 : 97);
        result = result * 59 + this.getTimeout();
        result = result * 59 + this.getTimeBetweenEvictionRunsMillis();
        result = result * 59 + this.getMinEvictableIdleTimeMills();
        result = result * 59 + this.getNumTestsPerEvictionRun();
        Object $keyPrefix = this.getKeyPrefix();
        result = result * 59 + ($keyPrefix == null ? 43 : $keyPrefix.hashCode());
        Object $ip = this.getIp();
        result = result * 59 + ($ip == null ? 43 : $ip.hashCode());
        result = result * 59 + this.getPort();
        Object $host = this.getHost();
        result = result * 59 + ($host == null ? 43 : $host.hashCode());
        Object $password = this.getPassword();
        result = result * 59 + ($password == null ? 43 : $password.hashCode());
        return result;
    }

    public String toString() {
        return "RedisProperties(maxTotal=" + this.getMaxTotal() + ", maxIdle=" + this.getMaxIdle() + ", minIdle=" + this.getMinIdle() + ", maxWaitMillis=" + this.getMaxWaitMillis() + ", testOnBorrow=" + this.isTestOnBorrow() + ", testOnReturn=" + this.isTestOnReturn() + ", testWhileIdle=" + this.isTestWhileIdle() + ", timeout=" + this.getTimeout() + ", timeBetweenEvictionRunsMillis=" + this.getTimeBetweenEvictionRunsMillis() + ", minEvictableIdleTimeMills=" + this.getMinEvictableIdleTimeMills() + ", numTestsPerEvictionRun=" + this.getNumTestsPerEvictionRun() + ", keyPrefix=" + this.getKeyPrefix() + ", ip=" + this.getIp() + ", port=" + this.getPort() + ", host=" + this.getHost() + ", password=" + this.getPassword() + ")";
    }
}