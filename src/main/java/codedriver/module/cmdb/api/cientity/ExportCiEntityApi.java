/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.ExcelBuilder;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.module.cmdb.utils.RelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportCiEntityApi extends PrivateBinaryStreamApiComponentBase {
    private final static Logger logger = LoggerFactory.getLogger(ExportCiEntityApi.class);
    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/export";
    }

    @Override
    public String getName() {
        return "导出配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键字"),
            @Param(name = "attrFilterList", type = ApiParamType.STRING, desc = "属性过滤条件"),
            @Param(name = "relFilterList", type = ApiParamType.JSONARRAY, desc = "关系过滤条件")
    })
    @Description(desc = "导出配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CiEntityVo ciEntityVo = JSONObject.toJavaObject(jsonObj, CiEntityVo.class);
        Long ciId = jsonObj.getLong("ciId");
        CiVo ciVo = ciMapper.getCiById(ciId);
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciEntityVo.getCiId());
        List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
        List<Long> attrIdList = null, relIdList = null;
        List<String> headerList = new ArrayList<>();
        headerList.add("id");
        headerList.add("uuid");
        List<String> columnList = new ArrayList<>();
        columnList.add("id");
        columnList.add("uuid");
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            attrIdList = new ArrayList<>();
            relIdList = new ArrayList<>();
            for (CiViewVo ciview : ciViewList) {
                switch (ciview.getType()) {
                    case "attr":
                        attrIdList.add(ciview.getItemId());
                        columnList.add("attr_" + ciview.getItemId());
                        headerList.add(ciview.getItemLabel());
                        break;
                    case "relfrom":
                        relIdList.add(ciview.getItemId());
                        columnList.add("relfrom_" + ciview.getItemId());
                        headerList.add(ciview.getItemLabel());
                        break;
                    case "relto":
                        relIdList.add(ciview.getItemId());
                        columnList.add("relto_" + ciview.getItemId());
                        headerList.add(ciview.getItemLabel());
                        break;
                }
            }
        }

        ciEntityVo.setAttrIdList(attrIdList);
        ciEntityVo.setRelIdList(relIdList);


        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        List<Map<String, Object>> dataList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciEntityList)) {
            for (CiEntityVo entity : ciEntityList) {
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("id", entity.getId());
                dataMap.put("uuid", entity.getUuid());
                dataMap.put("name", entity.getName());
                dataMap.put("ciId", entity.getCiId());
                dataMap.put("ciName", entity.getCiName());
                dataMap.put("ciLabel", entity.getCiLabel());
                dataMap.put("type", entity.getTypeId());
                dataMap.put("typeName", entity.getTypeName());
                for (String column : columnList) {
                    if (entity.getAttrEntityData().containsKey(column)) {
                        JSONObject attrObj = entity.getAttrEntityData().getJSONObject(column);
                        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrObj.getString("type"));
                        AttrVo attrVo = new AttrVo();
                        attrVo.setConfig(attrObj.getJSONObject("config"));
                        attrVo.setTargetCiId(attrObj.getLong("targetCiId"));
                        if (handler != null) {
                            JSONArray valueList = attrObj.getJSONArray("valueList");
                            handler.transferValueListToExport(attrVo, valueList);
                            if (CollectionUtils.isNotEmpty(valueList)) {
                                dataMap.put(column, valueList.stream().map(Object::toString).collect(Collectors.joining(",")));
                            }
                        }
                    } else if (entity.getRelEntityData().containsKey(column)) {
                        JSONArray valueList = entity.getRelEntityData().getJSONObject(column).getJSONArray("valueList");
                        if (CollectionUtils.isNotEmpty(valueList)) {
                            String relCiEntityName = "";
                            for (int i = 0; i < valueList.size(); i++) {
                                if (StringUtils.isNotBlank(relCiEntityName)) {
                                    relCiEntityName += ",";
                                }
                                relCiEntityName += valueList.getJSONObject(i).getString("ciEntityName");
                            }
                            dataMap.put(column, relCiEntityName);
                        }
                    }
                }
                dataList.add(dataMap);
            }
        }
        ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
        builder.withSheetName("数据")
                .withHeaderList(headerList)
                .withColumnList(columnList)
                .withDataList(dataList)
                .withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                .withColumnWidth(30);
        Workbook workbook = builder.build();
        String fileNameEncode = ciVo.getId() + "_" + ciVo.getLabel() + ".xlsx";
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
