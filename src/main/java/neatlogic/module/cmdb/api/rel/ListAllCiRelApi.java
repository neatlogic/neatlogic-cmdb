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

package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiRelPathVo;
import neatlogic.framework.cmdb.dto.ci.CiRelVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAllCiRelApi extends PrivateApiComponentBase {

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/rel/list";
    }

    @Override
    public String getName() {
        return "nmcar.listallcirelapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "ciIdList", type = ApiParamType.JSONARRAY, desc = "nmcar.listallcirelapi.ciidlist"),
            @Param(name = "endCiId", type = ApiParamType.LONG, desc = "nmcar.listallcirelapi.endciid")
            //@Param(name = "direction", type = ApiParamType.ENUM, isRequired = true, desc = "方向", member = RelDirectionType.class),
    })
    @Output({@Param(explode = RelVo[].class)})
    @Description(desc = "nmcar.listallcirelapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        JSONArray ciIdListJson = jsonObj.getJSONArray("ciIdList");
        Long endCiId = jsonObj.getLong("endCiId");
        List<Long> ciIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciIdListJson)) {
            for (int i = 0; i < ciIdListJson.size(); i++) {
                ciIdList.add(ciIdListJson.getLong(i));
            }
        }
        List<CiRelVo> path = new ArrayList<>();
        List<CiRelPathVo> pathList = new ArrayList<>();
        getRelPathByCiId(ciId, path, pathList);
        if (CollectionUtils.isNotEmpty(pathList) && (CollectionUtils.isNotEmpty(ciIdList) || endCiId != null)) {

            for (int i = pathList.size() - 1; i >= 0; i--) {
                List<CiRelVo> pList = pathList.get(i).getCiRelList();
                boolean allMatch = true;
                if (CollectionUtils.isNotEmpty(ciIdList)) {
                    for (Long cId : ciIdList) {
                        if (pList.stream().noneMatch(d -> d.getCiId().equals(cId))) {
                            allMatch = false;
                            break;
                        }
                    }
                }
                if (endCiId != null && (!pList.get(pList.size() - 1).getCiId().equals(endCiId))) {
                    allMatch = false;
                }
                if (!allMatch) {
                    pathList.remove(i);
                }
            }
        }
        return pathList;
    }


    private void getRelPathByCiId(Long ciId, List<CiRelVo> ciRelList, List<CiRelPathVo> pathList) {
        if (!ciRelList.contains(new CiRelVo(ciId))) {// 出现回环，中断递归
            List<RelVo> relList = relMapper.getRelByCiId(ciId);
            if (CollectionUtils.isNotEmpty(relList)) {
                for (RelVo relVo : relList) {
                    CiRelVo ciRelVo = new CiRelVo();
                    ciRelVo.setRelId(relVo.getId());
                    ciRelVo.setDirection(relVo.getDirection());
                    if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                        ciRelVo.setCiId(relVo.getFromCiId());
                        ciRelVo.setCiName(relVo.getFromCiName());
                        ciRelVo.setCiLabel(relVo.getFromCiLabel());
                        ciRelVo.setRelName(relVo.getToName());
                        ciRelVo.setRelLabel(relVo.getToLabel());
                    } else {
                        ciRelVo.setCiId(relVo.getToCiId());
                        ciRelVo.setCiName(relVo.getToCiName());
                        ciRelVo.setCiLabel(relVo.getToCiLabel());
                        ciRelVo.setRelName(relVo.getFromName());
                        ciRelVo.setRelLabel(relVo.getFromLabel());
                    }
                    if (!ciRelList.contains(ciRelVo)) {
                        List<CiRelVo> newCiRelList = new ArrayList<>(ciRelList);
                        newCiRelList.add(ciRelVo);
                        getRelPathByCiId(relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId(), newCiRelList, pathList);
                    } else {
                        //如果path中已经存在了ciId，代表当前关系链已经构建完成
                        CiRelPathVo ciRelPathVo = new CiRelPathVo();
                        ciRelPathVo.setCiRelList(ciRelList);
                        if (!pathList.contains(ciRelPathVo)) {
                            pathList.add(ciRelPathVo);
                        }
                    }
                }
            } else {
                CiVo ciVo = ciMapper.getCiById(ciId);
                CiRelVo ciRelVo = new CiRelVo();
                ciRelVo.setCiId(ciId);
                ciRelVo.setCiName(ciVo.getName());
                ciRelVo.setCiLabel(ciVo.getLabel());
                ciRelList.add(ciRelVo);
                CiRelPathVo ciRelPathVo = new CiRelPathVo();
                ciRelPathVo.setCiRelList(ciRelList);
                if (!pathList.contains(ciRelPathVo)) {
                    pathList.add(ciRelPathVo);
                }
            }
        } else {
            //如果path中已经存在了ciId，代表当前关系链已经构建完成
            CiRelPathVo ciRelPathVo = new CiRelPathVo();
            ciRelPathVo.setCiRelList(ciRelList);
            if (!pathList.contains(ciRelPathVo)) {
                pathList.add(ciRelPathVo);
            }
        }
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

}
