/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.InspectStatus;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.TimeUtil;
import neatlogic.framework.util.excel.ExcelBuilder;
import neatlogic.framework.util.excel.SheetBuilder;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportResourceApi extends PrivateBinaryStreamApiComponentBase {

    private static Logger logger = LoggerFactory.getLogger(ExportResourceApi.class);

    @Resource
    private ResourceMapper resourceMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getToken() {
        return "resourcecenter/resource/export";
    }

    @Override
    public String getName() {
        return "导出资产清单列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true,  desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "vendorIdList", type = ApiParamType.JSONARRAY, desc = "厂商id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表")
    })
    @Description(desc = "导出资产清单列表")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ResourceSearchVo searchVo = JSONObject.toJavaObject(paramObj, ResourceSearchVo.class);
        Long typeId = searchVo.getTypeId();
        CiVo ciVo = ciMapper.getCiById(typeId);
        if (ciVo == null) {
            throw new CiNotFoundException(typeId);
        }
        String fileNameEncode = ciVo.getId() + "_" + ciVo.getLabel() + ".xlsx";
        boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
            fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
        } else {
            fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
        }
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");

        ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
        SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                .withColumnWidth(30)
                .addSheet("数据")
                .withHeaderList(getHeaderList())
                .withColumnList(getColumnList());
        Workbook workbook = builder.build();
        List<ResourceVo> resourceList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            resourceList = resourceMapper.getResourceListByIdList(idList);
            resourceCenterResourceService.addTagAndAccountInformation(resourceList);
            for (ResourceVo resourceVo : resourceList) {
                Map<String, Object> dataMap = resourceConvertDataMap(resourceVo);
                sheetBuilder.addData(dataMap);
            }
        } else {
            List<Long> typeIdList = resourceCenterResourceService.getDownwardCiIdListByCiIdList(Arrays.asList(typeId));
            searchVo.setTypeIdList(typeIdList);
            int rowNum = resourceMapper.getResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setPageSize(100);
                searchVo.setRowNum(rowNum);
                for (int i = 1; i <= searchVo.getPageCount(); i++) {
                    searchVo.setCurrentPage(i);
                    List<Long> idList = resourceMapper.getResourceIdList(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        resourceList = resourceMapper.getResourceListByIdList(idList);
                        resourceCenterResourceService.addTagAndAccountInformation(resourceList);
                        for (ResourceVo resourceVo : resourceList) {
                            Map<String, Object> dataMap = resourceConvertDataMap(resourceVo);
                            sheetBuilder.addData(dataMap);
                        }
                    }
                }
            }
        }
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 表头信息
     * @return
     */
    private List<String> getHeaderList() {
        List<String> headerList = new ArrayList<>();
        headerList.add("资产id");
        headerList.add("IP地址");
        headerList.add("类型");
        headerList.add("名称");
        headerList.add("监控状态");
        headerList.add("巡检状态");
        headerList.add("模块");
        headerList.add("应用");
        headerList.add("IP列表");
        headerList.add("所属部门");
        headerList.add("所有者");
        headerList.add("资产状态");
        headerList.add("网络区域");
        headerList.add("标签");
        headerList.add("维护窗口");
        headerList.add("帐号");
        headerList.add("描述");
        return headerList;
    }

    /**
     * 每列对应的key
     * @return
     */
    private List<String> getColumnList() {
        List<String> columnList = new ArrayList<>();
        columnList.add("resourceId");
        columnList.add("ip:port");
        columnList.add("typeLabel");
        columnList.add("name");
        columnList.add("monitorStatus");
        columnList.add("inspectStatus");
        columnList.add("appModuleName");
        columnList.add("appSystemName");
        columnList.add("allIpList");
        columnList.add("bgList");
        columnList.add("ownerList");
        columnList.add("stateName");
        columnList.add("networkArea");
        columnList.add("tagList");
        columnList.add("maintenanceWindow");
        columnList.add("accountList");
        columnList.add("description");
        return columnList;
    }

    /**
     * 资产对象转换成excel中一行数据dataMap
     * @param resourceVo 资产对象
     */
    private Map<String, Object> resourceConvertDataMap(ResourceVo resourceVo) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("ip:port", resourceVo.getIp() + (resourceVo.getPort() != null ? ":" + resourceVo.getPort() : StringUtils.EMPTY));
        dataMap.put("typeLabel", resourceVo.getTypeLabel());
        dataMap.put("resourceId", resourceVo.getId());
        dataMap.put("name", resourceVo.getName());
        dataMap.put("description", resourceVo.getDescription());
        dataMap.put("monitorStatus", resourceVo.getMonitorStatus());
        dataMap.put("inspectStatus", StringUtils.isNotBlank(resourceVo.getInspectStatus()) ? InspectStatus.getText(resourceVo.getInspectStatus()) + " "
                + (resourceVo.getInspectTime() != null ? TimeUtil.convertDateToString(resourceVo.getInspectTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS)
                : StringUtils.EMPTY) : StringUtils.EMPTY);
        dataMap.put("allIpList", CollectionUtils.isNotEmpty(resourceVo.getAllIp()) ? resourceVo.getAllIp().stream().map(IpVo::getIp).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("bgList", CollectionUtils.isNotEmpty(resourceVo.getBgList()) ? resourceVo.getBgList().stream().map(BgVo::getBgName).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("ownerList", CollectionUtils.isNotEmpty(resourceVo.getOwnerList()) ? resourceVo.getOwnerList().stream().map(OwnerVo::getUserName).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("stateName", resourceVo.getStateName());
        dataMap.put("networkArea", resourceVo.getNetworkArea());
        if (CollectionUtils.isNotEmpty(resourceVo.getTagList())) {
            dataMap.put("tagList", String.join("、", resourceVo.getTagList()));
        } else {
            dataMap.put("tagList", StringUtils.EMPTY);
        }
        dataMap.put("maintenanceWindow", resourceVo.getMaintenanceWindow());
        if (StringUtils.isNotBlank(resourceVo.getAppModuleName()) && StringUtils.isNotBlank(resourceVo.getAppModuleAbbrName())) {
            dataMap.put("appModuleName", resourceVo.getAppModuleName() + "(" + resourceVo.getAppModuleAbbrName() + ")");
        } else if (StringUtils.isNotBlank(resourceVo.getAppModuleName())) {
            dataMap.put("appModuleName", resourceVo.getAppModuleName());
        } else if (StringUtils.isNotBlank(resourceVo.getAppModuleAbbrName())) {
            dataMap.put("appModuleName", resourceVo.getAppModuleAbbrName());
        } else {
            dataMap.put("appModuleName", StringUtils.EMPTY);
        }

        if (StringUtils.isNotBlank(resourceVo.getAppSystemName()) && StringUtils.isNotBlank(resourceVo.getAppSystemAbbrName())) {
            dataMap.put("appSystemName", resourceVo.getAppSystemName() + "(" + resourceVo.getAppSystemAbbrName() + ")");
        } else if (StringUtils.isNotBlank(resourceVo.getAppSystemName())) {
            dataMap.put("appSystemName", resourceVo.getAppSystemName());
        } else if (StringUtils.isNotBlank(resourceVo.getAppSystemAbbrName())) {
            dataMap.put("appSystemName", resourceVo.getAppSystemAbbrName());
        } else {
            dataMap.put("appSystemName", StringUtils.EMPTY);
        }
        List<AccountVo> accountList = resourceVo.getAccountList();
        if (CollectionUtils.isNotEmpty(accountList)) {
            List<String> list = new ArrayList<>();
            for (AccountVo accountVo : accountList) {
                list.add(accountVo.getName() + "(" + accountVo.getAccount() + "/" + accountVo.getProtocol() + ")");
            }
            dataMap.put("accountList", String.join("、", list));
        } else {
            dataMap.put("accountList", StringUtils.EMPTY);
        }
        return dataMap;
    }
}
