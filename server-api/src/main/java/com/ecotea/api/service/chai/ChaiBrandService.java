package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.mapper.chai.ChaiBrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChaiBrandService {

    private final ChaiBrandMapper chaiBrandMapper;

    /**
     * 上架品牌：按首字母 A-Z，同字母再按每字首字母串；非 A-Z 垫底（与 admin listOnlineOrdered 一致）
     */
    public List<ChaiBrand> listOnlineOrdered() {
        List<ChaiBrand> list = chaiBrandMapper.selectList(new LambdaQueryWrapper<ChaiBrand>()
                .eq(ChaiBrand::getStatus, 1));
        list.sort(Comparator
                .comparingInt((ChaiBrand b) -> isLetterInitial(b.getNameInitial()) ? 0 : 1)
                .thenComparing(b -> nullToEmpty(b.getNameInitial()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> nullToEmpty(b.getNamePinyin()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> nullToEmpty(b.getName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> b.getId() == null ? 0L : b.getId()));
        return list;
    }

    public List<ChaiBrand> listAll() {
        return chaiBrandMapper.selectList(new LambdaQueryWrapper<ChaiBrand>()
                .orderByDesc(ChaiBrand::getOrderNum)
                .orderByDesc(ChaiBrand::getUpdateTime));
    }

    private static boolean isLetterInitial(String initial) {
        if (initial == null || initial.length() != 1) {
            return false;
        }
        char c = initial.charAt(0);
        return c >= 'A' && c <= 'Z';
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
