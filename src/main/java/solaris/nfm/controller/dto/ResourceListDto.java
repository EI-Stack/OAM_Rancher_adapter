package solaris.nfm.controller.dto;

import java.util.List;

public class ResourceListDto<B>
{
	private List<B>			resourceList;
	private PaginationDto	pageInfo;

	public List<B> getResourceList()
	{
		return this.resourceList;
	}

	public void setResourceList(final List<B> resourceList)
	{
		this.resourceList = resourceList;
	}

	public PaginationDto getPageInfo()
	{
		return this.pageInfo;
	}

	public void setPageInfo(final PaginationDto pageMetaBean)
	{
		this.pageInfo = pageMetaBean;
	}
}
