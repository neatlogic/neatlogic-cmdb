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

package neatlogic.module.cmdb.rebuilddatabaseview.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.batch.BatchRunner;
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
import java.util.Collections;
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
        List<ViewStatusInfo> resultList = Collections.synchronizedList(new ArrayList<>());
        List<Long> idList = customViewMapper.getAllIdList();
        BatchRunner<Long> runner = new BatchRunner<>();
        runner.execute(idList, 5, (threadIndex, dataIndex, id) -> {
            long startTime = System.currentTimeMillis();
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
            viewStatusInfo.setTimeCost(System.currentTimeMillis() - startTime);
            resultList.add(viewStatusInfo);
        }, "REBUILD-DATABASE-VIEW-FOR-CUSTOMVIEW");
//        for (Long id : idList) {
//            CustomViewVo customViewVo = customViewMapper.getCustomViewById(id);
//            customViewService.parseConfig(customViewVo);
//            ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
//            viewStatusInfo.setName("customview_" + id);
//            viewStatusInfo.setLabel(customViewVo.getName());
//            try {
//                CustomViewBuilder builder = new CustomViewBuilder(customViewVo);
//                builder.buildView();
//                viewStatusInfo.setStatus(ViewStatusInfo.Status.SUCCESS.toString());
//            } catch (Exception e) {
//                viewStatusInfo.setStatus(ViewStatusInfo.Status.FAILURE.toString());
//                viewStatusInfo.setError(e.getMessage());
//            }
//            resultList.add(viewStatusInfo);
//        }
        return resultList;
    }

    @Override
    public int getSort() {
        return 3;
    }
}
