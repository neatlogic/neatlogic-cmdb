/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.rebuilddatabaseview.handler;

import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.rebuilddatabaseview.core.IRebuildDataBaseView;
import neatlogic.framework.rebuilddatabaseview.core.ViewStatusInfo;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.service.customview.CustomViewService;
import neatlogic.module.cmdb.utils.CustomViewBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class CustomViewRebuildHandler implements IRebuildDataBaseView {

    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private CustomViewService customViewService;

    @Override
    public String getDescription() {
        return "重建配置管理中自定义视图的视图";
    }

    @Override
    public List<ViewStatusInfo> execute() {
        List<ViewStatusInfo> resultList = new ArrayList<>();
        List<Long> idList = customViewMapper.getAllIdList();
        for (Long id : idList) {
            CustomViewVo customViewVo = customViewMapper.getCustomViewById(id);
            customViewService.parseConfig(customViewVo);
            ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
            viewStatusInfo.setViewName("customview_" + id);
            viewStatusInfo.setLabel(customViewVo.getName());
            try {
                CustomViewBuilder builder = new CustomViewBuilder(customViewVo);
                builder.buildView();
                viewStatusInfo.setStatus(ViewStatusInfo.Status.SUCCESS.toString());
            } catch (Exception e) {
                viewStatusInfo.setStatus(ViewStatusInfo.Status.FAILURE.toString());
                viewStatusInfo.setError(e.getMessage());
            }
            resultList.add(viewStatusInfo);
        }
        return resultList;
    }
}
