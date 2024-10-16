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

package neatlogic.module.cmdb.api.customview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.customview.*;
import neatlogic.framework.cmdb.exception.customview.CustomViewNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
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
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportCustomViewDataApi extends PrivateBinaryStreamApiComponentBase {
    private final static Logger logger = LoggerFactory.getLogger(ExportCustomViewDataApi.class);
    @Resource
    private CustomViewDataService customViewDataService;

    @Resource
    private CustomViewMapper customViewMapper;


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
    @Description(desc = "查询自定义视图数据")
    //TODO 后续要对数据进行优化防止OOM
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CustomViewConditionVo customViewConditionVo = JSON.toJavaObject(paramObj, CustomViewConditionVo.class);
        Long customViewId = paramObj.getLong("id");
        CustomViewVo customViewVo = customViewMapper.getCustomViewById(customViewId);
        if (customViewVo == null) {
            throw new CustomViewNotFoundException(customViewId);
        }
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
        if(CollectionUtils.isNotEmpty(globalAttrList)) {
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
        customViewConditionVo.setPageSize(100);
        customViewConditionVo.setCurrentPage(1);
        List<Map<String, Object>> dataList = customViewDataService.searchCustomViewData(customViewConditionVo);
        while (CollectionUtils.isNotEmpty(dataList)) {
            //由于展示页面的特殊性，查询sql用的是pageSizePlus，所以要去掉租后一条数据
            for (int i = 0; i < Math.min(customViewConditionVo.getPageSize(), dataList.size()); i++) {
                sheetBuilder.addData(dataList.get(i));
            }
            customViewConditionVo.setCurrentPage(customViewConditionVo.getCurrentPage() + 1);
            dataList = customViewDataService.searchCustomViewData(customViewConditionVo);
        }

        String fileNameEncode = customViewVo.getName() + ".xlsx";
        boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
            fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
        } else {
            fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
        }
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");
        try (OutputStream os = response.getOutputStream();) {
            workbook.write(os);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

}
