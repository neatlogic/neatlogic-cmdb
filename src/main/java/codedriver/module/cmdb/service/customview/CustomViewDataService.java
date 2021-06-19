/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.customview;

import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionVo;
import codedriver.framework.cmdb.dto.customview.CustomViewDataGroupVo;
import codedriver.framework.cmdb.dto.customview.CustomViewDataVo;

import java.util.List;
import java.util.Map;

public interface CustomViewDataService {
    CustomViewDataVo getCustomViewData(CustomViewConditionVo customViewConditionVo);

    List<Map<String, Object>> searchCustomViewData(CustomViewConditionVo customViewConditionVo);


    List<Map<String, Long>> getCustomViewCiEntityIdById(CustomViewConditionVo customViewConditionVo);

    List<CiEntityVo> searchCustomViewCiEntity(CustomViewConditionVo customViewConditionVo);

    List<CustomViewDataGroupVo> searchCustomViewDataGroup(CustomViewConditionVo customViewConditionVo);
}
