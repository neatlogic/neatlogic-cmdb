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
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.rebuilddatabaseview.core.IRebuildDataBaseView;
import neatlogic.framework.rebuilddatabaseview.core.ViewStatusInfo;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class CiViewRebuildHandler implements IRebuildDataBaseView {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiService ciService;

    @Resource
    private SchemaMapper schemaMapper;

    @Override
    public String getDescription() {
        return "重建配置项中虚拟模型视图";
    }

    @Override
    public List<ViewStatusInfo> createViewIfNotExists() {
        List<ViewStatusInfo> resultList = new ArrayList<>();
        int rowNum = ciMapper.getVirtualCiCount();
        if (rowNum > 0) {
            BasePageVo searchVo = new BasePageVo();
            searchVo.setRowNum(rowNum);
            searchVo.setPageSize(100);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<CiVo> ciList = ciMapper.getVirtualCiList(searchVo);
                for (CiVo ciVo : ciList) {
                    String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), "cmdb_" + ciVo.getId());
                    if (Objects.equals(tableType, "VIEW")) {
                        continue;
                    }
                    ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
                    viewStatusInfo.setName("cmdb_" + ciVo.getId());
                    viewStatusInfo.setLabel(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                    String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
                    ciVo.setViewXml(viewXml);
                    try {
                        ciService.buildCiView(ciVo);
                        viewStatusInfo.setStatus(ViewStatusInfo.Status.SUCCESS.toString());
                    } catch (Exception e) {
                        viewStatusInfo.setStatus(ViewStatusInfo.Status.FAILURE.toString());
                        viewStatusInfo.setError(e.getMessage());
                    }
                    resultList.add(viewStatusInfo);
                }
            }
        }
        return resultList;
    }

    @Override
    public List<ViewStatusInfo> createOrReplaceView() {
        List<ViewStatusInfo> resultList = Collections.synchronizedList(new ArrayList<>());
        int rowNum = ciMapper.getVirtualCiCount();
        if (rowNum > 0) {
            BasePageVo searchVo = new BasePageVo();
            searchVo.setRowNum(rowNum);
            searchVo.setPageSize(100);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<CiVo> ciList = ciMapper.getVirtualCiList(searchVo);
//                System.out.println("ciList.size() = " + ciList.size());
                BatchRunner<CiVo> runner = new BatchRunner<>();
                runner.execute(ciList, 5, (threadIndex, dataIndex, ciVo) -> {
//                    System.out.println("start cmdb_" + ciVo.getId());
                    long startTime = System.currentTimeMillis();
                    ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
                    viewStatusInfo.setName("cmdb_" + ciVo.getId());
                    viewStatusInfo.setLabel(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                    String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
                    ciVo.setViewXml(viewXml);
                    try {
                        ciService.buildCiView(ciVo);
                        viewStatusInfo.setStatus(ViewStatusInfo.Status.SUCCESS.toString());
                    } catch (Exception e) {
                        viewStatusInfo.setStatus(ViewStatusInfo.Status.FAILURE.toString());
                        viewStatusInfo.setError(e.getMessage());
                    }
                    viewStatusInfo.setTimeCost(System.currentTimeMillis() - startTime);
                    resultList.add(viewStatusInfo);
//                    System.out.println("end cmdb_" + ciVo.getId());
                }, "REBUILD-DATABASE-VIEW-FOR-CIVIEW");
//                for (CiVo ciVo : ciList) {
//                    ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
//                    viewStatusInfo.setName("cmdb_" + ciVo.getId());
//                    viewStatusInfo.setLabel(ciVo.getLabel() + "(" + ciVo.getName() + ")");
//                    String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
//                    ciVo.setViewXml(viewXml);
//                    try {
//                        ciService.buildCiView(ciVo);
//                        viewStatusInfo.setStatus(ViewStatusInfo.Status.SUCCESS.toString());
//                    } catch (Exception e) {
//                        viewStatusInfo.setStatus(ViewStatusInfo.Status.FAILURE.toString());
//                        viewStatusInfo.setError(e.getMessage());
//                    }
//                    resultList.add(viewStatusInfo);
//                }
            }
        }
        return resultList;
    }

    @Override
    public int getSort() {
        return 1;
    }
}
