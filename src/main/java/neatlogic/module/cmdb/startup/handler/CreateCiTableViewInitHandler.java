/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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
