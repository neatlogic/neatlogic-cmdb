/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import codedriver.framework.util.excel.SheetBuilder;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportCiForExcelApi extends PrivateBinaryStreamApiComponentBase {
    private final static Logger logger = LoggerFactory.getLogger(ExportCiForExcelApi.class);

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/export/forexcel";
    }

    @Override
    public String getName() {
        return "导出配置项模型及属性列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "模型ID列表")
    })
    @Description(desc = "导出配置项模型及属性列表")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONArray idArray = jsonObj.getJSONArray("idList");
        List<Long> idList = idArray.toJavaList(Long.class);
        List<CiVo> ciList = ciMapper.getCiByIdList(idList);
        if (CollectionUtils.isEmpty(ciList)) {
            return null;
        }
        ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
        List<String> headerList = getHeaderList();
        List<String> columnList = getColumnList();
        for (CiVo ciVo : ciList) {
            List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
            SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                    .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                    .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                    .withColumnWidth(30)
                    .addSheet(ciVo.getLabel() + "(" + ciVo.getName() + ")")
                    .withHeaderList(headerList)
                    .withColumnList(columnList);
            for (AttrVo attrVo : attrList) {
                Map<String, Object> dataMap = resourceConvertDataMap(attrVo);
                sheetBuilder.addData(dataMap);
            }
        }

        Workbook workbook = builder.build();
        String fileName = FileUtil.getEncodedFileName("配置模型属性_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx");
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 表头信息
     *
     * @return
     */
    private List<String> getHeaderList() {
        List<String> headerList = new ArrayList<>();
        headerList.add("唯一标识");
        headerList.add("名称");
        headerList.add("类型");
        headerList.add("继承自");
        headerList.add("是否必填");
        headerList.add("是否唯一");
        headerList.add("自动采集");
        return headerList;
    }

    /**
     * 每列对应的key
     *
     * @return
     */
    private List<String> getColumnList() {
        List<String> columnList = new ArrayList<>();
        columnList.add("name");
        columnList.add("label");
        columnList.add("typeText");
        columnList.add("ciLabel");
        columnList.add("isRequired");
        columnList.add("isUnique");
        columnList.add("inputType");
        return columnList;
    }

    /**
     * 属性对象转换成excel中一行数据dataMap
     *
     * @param attrVo 属性对象
     */
    private Map<String, Object> resourceConvertDataMap(AttrVo attrVo) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("name", attrVo.getName());
        dataMap.put("label", attrVo.getLabel());
        dataMap.put("typeText", attrVo.getTypeText());
        dataMap.put("ciLabel", attrVo.getCiLabel());
        dataMap.put("isRequired", Objects.equals(attrVo.getIsRequired(), 1) ? "是" : "否");
        dataMap.put("isUnique", Objects.equals(attrVo.getIsUnique(), 1) ? "是" : "否");
        dataMap.put("inputType", Objects.equals(attrVo.getInputType(), "at") ? "是" : "否");
        return dataMap;
    }
}