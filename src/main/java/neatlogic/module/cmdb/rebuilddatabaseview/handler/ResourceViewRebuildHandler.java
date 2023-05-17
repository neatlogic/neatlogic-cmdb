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

import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.rebuilddatabaseview.core.IRebuildDataBaseView;
import neatlogic.framework.rebuilddatabaseview.core.ViewStatusInfo;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ResourceViewRebuildHandler implements IRebuildDataBaseView {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getDescription() {
        return "重建资源中心视图";
    }

    @Override
    public List<ViewStatusInfo> execute() {
        List<ViewStatusInfo> resultList = new ArrayList<>();
        List<ResourceEntityVo> resourceEntityList = resourceCenterResourceService.rebuildResourceEntity();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
            viewStatusInfo.setViewName(resourceEntityVo.getName());
            viewStatusInfo.setLabel(resourceEntityVo.getLabel());
            viewStatusInfo.setError(resourceEntityVo.getError());
            if (Objects.equals(resourceEntityVo.getStatus(), Status.ERROR.getValue())) {
                viewStatusInfo.setStatus(ViewStatusInfo.Status.FAILURE.toString());
            } else {
                viewStatusInfo.setStatus(ViewStatusInfo.Status.SUCCESS.toString());
            }
            resultList.add(viewStatusInfo);
        }
        return resultList;
    }
}
