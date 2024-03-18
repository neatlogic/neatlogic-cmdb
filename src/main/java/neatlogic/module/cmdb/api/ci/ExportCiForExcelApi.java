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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.excel.ExcelBuilder;
import neatlogic.framework.util.excel.SheetBuilder;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
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

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/export/forexcel";
    }

    @Override
    public String getName() {
        return "nmcac.exportciforexcelapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "term.cmdb.ciidlist")
    })
    @Description(desc = "nmcac.exportciforexcelapi.getname")
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
            List<RelVo> relList = relMapper.getRelByCiId(ciVo.getId());
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
            for (RelVo relVo : relList) {
                Map<String, Object> dataMap = resourceConvertDataMap(relVo);
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
        } finally {
            workbook.close();
        }
        return null;
    }

    /**
     * 表头信息
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

    private Map<String, Object> resourceConvertDataMap(RelVo relVo) {
        Map<String, Object> dataMap = new HashMap<>();
        String name, label, ciLabel, isRequired, isUnique;
        if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
            name = relVo.getToName();
            label = relVo.getToLabel();
            ciLabel = relVo.getToCiLabel();
            isRequired = Objects.equals(relVo.getToIsRequired(), 1) ? "是" : "否";
            isUnique = Objects.equals(relVo.getToIsUnique(), 1) ? "是" : "否";
        } else {
            name = relVo.getFromName();
            label = relVo.getFromLabel();
            ciLabel = relVo.getFromCiLabel();
            isRequired = Objects.equals(relVo.getFromIsRequired(), 1) ? "是" : "否";
            isUnique = Objects.equals(relVo.getFromIsUnique(), 1) ? "是" : "否";
        }
        dataMap.put("name", name);
        dataMap.put("label", label);
        dataMap.put("typeText", "关系");
        dataMap.put("ciLabel", ciLabel);
        dataMap.put("isRequired", isRequired);
        dataMap.put("isUnique", isUnique);
        dataMap.put("inputType", Objects.equals(relVo.getInputType(), "at") ? "是" : "否");
        return dataMap;
    }
}
