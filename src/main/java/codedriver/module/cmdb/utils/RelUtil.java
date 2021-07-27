/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.utils;

import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class RelUtil {
    /**
     * 如果出现子模型引用父模型的情况，会由于集成关系出现两条关系，所以需要去除
     *
     * @param relList 关系列表
     * @return 关系列表
     */
    public static List<RelVo> ClearRepeatRel(List<RelVo> relList) {

        List<RelVo> originalRelList = relList.stream().filter(rel -> rel.getIsExtended().equals(0)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(originalRelList)) {
            relList.removeIf(rel -> rel.getIsExtended().equals(1) && originalRelList.stream().anyMatch(er -> er.getFromCiId().equals(rel.getFromCiId()) && er.getToCiId().equals(rel.getToCiId())));
        }
        return relList;
    }

    public static List<CiViewVo> ClearCiViewRepeatRel(List<CiViewVo> ciViewList) {
        List<CiViewVo> originalRelList = ciViewList.stream().filter(view -> view.getIsExtended().equals(0) && view.getType().startsWith("rel")).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(originalRelList)) {
            ciViewList.removeIf(view -> view.getIsExtended().equals(1) && view.getType().startsWith("rel") && originalRelList.stream().anyMatch(er -> er.getUniqueKey().equals(view.getUniqueKey())));
        }
        return ciViewList;
    }
}
