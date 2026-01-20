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

package neatlogic.module.cmdb.api.customview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.customview.*;
import neatlogic.framework.cmdb.enums.CmdbUserExportFileType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityIsExportingException;
import neatlogic.framework.cmdb.exception.customview.CustomViewNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserExportFileMapper;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.userexportfile.dto.UserExportFileVo;
import neatlogic.framework.util.UserExportFileUtil;
import neatlogic.framework.util.excel.ExcelBuilder;
import neatlogic.framework.util.excel.SheetBuilder;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.service.customview.CustomViewDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportCustomViewDataApi extends PrivateBinaryStreamApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(ExportCustomViewDataApi.class);
    private final ReentrantLock exportLock = new ReentrantLock();
    @Resource
    private CustomViewDataService customViewDataService;

    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private UserExportFileMapper userExportFileMapper;


    @Override
    public String getToken() {
        return "/cmdb/customview/data/export";
    }

    @Override
    public String getName() {
        return "导出自定义视图数据";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "视图id", isRequired = true),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "attrFilterList", type = ApiParamType.JSONARRAY, desc = "高级搜索条件")
    })
    @Output({@Param(name = "dataList", type = ApiParamType.JSONARRAY, desc = "结果集"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小")})
    @Description(desc = "导出自定义视图数据")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        //最大导出数量
        final int MAX_COUNT = 100000;
        try {
            if (!exportLock.tryLock()) {
                throw new CiEntityIsExportingException();
            }
            CustomViewConditionVo customViewConditionVo = JSON.toJavaObject(paramObj, CustomViewConditionVo.class);
            Long customViewId = paramObj.getLong("id");
            CustomViewVo customViewVo = customViewMapper.getCustomViewById(customViewId);
            if (customViewVo == null) {
                throw new CustomViewNotFoundException(customViewId);
            }
            UserExportFileVo userExportFileVo = new UserExportFileVo(CmdbUserExportFileType.CUSTOMVIEW_DATA, customViewVo.getName(), ".xlsx", "application/vnd.ms-excel;charset=utf-8");
            userExportFileMapper.insertUserExportFile(userExportFileVo);
            CustomViewAttrVo pCustomViewAttrVo = new CustomViewAttrVo();
            pCustomViewAttrVo.setCustomViewId(customViewId);
            pCustomViewAttrVo.setIsHidden(0);
            CustomViewConstAttrVo pCustomViewConstAttrVo = new CustomViewConstAttrVo();
            pCustomViewConstAttrVo.setCustomViewId(customViewId);
            pCustomViewConstAttrVo.setIsHidden(0);
            CustomViewGlobalAttrVo pCustomViewGlobalAttrVo = new CustomViewGlobalAttrVo();
            pCustomViewGlobalAttrVo.setCustomViewId(customViewId);
            pCustomViewGlobalAttrVo.setIsHidden(0);
            List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByCustomViewId(pCustomViewAttrVo);
            List<CustomViewConstAttrVo> constAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(pCustomViewConstAttrVo);
            List<CustomViewGlobalAttrVo> globalAttrList = customViewMapper.getCustomViewGlobalAttrByCustomViewId(pCustomViewGlobalAttrVo);
            List<JSONObject> attrsList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(attrList)) {
                for (CustomViewAttrVo attrVo : attrList) {
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("alias", attrVo.getAlias());
                    dataObj.put("uuid", attrVo.getUuid());
                    dataObj.put("sort", attrVo.getSort());
                    attrsList.add(dataObj);
                }
            }
            if (CollectionUtils.isNotEmpty(constAttrList)) {
                for (CustomViewConstAttrVo attrVo : constAttrList) {
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("alias", attrVo.getAlias());
                    dataObj.put("uuid", attrVo.getUuid());
                    dataObj.put("sort", attrVo.getSort());
                    attrsList.add(dataObj);
                }
            }
            if (CollectionUtils.isNotEmpty(globalAttrList)) {
                for (CustomViewGlobalAttrVo attrVo : globalAttrList) {
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("alias", attrVo.getAlias());
                    dataObj.put("uuid", attrVo.getUuid());
                    dataObj.put("sort", attrVo.getSort());
                    attrsList.add(dataObj);
                }
            }
            attrsList.sort(Comparator.comparing(o -> o.getInteger("sort")));
            List<String> headerList = new ArrayList<>();
            List<String> columnList = new ArrayList<>();
            for (JSONObject attr : attrsList) {
                headerList.add(attr.getString("alias"));
                columnList.add(attr.getString("uuid"));
            }

            ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
            SheetBuilder sheetBuilder = builder
                    .withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                    .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                    .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                    .withColumnWidth(30).addSheet("数据")
                    .withHeaderList(headerList)
                    .withColumnList(columnList);
            Workbook workbook = builder.build();

            customViewConditionVo.setCustomViewId(customViewId);
            customViewConditionVo.setPageSize(1000);
            customViewConditionVo.setCurrentPage(1);
            List<Map<String, Object>> dataList = customViewDataService.searchCustomViewData(customViewConditionVo);
            int k = 0;
            while (CollectionUtils.isNotEmpty(dataList)) {
                //由于展示页面的特殊性，查询sql用的是pageSizePlus，所以要去掉最后一条数据
                for (int i = 0; i < Math.min(customViewConditionVo.getPageSize(), dataList.size()); i++) {
                    k += 1;
                    sheetBuilder.addData(dataList.get(i));
                    if (k >= MAX_COUNT) {
                        break;
                    }
                }
                customViewConditionVo.setCurrentPage(customViewConditionVo.getCurrentPage() + 1);
                dataList = customViewDataService.searchCustomViewData(customViewConditionVo);
            }
            UserExportFileUtil.saveWorkbook(workbook, userExportFileVo, response);
            return null;
        } finally {
            if (exportLock.isLocked() && exportLock.isHeldByCurrentThread()) {
                exportLock.unlock();
            }
        }
    }

}
