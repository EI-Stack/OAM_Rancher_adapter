package solaris.nfm.controller.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Holisun Wu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestResultDto<E>
{
	private List<E>			content;
	private PaginationDto	pagination;
}