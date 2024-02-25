package neatlogic.module.cmdb.workerdispatcher.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewAttrVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConditionFilterVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConditionVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.RoleMapper;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.RoleVo;
import neatlogic.framework.dto.TeamVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.form.service.IFormCrossoverService;
import neatlogic.framework.process.constvalue.ProcessUserType;
import neatlogic.framework.process.crossover.IProcessTaskCrossoverService;
import neatlogic.framework.process.dao.mapper.ProcessTaskMapper;
import neatlogic.framework.process.dao.mapper.SelectContentByHashMapper;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
import neatlogic.framework.process.dto.ProcessTaskFormVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskStepWorkerVo;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;
import neatlogic.framework.process.workerdispatcher.core.WorkerDispatcherBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.cmdb.service.customview.CustomViewDataService;
import neatlogic.module.cmdb.workerdispatcher.exception.CmdbDispatcherDispatchFailedException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CmdbDispatcher extends WorkerDispatcherBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private ProcessTaskMapper processTaskMapper;

    @Resource
    private SelectContentByHashMapper selectContentByHashMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CustomViewDataService customViewDataService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private RoleMapper roleMapper;

    @Override
    public String getName() {
        return "nmcwh.cmdbdispatcher.getname";
    }

    @Override
    public JSONArray getConfig() {
        return new JSONArray();
    }

    @Override
    public String getHelp() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("数据类型：单选；默认选择配置项");
        stringBuilder.append("\n");
        stringBuilder.append("数据来源：");
        stringBuilder.append("\n");
        stringBuilder.append("【数据类型】为配置项时，此处的选项为所有配置项的名称");
        stringBuilder.append("\n");
        stringBuilder.append("【数据类型】为视图时，此处的选项为所有CMDB自定义视图的名称");
        stringBuilder.append("\n");
        stringBuilder.append("匹配映射：");
        stringBuilder.append("\n");
        stringBuilder.append("配置模型或视图的属性，等于表单某个属性，过滤数据；");
        stringBuilder.append("\n");
        stringBuilder.append("支持多组条件的配置；");
        stringBuilder.append("\n");
        stringBuilder.append("多组之间的关系为AND关系");
        stringBuilder.append("\n");
        stringBuilder.append("处理人：");
        stringBuilder.append("\n");
        stringBuilder.append("从配置模型或视图中，选择一个或多个字段，作为处理人；");
        stringBuilder.append("\n");
        stringBuilder.append(" 必填，支持多选");
        stringBuilder.append("\n");
        stringBuilder.append("需要考虑分组、角色");
        stringBuilder.append("\n");
        stringBuilder.append("优先级：");
        stringBuilder.append("\n");
        stringBuilder.append(" 优先级中的选项，即【处理人】的选中项。可通过拖拽排序。比如下图，运维人员A这个字段为空时，则向下找运维人员B，依次类推");
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    @Override
    protected List<ProcessTaskStepWorkerVo> myGetWorker(ProcessTaskStepVo processTaskStepVo, JSONObject configObj) throws ProcessTaskException {
        List<ProcessTaskStepWorkerVo> workerList = new ArrayList<>();
        JSONArray filterList = configObj.getJSONArray("filterList");
        JSONArray priorityArray = configObj.getJSONArray("priorityList");
        String type = configObj.getString("type");
        if (Objects.equals(type, "ci")) {
            Long ciId = configObj.getLong("ciId");
            if (ciId == null) {
                throw new CmdbDispatcherDispatchFailedException(ciId);
            }
            CiVo ci = ciMapper.getCiById(ciId);
            if (ci == null) {
                throw new CmdbDispatcherDispatchFailedException(ciId);
            }
            List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
            Map<Long, AttrVo> attrMap = attrList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));

            CiEntityVo ciEntityConditionVo = new CiEntityVo();
            ciEntityConditionVo.setCiId(ciId);
            List<Long> priorityList = priorityArray.toJavaList(Long.class);
            ciEntityConditionVo.setAttrIdList(priorityList);
            ciEntityConditionVo.setRelIdList(new ArrayList<Long>() {{
                this.add(0L);
            }});
            if (CollectionUtils.isNotEmpty(filterList)) {
                Map<String, Object> formAttributeDataMap = formAttributeDataMap(processTaskStepVo.getProcessTaskId());
                for (int i = 0; i < filterList.size(); i++) {
                    JSONObject filterObj = filterList.getJSONObject(i);
                    Long key = filterObj.getLong("key");
                    if (key == null) {
                        continue;
                    }
                    if (!attrMap.containsKey(key)) {
                        continue;
                    }
                    String formAttributeUuid = filterObj.getString("formAttributeUuid");
                    if (StringUtils.isBlank(formAttributeUuid)) {
                        continue;
                    }
                    Object value = formAttributeDataMap.get(formAttributeUuid);
                    if (value == null) {
                        continue;
                    }
                    List<String> valueList = new ArrayList<>();
                    if (value instanceof List) {
                        List list = (List) value;
                        for (Object e : list) {
                            valueList.add(e.toString());
                        }
                    } else {
                        valueList.add(value.toString());
                    }
                    AttrFilterVo filterVo = new AttrFilterVo();
                    filterVo.setAttrId(key);
                    filterVo.setExpression(SearchExpression.EQ.getExpression());
                    filterVo.setValueList(valueList);
                    ciEntityConditionVo.addAttrFilter(filterVo);
                }
            }
            List<CiEntityVo> checkList = ciEntityService.searchCiEntity(ciEntityConditionVo);
            if (CollectionUtils.isEmpty(checkList)) {
                throw new CmdbDispatcherDispatchFailedException(ci, ciEntityConditionVo.getAttrFilterList());
            }
            if (checkList.size() > 1) {
                throw new CmdbDispatcherDispatchFailedException(ci, ciEntityConditionVo.getAttrFilterList(), checkList.size());
            }
            CiEntityVo ciEntity = checkList.get(0);
            for (Long attrId : priorityList) {
                AttrVo attrVo = attrMap.get(attrId);
                if (attrVo == null) {
                    continue;
                }
                AttrEntityVo attrEntity = ciEntity.getAttrEntityByAttrId(attrId);
                if (attrEntity == null) {
                    continue;
                }
                JSONArray valueList = attrEntity.getValueList();
                if (CollectionUtils.isEmpty(valueList)) {
                    continue;
                }
                workerList = getWorkerList(processTaskStepVo, attrVo, valueList);
                if (CollectionUtils.isNotEmpty(workerList)) {
                    return workerList;
                }
            }
            if (CollectionUtils.isEmpty(workerList)) {
                throw new CmdbDispatcherDispatchFailedException(ciEntity);
            }
        } else if (Objects.equals(type, "customView")) {
            Long customViewId = configObj.getLong("customViewId");
            if (customViewId == null) {
                throw new CmdbDispatcherDispatchFailedException(customViewId);
            }
            CustomViewVo customView = customViewMapper.getCustomViewById(customViewId);
            if (customView == null) {
                throw new CmdbDispatcherDispatchFailedException(customViewId);
            }
            CustomViewAttrVo customViewAttrVo = new CustomViewAttrVo(customViewId);
            List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByCustomViewId(customViewAttrVo);
            Map<String, CustomViewAttrVo> attrMap = attrList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
            CustomViewConditionVo customViewConditionVo = new CustomViewConditionVo();
            if (CollectionUtils.isNotEmpty(filterList)) {
                Map<String, Object> formAttributeDataMap = formAttributeDataMap(processTaskStepVo.getProcessTaskId());
                for (int i = 0; i < filterList.size(); i++) {
                    JSONObject filterObj = filterList.getJSONObject(i);
                    String key = filterObj.getString("key");
                    if (StringUtils.isBlank(key)) {
                        continue;
                    }
                    if (!attrMap.containsKey(key)) {
                        continue;
                    }
                    String formAttributeUuid = filterObj.getString("formAttributeUuid");
                    if (StringUtils.isBlank(formAttributeUuid)) {
                        continue;
                    }
                    Object value = formAttributeDataMap.get(formAttributeUuid);
                    if (value == null) {
                        continue;
                    }
                    JSONArray valueList = new JSONArray();
                    if (value instanceof List) {
                        valueList.addAll((List) value);
                    } else {
                        valueList.add(value);
                    }
                    CustomViewConditionFilterVo filterVo = new CustomViewConditionFilterVo();
                    filterVo.setAttrUuid(key);
                    filterVo.setType("attr");
                    filterVo.setExpression(SearchExpression.EQ.getExpression());
                    filterVo.setValueList(valueList);
                    customViewConditionVo.addAttrFilter(filterVo);
                }
            }
            customViewConditionVo.setCustomViewId(customViewId);
            List<Map<String, Object>> dataList = customViewDataService.searchCustomViewData(customViewConditionVo);
            if (CollectionUtils.isEmpty(dataList)) {
                throw new CmdbDispatcherDispatchFailedException(customView, customViewConditionVo.getAttrFilterList());
            }
            if (dataList.size() > 1) {
                throw new CmdbDispatcherDispatchFailedException(customView, customViewConditionVo.getAttrFilterList(), dataList.size());
            }
            Map<String, Object> data = dataList.get(0);
            List<String> priorityList = priorityArray.toJavaList(String.class);
            for (String attrUuid : priorityList) {
                CustomViewAttrVo customViewAttr = attrMap.get(attrUuid);
                if (customViewAttr == null) {
                    continue;
                }
                Object obj = data.get(attrUuid);
                if (obj == null) {
                    continue;
                }
                String str = obj.toString();
                if (StringUtils.isBlank(str)) {
                    continue;
                }
                String[] split = str.split(",");
                JSONArray valueList = new JSONArray();
                for (String value : split) {
                    valueList.add(value);
                }
                AttrVo attrVo = customViewAttr.getAttrVo();
                workerList = getWorkerList(processTaskStepVo, attrVo, valueList);
                if (CollectionUtils.isNotEmpty(workerList)) {
                    return workerList;
                }
            }
            if (CollectionUtils.isEmpty(workerList)) {
                throw new CmdbDispatcherDispatchFailedException(data);
            }
        }
        return workerList;
    }

    private List<ProcessTaskStepWorkerVo> getWorkerList(ProcessTaskStepVo processTaskStepVo, AttrVo attrVo, JSONArray valueList) {
        List<ProcessTaskStepWorkerVo> workerList = new ArrayList<>();
        if (attrVo.getTargetCiId() != null) {
            List<Long> idList = new ArrayList<>();
            List<Long> longValueList = new ArrayList<>();
            List<String> stringValueList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                Object valueObj = valueList.get(i);
                if (valueObj instanceof Long) {
                    longValueList.add((Long) valueObj);
                } else if (valueObj instanceof String) {
                    String valueStr = valueObj.toString();
                    try {
                        Integer.valueOf(valueStr);
                        stringValueList.add(valueStr);
                    } catch (NumberFormatException e1) {
                        try {
                            longValueList.add(Long.valueOf(valueStr));
                        } catch (NumberFormatException e2) {
                            stringValueList.add(valueStr);
                        }
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(longValueList)) {
                idList.addAll(longValueList);
            }
            if (CollectionUtils.isNotEmpty(stringValueList)) {
                for (String name : stringValueList) {
                    List<CiEntityVo> ciEntityList = ciEntityService.getCiEntityBaseInfoByName(attrVo.getTargetCiId(), name);
                    idList.addAll(ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList()));
                }
            }
            List<UserVo> userList = userMapper.getUserByIdList(idList);
            if (CollectionUtils.isNotEmpty(userList)) {
                for (UserVo user : userList) {
                    ProcessTaskStepWorkerVo worker = new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId(), GroupSearch.USER.getValue(), user.getUuid(), ProcessUserType.MAJOR.getValue());
                    workerList.add(worker);
                }
            } else {
                List<TeamVo> teamList = teamMapper.getTeamByIdList(idList);
                if (CollectionUtils.isNotEmpty(teamList)) {
                    for (TeamVo team : teamList) {
                        ProcessTaskStepWorkerVo worker = new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId(), GroupSearch.TEAM.getValue(), team.getUuid(), ProcessUserType.MAJOR.getValue());
                        workerList.add(worker);
                    }
                } else {
                    List<RoleVo> roleList = roleMapper.getRoleByIdList(idList);
                    if (CollectionUtils.isNotEmpty(roleList)) {
                        for (RoleVo role : roleList) {
                            ProcessTaskStepWorkerVo worker = new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId(), GroupSearch.ROLE.getValue(), role.getUuid(), ProcessUserType.MAJOR.getValue());
                            workerList.add(worker);
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < valueList.size(); i++) {
                String name = valueList.getString(i);
                UserVo userVo = userMapper.getUserByUserId(name);
                if (userVo != null) {
                    ProcessTaskStepWorkerVo worker = new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId(), GroupSearch.USER.getValue(), userVo.getUuid(), ProcessUserType.MAJOR.getValue());
                    workerList.add(worker);
                } else {
                    List<String> userUuidList = userMapper.getUserUuidListByUserName(name);
                    if (CollectionUtils.isNotEmpty(userUuidList)) {
//                        List<UserVo> userList = userMapper.getUserByUserUuidList(userUuidList);
                        for (String userUuid : userUuidList) {
                            ProcessTaskStepWorkerVo worker = new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId(), GroupSearch.USER.getValue(), userUuid, ProcessUserType.MAJOR.getValue());
                            workerList.add(worker);
                        }
                    } else {
                        List<String> teamUuidList = teamMapper.getTeamUuidByName(name);
                        if (CollectionUtils.isNotEmpty(teamUuidList)) {
//                            List<TeamVo> teamList = teamMapper.getTeamByUuidList(teamUuidList);
                            for (String teamUuid : teamUuidList) {
                                ProcessTaskStepWorkerVo worker = new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId(), GroupSearch.TEAM.getValue(), teamUuid, ProcessUserType.MAJOR.getValue());
                                workerList.add(worker);
                            }
                        } else {
                            List<String> roleUuidList = roleMapper.getRoleUuidByName(name);
                            if (CollectionUtils.isNotEmpty(roleUuidList)) {
//                                List<RoleVo> roleList = roleMapper.getRoleByUuidList(roleUuidList);
                                for (String roleUuid : roleUuidList) {
                                    ProcessTaskStepWorkerVo worker = new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId(), GroupSearch.ROLE.getValue(), roleUuid, ProcessUserType.MAJOR.getValue());
                                    workerList.add(worker);
                                }
                            }
                        }
                    }
                }
            }
        }
        return workerList;
    }

    private Map<String, Object> formAttributeDataMap(Long processTaskId) {
        Map<String, Object> formAttributeDataMap = new HashMap<>();
        String formConfig = null;
        // 如果工单有表单信息，则查询出表单配置及数据
        ProcessTaskFormVo processTaskFormVo = processTaskMapper.getProcessTaskFormByProcessTaskId(processTaskId);
        if (processTaskFormVo != null) {
            formConfig = selectContentByHashMapper.getProcessTaskFromContentByHash(processTaskFormVo.getFormContentHash());
            IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskId(processTaskId);
            if (CollectionUtils.isNotEmpty(processTaskFormAttributeDataList)) {
                for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataList) {
                    Object dataObj = formAttributeDataAdaptsToCmdb(attributeDataVo.getAttributeUuid(), attributeDataVo.getDataObj(), formConfig);
                    if (dataObj == null) {
                        continue;
                    }
                    formAttributeDataMap.put(attributeDataVo.getAttributeUuid(), dataObj);
                }
            }
        }
        return formAttributeDataMap;
    }

    /**
     * 表单数据适配CMDB数据
     * @param attributeUuid 表单属性uuid
     * @param originalValue 表单属性值
     * @param formConfig 表单配置信息
     * @return
     */
    private Object formAttributeDataAdaptsToCmdb(String attributeUuid, Object originalValue, String formConfig) {
        if (originalValue == null) {
            return null;
        }
        IFormCrossoverService formCrossoverService = CrossoverServiceFactory.getApi(IFormCrossoverService.class);
        String handler = formCrossoverService.getFormAttributeHandler(attributeUuid, formConfig);
        if (Objects.equals(handler, FormHandler.FORMUPLOAD.getHandler())) {
            JSONArray resultList = new JSONArray();
            if (originalValue instanceof JSONArray) {
                JSONArray dataArray = (JSONArray) originalValue;
                for (int m = 0; m < dataArray.size(); m++) {
                    JSONObject data = dataArray.getJSONObject(m);
                    Long id = data.getLong("id");
                    if (id != null) {
                        resultList.add(id);
                    }
                }
            }
            return resultList;
        } else if (Objects.equals(handler, FormHandler.FORMRADIO.getHandler())
                || Objects.equals(handler, FormHandler.FORMCHECKBOX.getHandler())
                || Objects.equals(handler, FormHandler.FORMSELECT.getHandler())) {
            return formCrossoverService.getFormSelectAttributeValueByOriginalValue(originalValue);
        }
        return originalValue;
    }
}
