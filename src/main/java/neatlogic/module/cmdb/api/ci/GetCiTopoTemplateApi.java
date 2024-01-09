/*
 * Copyright(c) 2024 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiTopoTemplateVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTopoTemplateMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiTopoTemplateApi extends PrivateApiComponentBase {

    @Resource
    private CiTopoTemplateMapper ciTopoTemplateMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/topotemplate/get";
    }

    @Override
    public String getName() {
        return "nmcac.getcitopotemplateapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true),
            @Param(name = "needRelPath", desc = "term.cmdb.needcirel", type = ApiParamType.INTEGER)
    })
    @Output({@Param(explode = CiTopoTemplateVo.class)})
    @Description(desc = "nmcac.getcitopotemplateapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTopoTemplateVo ciTopoTemplateVo = ciTopoTemplateMapper.getCiTopoTemplateById(jsonObj.getLong("id"));
        Integer needRelPath = jsonObj.getInteger("needRelPath");
        if (needRelPath != null && needRelPath.equals(1)) {
            //以下逻辑是为了可以正确回显已经选择的关系路径
            JSONArray ciRelList = ciTopoTemplateVo.getConfig().getJSONArray("ciRelList");
            Long ciId = ciTopoTemplateVo.getCiId();
            if (ciId != null) {
                JSONObject root = new JSONObject();
                generateData(root, 0, ciRelList, ciId);
                if (MapUtils.isNotEmpty(root)) {
                    ciTopoTemplateVo.setRelPath(root.getJSONArray("children"));
                }
            }
        }
        return ciTopoTemplateVo;
    }

    private void generateData(JSONObject parentObj, int level, JSONArray ciRelList, Long ciId) {
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        JSONArray relObjList = new JSONArray();
        for (RelVo relVo : relList) {
            JSONObject relObj = JSONObject.parseObject(JSONObject.toJSONString(relVo));
            relObj.put("children", new JSONArray());
            relObj.put("loading", false);
            relObj.put("selected", false);
            relObj.put("excludeCiIdList", parentObj.get("excludeCiIdList") != null ? JSONArray.parseArray(parentObj.getJSONArray("excludeCiIdList").toString()) : new JSONArray());
            relObj.put("path", parentObj.get("path") != null ? JSONArray.parseArray(parentObj.getJSONArray("path").toString()) : new JSONArray());
            if (parentObj.get("ciId") != null) {
                relObj.getJSONArray("excludeCiIdList").add(parentObj.getLong("ciId"));
            }
            if (parentObj.get("id") != null && relVo.getId().equals(parentObj.getLong("id"))) {
                continue;
            }
            boolean isExists = false;
            for (int i = 0; i < relObj.getJSONArray("excludeCiIdList").size(); i++) {
                Long cid = relObj.getJSONArray("excludeCiIdList").getLong(i);
                if (relVo.getDirection().equals(RelDirectionType.FROM.getValue()) && relVo.getToCiId().equals(cid)) {
                    isExists = true;
                    break;
                } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue()) && relVo.getFromCiId().equals(cid)) {
                    isExists = true;
                    break;
                }
            }
            if (isExists) {
                continue;
            }
            JSONObject relPathObj = new JSONObject();
            relPathObj.put("relId", relVo.getId());
            relPathObj.put("direction", relVo.getDirection());
            if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                relPathObj.put("relName", relVo.getToName());
                relPathObj.put("relLabel", relVo.getToLabel());
                relPathObj.put("ciName", relVo.getFromCiName());
                relPathObj.put("ciLabel", relVo.getFromCiLabel());
                relPathObj.put("ciId", relVo.getFromCiId());
                relPathObj.put("targetCiId", relVo.getToCiId());
                relPathObj.put("targetCiName", relVo.getToCiName());
                relPathObj.put("targetCiLabel", relVo.getToCiLabel());
            } else {
                relPathObj.put("relName", relVo.getFromName());
                relPathObj.put("relLabel", relVo.getFromLabel());
                relPathObj.put("ciName", relVo.getToCiName());
                relPathObj.put("ciLabel", relVo.getToCiLabel());
                relPathObj.put("ciId", relVo.getToCiId());
                relPathObj.put("targetCiId", relVo.getFromCiId());
                relPathObj.put("targetCiName", relVo.getFromCiName());
                relPathObj.put("targetCiLabel", relVo.getFromCiLabel());
            }
            relObj.getJSONArray("path").add(relPathObj);
            if (CollectionUtils.isNotEmpty(ciRelList) && level < ciRelList.size()) {
                JSONObject ciRelObj = ciRelList.getJSONObject(level);
                if (relVo.getId().equals(ciRelObj.getLong("relId")) && relVo.getDirection().equals(ciRelObj.getString("direction"))) {
                    level++;
                    if (level < ciRelList.size()) {
                        generateData(relObj, level, ciRelList, ciRelObj.getLong("targetCiId"));
                    } else {
                        relObj.put("selected", true);
                    }
                }
            }
            relObjList.add(relObj);
        }
        parentObj.put("children", relObjList);
        parentObj.put("expand", true);
    }
}
