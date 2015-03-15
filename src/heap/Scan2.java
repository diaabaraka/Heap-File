//package heap;
//
//import java.io.IOException;
//
//import bufmgr.BufMgrException;
//import bufmgr.BufferPoolExceededException;
//import bufmgr.HashEntryNotFoundException;
//import bufmgr.HashOperationException;
//import bufmgr.InvalidFrameNumberException;
//import bufmgr.PageNotReadException;
//import bufmgr.PagePinnedException;
//import bufmgr.PageUnpinnedException;
//import bufmgr.ReplacerException;
//import global.PageId;
//import global.RID;
//import global.SystemDefs;
//
//public class Scan {
//	private PageId cursorPageId;
//	private RID cursorRID;
//
//	public Scan(Heapfile hf) {
//		cursorPageId = new PageId(hf.getfirstDirPageId().pid);
//		cursorRID = new RID(cursorPageId, 0);
//	}
//
//	public Tuple getNext(RID rid) throws ReplacerException,
//			HashOperationException, PageUnpinnedException,
//			InvalidFrameNumberException, PageNotReadException,
//			BufferPoolExceededException, PagePinnedException, BufMgrException,
//			IOException, InvalidSlotNumberException, HashEntryNotFoundException {
//
//		if (cursorPageId.pid != -1) {
//			rid.copyRid(cursorRID);
//			HFPage cursorPage = new HFPage();
//			SystemDefs.JavabaseBM.pinPage(cursorPageId, cursorPage, false);
//			Tuple retTuple = cursorPage.getRecord(rid);
//
//			if (cursorPage.nextRecord(cursorRID) == null) {
//				PageId temp = cursorPage.getNextPage();
//				SystemDefs.JavabaseBM.unpinPage(cursorPageId, false);
//				cursorPageId = temp;
//				cursorRID = new RID(cursorPageId, 0);
//
//			} else {
//				cursorRID.copyRid(cursorPage.nextRecord(cursorRID));
//				SystemDefs.JavabaseBM.unpinPage(cursorPageId, false);
//			}
//
//			return retTuple;
//
//		}
//		return null;
//	}
//
//	public void closescan() {
//
//		cursorPageId = null;
//		cursorRID = null;
//	}
//
//}
