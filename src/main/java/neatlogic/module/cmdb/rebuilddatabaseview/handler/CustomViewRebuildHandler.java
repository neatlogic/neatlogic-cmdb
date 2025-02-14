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

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.rebuilddatabaseview.core.IRebuildDataBaseView;
import neatlogic.framework.rebuilddatabaseview.core.ViewStatusInfo;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.service.customview.CustomViewService;
import neatlogic.module.cmdb.utils.CustomViewBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class CustomViewRebuildHandler implements IRebuildDataBaseView {

    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private CustomViewService customViewService;

    @Resource
    private SchemaMapper schemaMapper;

    @Override
    public String getDescription() {
        return "重建配置管理中自定义视图的视图";
    }

    @Override
    public List<ViewStatusInfo> createViewIfNotExists() {
        List<ViewStatusInfo> resultList = new ArrayList<>();
        List<Long> idList = customViewMapper.getAllIdList();
        for (Long id : idList) {
            String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), "customview_" + id);
            if (Objects.equals(tableType, "VIEW")) {
                continue;
            }
            CustomViewVo customViewVo = customViewMapper.getCustomViewById(id);
            customViewService.parseConfig(customViewVo);
            ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
            viewStatusInfo.setName("customview_" + id);
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

    @Override
    public List<ViewStatusInfo> createOrReplaceView() {
        List<ViewStatusInfo> resultList = new ArrayList<>();
        List<Long> idList = customViewMapper.getAllIdList();
        for (Long id : idList) {
            CustomViewVo customViewVo = customViewMapper.getCustomViewById(id);
            customViewService.parseConfig(customViewVo);
            ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
            viewStatusInfo.setName("customview_" + id);
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

    @Override
    public int getSort() {
        return 3;
    }
}
