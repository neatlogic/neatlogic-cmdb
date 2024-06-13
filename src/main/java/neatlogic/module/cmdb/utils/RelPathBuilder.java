/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.crossover.IRelCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class RelPathBuilder {

    public static void build(JSONObject parentObj, int level, JSONArray ciRelList, Long ciId) {
        IRelCrossoverMapper relMapper = CrossoverServiceFactory.getApi(IRelCrossoverMapper.class);
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        JSONArray relObjList = new JSONArray();
        for (RelVo relVo : relList) {
            JSONObject relObj = JSON.parseObject(JSON.toJSONString(relVo));
            relObj.put("children", new JSONArray());
            relObj.put("loading", false);
            relObj.put("selected", false);
            relObj.put("nodeKey", relVo.getId().toString());
            relObj.put("relObj", relVo.getId());
            relObj.put("excludeCiIdList", parentObj.get("excludeCiIdList") != null ? JSON.parseArray(parentObj.getJSONArray("excludeCiIdList").toString()) : new JSONArray());
            relObj.put("path", parentObj.get("path") != null ? JSON.parseArray(parentObj.getJSONArray("path").toString()) : new JSONArray());
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
            relPathObj.put("nodeKey", relVo.getId().toString());
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
                        build(relObj, level, ciRelList, ciRelObj.getLong("targetCiId"));
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
