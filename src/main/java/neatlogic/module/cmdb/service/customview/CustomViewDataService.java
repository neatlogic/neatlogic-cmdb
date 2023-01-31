/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.service.customview;

import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConditionVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewDataGroupVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewDataVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;

import java.util.List;
import java.util.Map;

public interface CustomViewDataService {
    CustomViewDataVo getCustomViewData(CustomViewConditionVo customViewConditionVo);

    List<Map<String, Object>> searchCustomViewDataFlatten(CustomViewConditionVo customViewConditionVo);

    List<Map<String, Object>> searchCustomViewData(CustomViewConditionVo customViewConditionVo);


    CustomViewVo getCustomViewCiEntityById(CustomViewConditionVo customViewConditionVo);

    List<CiEntityVo> searchCustomViewCiEntity(CustomViewConditionVo customViewConditionVo);

    List<CustomViewDataGroupVo> searchCustomViewDataGroup(CustomViewConditionVo customViewConditionVo);
}
