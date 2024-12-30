package solaris.nfm.model.resource.alarm.mapping;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.EntityIdInvalidException;
import solaris.nfm.exception.EntityNotFoundException;
import solaris.nfm.model.base.manager.DmoBase;

@Service
@Slf4j
public class FaultErrorMessageDmo extends DmoBase<FaultErrorMessage, FaultErrorMessageDao>
{
	@Override
	// @Cacheable("FaultErrorMessage")
	public FaultErrorMessage getOne(final Long id) throws EntityIdInvalidException, EntityNotFoundException
	{
		// log.debug("沒有用暫存");
		return super.getOne(id);
	}
}
