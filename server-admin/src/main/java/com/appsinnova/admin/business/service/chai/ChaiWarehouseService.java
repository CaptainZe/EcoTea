package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.pca.PcaCodeService;
import com.appsinnova.admin.business.domain.chai.ChaiWarehouse;
import com.appsinnova.admin.business.repository.chai.ChaiWarehouseRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChaiWarehouseService {

    private final ChaiWarehouseRepository chaiWarehouseRepository;
    private final PcaCodeService pcaCodeService;

    public ChaiWarehouse getById(Long id) {
        return chaiWarehouseRepository.findById(id).orElse(null);
    }

    public Page<ChaiWarehouse> getPageList(ChaiWarehouse param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        Page<ChaiWarehouse> result = chaiWarehouseRepository.findAll(
                (Root<ChaiWarehouse> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
                    List<Predicate> preList = genCondition(root, cb, param);
                    Predicate[] pres = new Predicate[preList.size()];
                    return query.where(preList.toArray(pres)).getRestriction();
                }, page);
        result.getContent().forEach(this::fillRegionNames);
        return result;
    }

    /**
     * 上架仓库，按排序号大到小
     */
    public List<ChaiWarehouse> listOnlineOrdered() {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "id"));
        return chaiWarehouseRepository.findAll(
                (Root<ChaiWarehouse> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                        cb.equal(root.get("status").as(Integer.class), 1),
                Sort.by(orders));
    }

    public boolean isNameTakenByOther(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (excludeId == null) {
            return chaiWarehouseRepository.findFirstByName(trimmed).isPresent();
        }
        return chaiWarehouseRepository.existsByNameAndIdNot(trimmed, excludeId);
    }

    public boolean isReferenced(Long whId) {
        if (whId == null) {
            return false;
        }
        return chaiWarehouseRepository.countStockWhByWhId(whId) > 0
                || chaiWarehouseRepository.countBillsByWhId(whId) > 0;
    }

    public ChaiWarehouse save(ChaiWarehouse entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return chaiWarehouseRepository.save(entity);
    }

    /**
     * 未被分仓结存或库存单据引用的才物理删。
     */
    @Transactional
    public List<String> deleteByIdIn(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> seen = new LinkedHashSet<>();
        List<Long> toDelete = new ArrayList<>();
        List<String> blockedNames = new ArrayList<>();
        for (Long id : idList) {
            if (id == null || !seen.add(id)) {
                continue;
            }
            ChaiWarehouse wh = chaiWarehouseRepository.findById(id).orElse(null);
            if (wh == null) {
                continue;
            }
            if (isReferenced(id)) {
                blockedNames.add(wh.getName());
            } else {
                toDelete.add(id);
            }
        }
        if (toDelete.isEmpty() && !blockedNames.isEmpty()) {
            throw new IllegalArgumentException(formatAllBlocked(blockedNames));
        }
        if (!toDelete.isEmpty()) {
            chaiWarehouseRepository.deleteByIdIn(toDelete);
        }
        return blockedNames;
    }

    public void fillRegionNames(ChaiWarehouse warehouse) {
        if (warehouse == null) {
            return;
        }
        warehouse.setProvinceName(pcaCodeService.resolveName(warehouse.getProvince()));
        warehouse.setCityName(pcaCodeService.resolveName(warehouse.getCity()));
        warehouse.setDistrictName(pcaCodeService.resolveName(warehouse.getDistrict()));
    }

    private static String formatAllBlocked(List<String> names) {
        if (names.size() == 1) {
            return "「" + names.get(0) + "」已被库存引用，无法删除，请先下架";
        }
        return "以下已被库存引用，无法删除，请先下架：" + String.join("、", names);
    }

    private List<Predicate> genCondition(Root<ChaiWarehouse> root, CriteriaBuilder cb, ChaiWarehouse param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName().trim() + "%"));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        if (StringUtils.hasText(param.getProvince())) {
            preList.add(cb.equal(root.get("province").as(String.class), param.getProvince().trim()));
        }
        if (StringUtils.hasText(param.getCity())) {
            preList.add(cb.equal(root.get("city").as(String.class), param.getCity().trim()));
        }
        if (StringUtils.hasText(param.getDistrict())) {
            preList.add(cb.equal(root.get("district").as(String.class), param.getDistrict().trim()));
        }
        return preList;
    }
}
