/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
            viewStatusInfo.setName(resourceEntityVo.getName());
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

    @Override
    public int getSort() {
        return 4;
    }
}
