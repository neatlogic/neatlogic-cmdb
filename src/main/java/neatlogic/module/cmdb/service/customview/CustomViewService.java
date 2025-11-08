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

import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CustomViewService {

    void updateCustomViewActive(CustomViewVo customViewVo);

    CustomViewVo getCustomViewById(Long id);

    CustomViewVo getCustomViewDetailById(Long id);

    List<CustomViewVo> searchCustomView(CustomViewVo customViewVo);

    @Transactional
    void insertCustomView(CustomViewVo customViewVo);

    @Transactional
    void updateCustomView(CustomViewVo customViewVo);

    void buildCustomView(String sql);

    @Transactional
    void deleteCustomView(Long id);

    void parseConfig(CustomViewVo customViewVo);
}
