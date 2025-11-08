/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
