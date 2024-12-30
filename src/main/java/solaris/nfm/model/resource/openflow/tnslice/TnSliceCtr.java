//package solaris.nfm.model.resource.openflow.tnslice;
//
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.querydsl.binding.QuerydslPredicate;
//import org.springframework.http.HttpStatus;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseStatus;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import com.querydsl.core.types.Predicate;
//
//import lombok.extern.slf4j.Slf4j;
//import solaris.nfm.controller.base.ControllerBase;
//import solaris.nfm.controller.dto.RestResultDto;
//import solaris.nfm.model.resource.openflow.cell.Cell;
//import solaris.nfm.model.resource.openflow.cell.CellDao;
//import solaris.nfm.model.resource.openflow.cell.QCell;
//import solaris.nfm.service.TnsliceService;
//
//@RestController
//@RequestMapping("/v1/tnSlice")
//@Slf4j
//public class TnSliceCtr extends ControllerBase<TnSlice, TnSliceVo, TnSliceDto, TnSliceDto, TnSliceDmo>
//{
//	@Autowired
//	private TnSliceDao		dao;
//	@Autowired
//	private CellDao			cellDao;
//	@Autowired
//	private TnsliceService	tnsliceService;
//
//	/**
//	 * 取得全部的 TN 切片
//	 */
//	@GetMapping(value = "")
//	@ResponseStatus(HttpStatus.OK)
//	public RestResultDto<TnSliceVo> fetchAllTnSlices(@QuerydslPredicate(root = TnSlice.class) final Predicate predicate, final Pageable pageable) throws Exception
//	{
//		return findAllEntity(predicate, pageable);
//	}
//
//	/**
//	 * 取得指定的 TN 切片
//	 */
//	@GetMapping(value = "/{id}")
//	@ResponseStatus(HttpStatus.OK)
//	public TnSliceVo fetchTnSlice(@PathVariable("id") final Long id) throws Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		this.dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		return findEntity(id);
//	}
//
//	@PostMapping("")
//	@ResponseStatus(HttpStatus.CREATED)
//	public JsonNode createTnSlice(@Validated(TnSliceDto.Create.class) @RequestBody final TnSliceDto dto) throws SecurityException, Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		// 檢查 name 是否為唯一值
//		// if (this.dao.countByNetworkTypeAndCode(dto.getNetworkType(), dto.getCode()) > 0)
//		// throw new EntityHasExistedException("Error message (" + dto.getNetworkType() + ", " + dto.getCode() + ") has existed.");
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		final TnSlice detach = new TnSlice();
//		BeanUtils.copyProperties(dto, detach);
//
//		final TnSlice entity = this.dmo.createOne(detach);
//		return JsonNodeFactory.instance.objectNode().put("id", entity.getId());
//	}
//
//	@PutMapping("/{id}")
//	@ResponseStatus(HttpStatus.NO_CONTENT)
//	public void modifyTnSlice(@PathVariable("id") final Long id, @Validated(TnSliceDto.Create.class) @RequestBody final TnSliceDto dto) throws Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		this.dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		updateEntity(id, dto);
//
//		final TnSlice tnSlice = this.dao.getById(id);
//		for (final Cell cell : cellDao.findAll(QCell.cell.tnsliceId.eq(id)))
//		{
//			tnsliceService.putFlows(tnSlice, cell.getMac());
//		}
//	}
//
//	@DeleteMapping("/{id}")
//	@ResponseStatus(HttpStatus.NO_CONTENT)
//	public void removeTnSlice(@PathVariable("id") final Long id) throws Exception
//	{
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[S]
//		this.dmo.checkOneById(id);
//		// ---[ 入參驗證 ]-------------------------------------------------------------------------------------------------[E]
//
//		deleteEntity(id);
//	}
//}