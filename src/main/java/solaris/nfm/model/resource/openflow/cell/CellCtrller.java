//package solaris.nfm.model.resource.openflow.cell;
//
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//import solaris.nfm.exception.base.ExceptionBase;
//import solaris.nfm.model.resource.openflow.tnslice.TnSlice;
//import solaris.nfm.model.resource.openflow.tnslice.TnSliceDao;
//import solaris.nfm.service.TnsliceService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/v1/cell")
//@Slf4j
//public class CellCtrller {
//    @Autowired
//    private CellDao cellDao;
//    @Autowired
//    private TnSliceDao tnsliceDao;
//    @Autowired
//    private TnsliceService tnsliceService;
//
//    @GetMapping("")
//    @ResponseStatus(HttpStatus.OK)
//    public List<Cell> getCell() {
//        return cellDao.findAll();
//    }
//
//    @PostMapping("")
//    @ResponseStatus(HttpStatus.CREATED)
//    public Cell createCell(@RequestBody @Validated Cell cell) {
//        cellDao.save(cell);
//        return cell;
//    }
//
//    public Cell checkCell(Cell cell) {
//    	if(cell != null) {
//    		return cell;
//    	}
//    	return null;
//    }
//
//    @DeleteMapping("/{cellId}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void removeCell(@PathVariable Long cellId) throws ExceptionBase {
//        if (!cellDao.existsById(cellId)) {
//            throw new ExceptionBase(HttpStatus.NOT_FOUND.value(), "Cell not found");
//        }
//        cellDao.deleteById(cellId);
//    }
//
//    @PutMapping("/{cellId}/tns/{tnsId}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void setNs(@PathVariable Long cellId, @PathVariable Long tnsId) throws ExceptionBase {
//        if (!cellDao.existsById(cellId) || !tnsliceDao.existsById(tnsId)) {
//            throw new ExceptionBase(HttpStatus.NOT_FOUND.value(), "Cell or TnSlice not found");
//        }
//        Cell cell = cellDao.findById(cellId).get();
//        cell.setTnsliceId(tnsId);
//        cellDao.save(cell);
//        TnSlice tnslice = tnsliceDao.findById(tnsId).get();
//        tnsliceService.putFlows(tnslice, cell.getMac());
//    }
//
//    @DeleteMapping("/{cellId}/tns/{tnsId}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void removeNs(@PathVariable Long cellId, @PathVariable Long tnsId) throws ExceptionBase {
//        if (!cellDao.existsById(cellId) || !tnsliceDao.existsById(tnsId)) {
//            throw new ExceptionBase(HttpStatus.NOT_FOUND.value(), "Cell or TnSlice not found");
//        }
//        Cell cell = cellDao.findById(cellId).get();
//        cell.setTnsliceId(null);
//        cellDao.save(cell);
//        TnSlice tnslice = tnsliceDao.findById(tnsId).get();
//        tnsliceService.removeFlows(tnslice, cell.getMac());
//    }
//}