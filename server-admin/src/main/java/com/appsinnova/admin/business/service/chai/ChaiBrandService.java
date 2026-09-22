package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.utils.PinyinUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.repository.chai.ChaiBrandRepository;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChaiBrandService {

    private final ChaiBrandRepository chaiBrandRepository;
    private final ChaiSpuService chaiSpuService;

    public ChaiBrand getById(Long id) {
        return chaiBrandRepository.findById(id).orElse(null);
    }

    public Page<ChaiBrand> getPageList(ChaiBrand param) {
        List<Sort.Order> orders = new ArrayList<>();
        // 排序号大到小，其次按更新时间
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return chaiBrandRepository.findAll((Root<ChaiBrand> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public ChaiBrand getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return chaiBrandRepository.findFirstByName(name.trim()).orElse(null);
    }

    /**
     * 品牌名称是否已被其他记录占用
     */
    public boolean isNameTakenByOther(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (excludeId == null) {
            return chaiBrandRepository.findFirstByName(trimmed).isPresent();
        }
        return chaiBrandRepository.existsByNameAndIdNot(trimmed, excludeId);
    }

    public ChaiBrand save(ChaiBrand entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        fillNameLetters(entity);
        entity.setUpdateTime(System.currentTimeMillis());
        return chaiBrandRepository.save(entity);
    }

    /**
     * 根据名称重算首字母与每字首字母串。
     */
    public void fillNameLetters(ChaiBrand entity) {
        if (entity == null) {
            return;
        }
        String name = entity.getName() != null ? entity.getName().trim() : "";
        entity.setNameInitial(PinyinUtil.firstInitial(name));
        entity.setNamePinyin(PinyinUtil.nameInitials(name));
    }

    /**
     * 批量回填品牌拼音字段。
     *
     * @param forceAll true=全量重算；false=仅首字母或全字母为空的记录
     * @return updated / skipped / total
     */
    @Transactional
    public Map<String, Integer> fillNameLettersBatch(boolean forceAll) {
        List<ChaiBrand> list = chaiBrandRepository.findAll();
        int updated = 0;
        int skipped = 0;
        for (ChaiBrand brand : list) {
            if (!forceAll && StringUtils.hasText(brand.getNameInitial())
                    && StringUtils.hasText(brand.getNamePinyin())) {
                skipped++;
                continue;
            }
            String beforeInitial = brand.getNameInitial();
            String beforePinyin = brand.getNamePinyin();
            fillNameLetters(brand);
            if (Objects.equals(beforeInitial, brand.getNameInitial())
                    && Objects.equals(beforePinyin, brand.getNamePinyin())) {
                skipped++;
                continue;
            }
            chaiBrandRepository.save(brand);
            updated++;
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("updated", updated);
        result.put("skipped", skipped);
        return result;
    }

    /**
     * 首字母或全字母为空的品牌数量（工具页展示用）。
     */
    public long countMissingNameLetters() {
        return chaiBrandRepository.count((Root<ChaiBrand> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.or(
                        cb.isNull(root.get("nameInitial")),
                        cb.equal(root.get("nameInitial").as(String.class), ""),
                        cb.isNull(root.get("namePinyin")),
                        cb.equal(root.get("namePinyin").as(String.class), "")
                ));
    }

    /**
     * 上架品牌：按首字母 A-Z，同字母再按每字首字母串；非 A-Z 垫底。
     */
    public List<ChaiBrand> listOnlineOrdered() {
        List<ChaiBrand> list = chaiBrandRepository.findAll((Root<ChaiBrand> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("status").as(Integer.class), 1));
        list.sort(brandNameLetterComparator());
        return list;
    }

    private static Comparator<ChaiBrand> brandNameLetterComparator() {
        return Comparator
                .comparingInt((ChaiBrand b) -> PinyinUtil.isLetterInitial(b.getNameInitial()) ? 0 : 1)
                .thenComparing(b -> nullToEmpty(b.getNameInitial()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> nullToEmpty(b.getNamePinyin()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> nullToEmpty(b.getName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> b.getId() == null ? 0L : b.getId());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 未被商品引用的才物理删。
     *
     * @return 被引用而未删除的名称；空表示全部已删
     * @throws IllegalArgumentException 全部被引用，一条都未删
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
            ChaiBrand brand = chaiBrandRepository.findById(id).orElse(null);
            if (brand == null) {
                continue;
            }
            if (chaiSpuService.isBrandInUse(id)) {
                blockedNames.add(brand.getName());
            } else {
                toDelete.add(id);
            }
        }
        if (toDelete.isEmpty() && !blockedNames.isEmpty()) {
            throw new IllegalArgumentException(formatAllBlocked(blockedNames));
        }
        if (!toDelete.isEmpty()) {
            chaiBrandRepository.deleteByIdIn(toDelete);
        }
        return blockedNames;
    }

    private static String formatAllBlocked(List<String> names) {
        if (names.size() == 1) {
            return "「" + names.get(0) + "」已被商品引用，无法删除，请先下架";
        }
        return "以下已被商品引用，无法删除，请先下架：" + String.join("、", names);
    }

    private List<Predicate> genCondition(Root<ChaiBrand> root, CriteriaBuilder cb, ChaiBrand param) {
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
        return preList;
    }
}
