package solaris.nfm.controller.dto;

import java.util.Iterator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;

import lombok.Getter;
import lombok.Setter;

public class PaginationDto
{
	private int		pageNumber		= 1;
	private int		pageSize		= 100;
	@Getter
	@Setter
	private int		totalPages		= 0;
	@Getter
	@Setter
	private Long	totalElements	= 0L;

	public PaginationDto()
	{}

	public PaginationDto(final Page<?> page)
	{
		setPageNumber(page.getNumber() + 1);
		setPageSize(page.getSize());
		setTotalElements(page.getTotalElements());
		setTotalPages(page.getTotalPages());
	}

	public PaginationDto(final Pageable pageable, final Long amount)
	{
		setPageNumber(pageable.getPageNumber() + 1);
		setPageSize(pageable.getPageSize());
		setTotalElements(amount);
		setTotalPages((int) Math.ceil((double) amount / (double) this.pageSize));
	}

	public PaginationDto(final PageRequest pageRequest, final Long amount)
	{
		setPageNumber(pageRequest.getPageNumber() + 1);
		setPageSize(pageRequest.getPageSize());
		// setAmount(amount);
		if (pageRequest.getSort() != null)
		{
			Iterator<Order> it = pageRequest.getSort()
					.iterator();
			while (it.hasNext())
			{
				Order order = it.next();
				// setSortDirection(order.getDirection().toString());
				// setSortField(order.getProperty());
			}
		}
	}

	public PaginationDto(final int pageNumber, final int pageSize)
	{
		setPageNumber(pageNumber);
		setPageSize(pageSize);
	}

	public PaginationDto(final int pageNumber, final int pageAmount, final int pageSize, final long resourceAmount)
	{
		setPageNumber(pageNumber);
		setTotalPages(pageAmount);
		setPageSize(pageSize);
		setTotalElements(resourceAmount);
	}

	public PaginationDto(final int pageNumber, final int pageSize, final String sortDirection, final String sortField)
	{
		setPageNumber(pageNumber);
		setPageSize(pageSize);
		// setSortDirection(sortDirection);
		// setSortField(sortField);
	}

	public int getPageNumber()
	{
		return this.pageNumber;
	}

	public void setPageNumber(int pageNumber)
	{
		if (pageNumber < 1) pageNumber = 1;
		this.pageNumber = pageNumber;
	}

	public int getPageSize()
	{
		return this.pageSize;
	}

	public void setPageSize(int pageSize)
	{
		if (pageSize < 1) pageSize = 1;
		if (pageSize > 100) pageSize = 100;
		this.pageSize = pageSize;
	}
}
