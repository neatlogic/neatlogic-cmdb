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

package neatlogic.module.cmdb.startup.handler;

import neatlogic.framework.tenantinit.TenantInitBase;
import neatlogic.module.cmdb.service.ci.CiService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author linbq
 * @since 2022/2/9 12:12
 **/
@Component
public class CreateCiTableViewInitHandler extends TenantInitBase {
    @Resource
    private CiService ciService;

    /**
     * 作业名称
     *
     * @return 字符串
     */
    @Override
    public String getName() {
        return "创建ci动态表或视图";
    }


    @Override
    public void execute() {
        ciService.initCiTableView();
    }

    /**
     * 排序
     *
     * @return 顺序
     */
    @Override
    public int sort() {
        return 1;
    }
}
