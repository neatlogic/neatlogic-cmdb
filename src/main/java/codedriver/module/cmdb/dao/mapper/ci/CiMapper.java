package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.module.cmdb.dto.ci.CiTypeVo;
import codedriver.module.cmdb.dto.ci.CiVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 * @ClassName: CiMapper
 * @Description: TODO
 */
public interface CiMapper {
    public List<CiVo> getAllCi();

    public List<CiVo> getCiByToCiId(Long ciId);

    public List<CiVo> getCiByIdList(@Param("ciIdList") List<Long> ciIds);

    public int checkCiNameIsExists(CiVo ciVo);

    public int checkCiLabelIsExists(CiVo ciVo);

    public List<CiTypeVo> searchCiTypeCi(CiVo ciVo);

    public CiVo getCiById(Long ciId);

    public int updateCi(CiVo ciVo);

    public int insertCi(CiVo ciVo);

    public int deleteCiById(Long ciId);
}
