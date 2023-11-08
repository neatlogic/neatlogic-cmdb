/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
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

package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.ci.RelativeRelVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListRelativeRelApi extends PrivateApiComponentBase {

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/relative/rel/list";
    }

    @Override
    public String getName() {
        return "获取关联关系列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    static class RelPathVo {
        private Long ciId;
        private Long relId;
        private String path;

        public Long getRelId() {
            return relId;
        }

        public void setRelId(Long relId) {
            this.relId = relId;
        }

        public String getPath() {
            if (path == null) {
                path = "";
            }
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Long getCiId() {
            return ciId;
        }

        public void setCiId(Long ciId) {
            this.ciId = ciId;
        }

        public RelPathVo(Long relId, Long ciId) {
            this.relId = relId;
            this.ciId = ciId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RelPathVo relPathVo = (RelPathVo) o;
            return relId.equals(relPathVo.relId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(relId);
        }
    }

    class UpwardCiBuilder {
        private final Map<Long, List<CiVo>> upwardCiMap = new HashMap<>();

        private List<CiVo> getUpwardCiListByCiId(Long ciId) {
            if (!upwardCiMap.containsKey(ciId)) {
                CiVo ciVo = ciMapper.getCiById(ciId);
                upwardCiMap.put(ciId, ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht()));
            }
            return upwardCiMap.get(ciId);
        }
    }

    private void findCiPathRecursive(String currentPath, Long sourceCiId, Long currentCiId, Map<Long, RelPathVo> pathMap, List<RelVo> relList, UpwardCiBuilder builder) {
        //找到当前模型的所有父模型，以便查找继承过来的关系
        List<CiVo> parentCiList = builder.getUpwardCiListByCiId(currentCiId);
        //找到当前节点的所有上游关系（下游端指向当前节点或当前节点的父节点）
        List<RelVo> fromRelList = relList.stream().filter(d -> parentCiList.stream().anyMatch(ci -> ci.getId().equals(d.getToCiId()))).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(fromRelList)) {
            fromRelList.forEach(d -> {
                if (!d.getFromCiId().equals(d.getToCiId())) {
                    RelPathVo relPathVo = new RelPathVo(d.getId(), d.getFromCiId());
                    String newPath = currentPath + "<" + d.getId();
                    if (!pathMap.containsKey(relPathVo.getRelId())) {
                        relPathVo.setPath(newPath);
                        pathMap.put(relPathVo.getRelId(), relPathVo);
                        findCiPathRecursive(newPath, sourceCiId, d.getFromCiId(), pathMap, relList, builder);
                    } else if (pathMap.containsKey(relPathVo.getRelId())) {
                        //比较原路径，用短的替换，同时要更换ciId，因为同一个relId可能会因为方向不一样，导致ciId不一样
                        if (StringUtils.isNotBlank(pathMap.get(relPathVo.getRelId()).getPath())) {
                            if (newPath.split("[>|<]").length < pathMap.get(relPathVo.getRelId()).getPath().split("[>|<]").length) {
                                pathMap.get(relPathVo.getRelId()).setPath(newPath);
                                pathMap.get(relPathVo.getRelId()).setCiId(d.getFromCiId());
                            }
                        }
                    }
                }
            });
        }
        //List<CiVo> toUpCiList = builder.getUpwardCiListByCiId(currentCiId);
        //找到当前节点的所有下游关系（上游端指向当前节点或当前节点的父节点）
        List<RelVo> toRelList = relList.stream().filter(d -> parentCiList.stream().anyMatch(ci -> ci.getId().equals(d.getFromCiId()))).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toRelList)) {
            toRelList.forEach(d -> {
                if (!d.getFromCiId().equals(d.getToCiId())) {
                    RelPathVo relPathVo = new RelPathVo(d.getId(), d.getToCiId());
                    String newPath = currentPath + ">" + d.getId();
                    if (!pathMap.containsKey(relPathVo.getRelId())) {
                        relPathVo.setPath(newPath);
                        pathMap.put(relPathVo.getRelId(), relPathVo);
                        findCiPathRecursive(newPath, sourceCiId, d.getToCiId(), pathMap, relList, builder);
                    } else if (pathMap.containsKey(relPathVo.getRelId())) {
                        //比较原路径，用短的替换，同时要更换ciId，因为同一个relId可能会因为方向不一样，导致ciId不一样
                        if (StringUtils.isNotBlank(pathMap.get(relPathVo.getRelId()).getPath())) {
                            if (newPath.split("[>|<]").length < pathMap.get(relPathVo.getRelId()).getPath().split("[>|<]").length) {
                                pathMap.get(relPathVo.getRelId()).setPath(newPath);
                                pathMap.get(relPathVo.getRelId()).setCiId(d.getToCiId());
                            }
                        }
                    }
                }
            });
        }
    }


    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "relId", type = ApiParamType.LONG, desc = "关系id,为空代表新关系"),
            @Param(name = "fromCiId", type = ApiParamType.LONG, isRequired = true, desc = "当前关系来源模型id"),
            @Param(name = "toCiId", type = ApiParamType.LONG, isRequired = true, desc = "当前关系目标模型id")})
    @Output({@Param(explode = RelativeRelVo[].class)})
    @Description(desc = "获取关联关系列表接口，配置关系的关联时使用")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciId = paramObj.getLong("ciId");
        Long relId = paramObj.getLong("relId");
        Long fromCiId = paramObj.getLong("fromCiId");
        Long toCiId = paramObj.getLong("toCiId");

        List<RelVo> relList = relMapper.getAllRelList();

        List<RelVo> ciRelList = relMapper.getRelByCiId(ciId);
        //排除当前模型中的关系
        if (relId != null) {
            relList.removeIf(d -> d.getId().equals(relId));
        }

        if (CollectionUtils.isNotEmpty(relList)) {

            Map<Long, RelPathVo> fromPathMap = new HashMap<>();
            findCiPathRecursive("", fromCiId, fromCiId, fromPathMap, relList, new UpwardCiBuilder());
            //补充起点节点进关系列表，关系id用0代替
            fromPathMap.put(0L, new RelPathVo(0L, fromCiId));

            Map<Long, RelPathVo> toPathMap = new HashMap<>();
            findCiPathRecursive("", toCiId, toCiId, toPathMap, relList, new UpwardCiBuilder());
            //补充起点节点进关系列表，关系id用0代替
            toPathMap.put(0L, new RelPathVo(0L, toCiId));

            /*System.out.println("FROM");
            for (RelPathVo path : fromPathSet) {
                System.out.println(path.getPath());
            }
            System.out.println("TO");
            for (RelPathVo path : toPathSet) {
                System.out.println(path.getPath());
            }*/

            if (MapUtils.isNotEmpty(fromPathMap) && MapUtils.isNotEmpty(toPathMap)) {
                List<RelativeRelVo> matchRelList = new ArrayList<>();
                Map<Long, RelativeRelVo> relativeRelMap = new HashMap<>();
                for (RelPathVo f : fromPathMap.values()) {
                    for (RelPathVo t : toPathMap.values()) {
                        Optional<RelVo> op = relList.stream().filter(d -> (d.getFromCiId().equals(f.getCiId()) && d.getToCiId().equals(t.getCiId()))
                                /*|| (ciId.equals(d.getFromCiId()) && d.getToCiId().equals(t.getCiId()))*/
                                /*|| (d.getFromCiId().equals(t.getCiId()) && d.getToCiId().equals(f.getCiId()))*/).findFirst();
                        if (op.isPresent()) {
                            RelVo rel = op.get();
                            //如果关系两端其中之一是抽象模型，则不能作为关联关系
                            //CiVo fromCi = ciMapper.getCiById(rel.getFromCiId());
                            //CiVo toCi = ciMapper.getCiById(rel.getToCiId());
                            //if (fromCi.getIsAbstract().equals(0) && toCi.getIsAbstract().equals(0)) {
                            //排除当前模型的关系
                            //if (ciRelList.stream().noneMatch(d -> d.getId().equals(rel.getId()))) {
                            RelativeRelVo relativeRelVo = new RelativeRelVo();
                            relativeRelVo.setRelativeRelId(rel.getId());
                            relativeRelVo.setRelativeRelLabel(rel.getFromLabel() + "->" + rel.getToLabel());
                            relativeRelVo.setFromPath(f.getPath());
                            relativeRelVo.setToPath(t.getPath());
                            if (!relativeRelMap.containsKey(relativeRelVo.getRelId())) {
                                relativeRelMap.put(relativeRelVo.getRelativeRelId(), relativeRelVo);
                            } else {
                                    //比较路径长度，用路径比较短的关系代替路径比较长的关系
                                    if ((relativeRelMap.get(relativeRelVo.getRelativeRelId()).getFromPath().split("[>|<]").length + relativeRelMap.get(relativeRelVo.getRelativeRelId()).getToPath().split("[>|<]").length)
                                            > (relativeRelVo.getFromPath().split("[>|<]").length + relativeRelVo.getToPath().split("[>|<]").length)) {
                                        relativeRelMap.put(relativeRelVo.getRelativeRelId(), relativeRelVo);
                                    }
                                }
                            //}
                            //}
                        }
                    }
                }
                for (Long k : relativeRelMap.keySet()) {
                    matchRelList.add(relativeRelMap.get(k));
                }
                if (CollectionUtils.isNotEmpty(matchRelList)) {
                    matchRelList.sort(Comparator.comparing(RelativeRelVo::getRelativeRelLabel));
                    //System.out.println("START");
                    /*for (RelativeRelVo rel : matchRelList) {
                        System.out.println(rel.getRelativeRelLabel() + ":" + rel.getFromPath() + "#" + rel.getToPath());
                    }*/
                    return matchRelList;
                }
            }
        }
        return null;
    }
}
