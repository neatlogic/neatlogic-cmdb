/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.cmdb.matrix.dto;

import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.RelFilterVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrFilterVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.common.dto.BasePageVo;

import java.util.List;

public class MatrixCiEntitySearchVo extends BasePageVo {

    private Boolean distinct = false;
    private Long ciId;
    private Integer isVirtual;

    private List<Long> attrIdList;
    private List<Long> globalAttrIdList;
    private List<Long> relIdList;

    private List<String> showConstList;
    private List<AttrVo> showAttrList;
    private List<RelVo> showRelList;
    private List<GlobalAttrVo> showGlobalAttrList;

    private CiVo fromCi;
    private List<CiVo> joinCiList;
    private List<AttrVo> joinAttrList;
    private List<RelVo> joinRelList;
    private List<GlobalAttrVo> joinGlobalAttrList;

    private List<AttrFilterVo> attrFilterList;
    private List<GlobalAttrFilterVo> globalAttrFilterList;
    private List<RelFilterVo> relFilterList;

    private List<Long> filterCiIdList;
    private List<Long> idList;
    private Integer isCheckExpiredTime = 0;

    public Boolean getDistinct() {
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    public Long getCiId() {
        return ciId;
    }

    public void setCiId(Long ciId) {
        this.ciId = ciId;
    }

    public Integer getIsVirtual() {
        return isVirtual;
    }

    public void setIsVirtual(Integer isVirtual) {
        this.isVirtual = isVirtual;
    }

    public List<Long> getAttrIdList() {
        return attrIdList;
    }

    public void setAttrIdList(List<Long> attrIdList) {
        this.attrIdList = attrIdList;
    }

    public List<Long> getGlobalAttrIdList() {
        return globalAttrIdList;
    }

    public void setGlobalAttrIdList(List<Long> globalAttrIdList) {
        this.globalAttrIdList = globalAttrIdList;
    }

    public List<Long> getRelIdList() {
        return relIdList;
    }

    public void setRelIdList(List<Long> relIdList) {
        this.relIdList = relIdList;
    }

    public List<String> getShowConstList() {
        return showConstList;
    }

    public void setShowConstList(List<String> showConstList) {
        this.showConstList = showConstList;
    }

    public List<AttrVo> getShowAttrList() {
        return showAttrList;
    }

    public void setShowAttrList(List<AttrVo> showAttrList) {
        this.showAttrList = showAttrList;
    }

    public List<RelVo> getShowRelList() {
        return showRelList;
    }

    public void setShowRelList(List<RelVo> showRelList) {
        this.showRelList = showRelList;
    }

    public List<GlobalAttrVo> getShowGlobalAttrList() {
        return showGlobalAttrList;
    }

    public void setShowGlobalAttrList(List<GlobalAttrVo> showGlobalAttrList) {
        this.showGlobalAttrList = showGlobalAttrList;
    }

    public CiVo getFromCi() {
        return fromCi;
    }

    public void setFromCi(CiVo fromCi) {
        this.fromCi = fromCi;
    }

    public List<CiVo> getJoinCiList() {
        return joinCiList;
    }

    public void setJoinCiList(List<CiVo> joinCiList) {
        this.joinCiList = joinCiList;
    }

    public List<AttrVo> getJoinAttrList() {
        return joinAttrList;
    }

    public void setJoinAttrList(List<AttrVo> joinAttrList) {
        this.joinAttrList = joinAttrList;
    }

    public List<RelVo> getJoinRelList() {
        return joinRelList;
    }

    public void setJoinRelList(List<RelVo> joinRelList) {
        this.joinRelList = joinRelList;
    }

    public List<GlobalAttrVo> getJoinGlobalAttrList() {
        return joinGlobalAttrList;
    }

    public void setJoinGlobalAttrList(List<GlobalAttrVo> joinGlobalAttrList) {
        this.joinGlobalAttrList = joinGlobalAttrList;
    }

    public List<AttrFilterVo> getAttrFilterList() {
        return attrFilterList;
    }

    public void setAttrFilterList(List<AttrFilterVo> attrFilterList) {
        this.attrFilterList = attrFilterList;
    }

    public List<GlobalAttrFilterVo> getGlobalAttrFilterList() {
        return globalAttrFilterList;
    }

    public void setGlobalAttrFilterList(List<GlobalAttrFilterVo> globalAttrFilterList) {
        this.globalAttrFilterList = globalAttrFilterList;
    }

    public List<RelFilterVo> getRelFilterList() {
        return relFilterList;
    }

    public void setRelFilterList(List<RelFilterVo> relFilterList) {
        this.relFilterList = relFilterList;
    }

    public List<Long> getFilterCiIdList() {
        return filterCiIdList;
    }

    public void setFilterCiIdList(List<Long> filterCiIdList) {
        this.filterCiIdList = filterCiIdList;
    }

    public List<Long> getIdList() {
        return idList;
    }

    public void setIdList(List<Long> idList) {
        this.idList = idList;
    }

    public Integer getIsCheckExpiredTime() {
        return isCheckExpiredTime;
    }

    public void setIsCheckExpiredTime(Integer isCheckExpiredTime) {
        this.isCheckExpiredTime = isCheckExpiredTime;
    }
}
