package com.appsinnova.admin.business.service.sys;

import com.appsinnova.admin.business.domain.sys.AppSecretKey;
import com.appsinnova.admin.business.repository.sys.AppSecretKeyRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Flushable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppSecretKeyService implements Flushable, CommandLineRunner {

    private final AppSecretKeyRepository appSecretKeyRepository;

    // 内存缓存
    private Map<Integer, AppSecretKey> configMap;

    @PostConstruct
    public void init() {
        configMap = new ConcurrentHashMap<>();
    }

    @Override
    public void run(String... args) {
        log.info("AppSecretKeyService run start...");

        this.flushData();

        log.info("AppSecretKeyService run end...");
    }

    @Scheduled(cron = "29 30 */1 * * ?")
    @Override
    public void flush() {
        log.info("AppSecretKeyService flush start...");

        this.flushData();

        log.info("AppSecretKeyService flush end...");
    }

    private void flushData() {
        List<AppSecretKey> list = appSecretKeyRepository.findAll();
        if (CollectionUtils.isEmpty(list)) {
            configMap.clear();
            return;
        }

        Map<Integer, AppSecretKey> configMapTemp = new HashMap<>();
        for (AppSecretKey model : list) {
            configMapTemp.put(model.getType(), model);
        }

        configMap.clear();
        configMap.putAll(configMapTemp);

        log.info("AppSecretKey flushData done! count: {}", configMap.size());
    }

    public AppSecretKey getSecretKey(Integer type) {
        if (configMap.containsKey(type)) {
            return configMap.get(type);
        }
        return null;
    }

    //////////////////////////////////////////////////////////////////////////

    public AppSecretKey getById(Long id) {
        return appSecretKeyRepository.findById(id).orElse(null);
    }

    public Page<AppSecretKey> getPageList(AppSecretKey param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "createTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return appSecretKeyRepository.findAll((Root<AppSecretKey> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = this.genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public AppSecretKey save(AppSecretKey entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return appSecretKeyRepository.save(entity);
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        appSecretKeyRepository.deleteByIdIn(idList);
    }

    private List<Predicate> genCondition(Root<AppSecretKey> root, CriteriaBuilder cb, AppSecretKey param) {
        List<Predicate> preList = new ArrayList<>();

        if (param.getType() != null) {
            preList.add(cb.equal(root.get("type").as(Integer.class), param.getType()));
        }

        return preList;
    }
}