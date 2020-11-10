package codedriver.module.cmdb.service.ci;

import org.springframework.transaction.annotation.Transactional;

import codedriver.module.cmdb.dto.ci.CiVo;

public interface CiService {
	@Transactional
	public int saveCi(CiVo ciVo);
	
	@Transactional
	public int deleteCi(Long ciId);

	public CiVo getCiById(Long id);

}
